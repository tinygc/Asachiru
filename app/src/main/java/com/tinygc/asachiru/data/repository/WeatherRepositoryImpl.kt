package com.tinygc.asachiru.data.repository

import android.util.Log
import com.tinygc.asachiru.data.datasource.remote.WeatherApiDataSource
import com.tinygc.asachiru.data.dto.toEntity
import com.tinygc.asachiru.data.util.PostalCodeConverter
import com.tinygc.asachiru.domain.common.AppException
import com.tinygc.asachiru.domain.common.Result
import com.tinygc.asachiru.domain.entity.Weather
import com.tinygc.asachiru.domain.repository.WeatherRepository
import java.io.IOException
import java.util.TimeZone

/**
 * WeatherRepositoryの実装
 */
class WeatherRepositoryImpl(
    private val weatherApiDataSource: WeatherApiDataSource
) : WeatherRepository {

    companion object {
        private const val TAG = "WeatherRepository"
    }

    override suspend fun getWeather(postalCode: String): Result<Weather> {
        return try {
            Log.d(TAG, "getWeather: postalCode=$postalCode")

            // 郵便番号を地域コードに変換
            val areaCode = PostalCodeConverter.convertToAreaCode(postalCode)
            Log.d(TAG, "getWeather: areaCode=$areaCode")

            // APIからデータ取得
            val apiResponse = weatherApiDataSource.fetchWeather(areaCode)
            Log.d(TAG, "getWeather: API response received, forecasts count=${apiResponse.forecasts.size}")

            // 現在の日付を日本時間（JST）で取得（YYYY-MM-DD形式）
            // 天気APIは日本の天気情報を日本時間で返すため、日付判定もJSTで行う
            val jstTimeZone = TimeZone.getTimeZone("Asia/Tokyo")
            val calendar = java.util.Calendar.getInstance(jstTimeZone)
            val currentDate = String.format(
                "%04d-%02d-%02d",
                calendar.get(java.util.Calendar.YEAR),
                calendar.get(java.util.Calendar.MONTH) + 1,
                calendar.get(java.util.Calendar.DAY_OF_MONTH)
            )
            val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            Log.d(TAG, "getWeather: currentDate=$currentDate (JST), currentHour=$currentHour (JST)")

            // forecasts[0]とforecasts[1]の日付を確認
            val todayForecast = apiResponse.forecasts.getOrNull(0)
            val tomorrowForecast = apiResponse.forecasts.getOrNull(1)

            Log.d(TAG, "getWeather: forecast[0].date=${todayForecast?.date}")
            Log.d(TAG, "getWeather: forecast[1].date=${tomorrowForecast?.date}")

            // 17時以降は明日の天気、それ以外は今日の天気を表示
            val (forecast, dateLabel) = if (currentHour >= 17) {
                // forecasts[1]が明日の日付か確認
                if (tomorrowForecast != null) {
                    Pair(tomorrowForecast, "明日")
                } else {
                    Log.w(TAG, "getWeather: No tomorrow forecast, using today's")
                    Pair(todayForecast ?: throw IOException("No forecast data available"), "今日")
                }
            } else {
                // forecasts[0]が今日の日付か確認
                if (todayForecast != null && todayForecast.date == currentDate) {
                    Pair(todayForecast, "今日")
                } else if (tomorrowForecast != null && tomorrowForecast.date == currentDate) {
                    // forecasts[0]が昨日のデータの場合、forecasts[1]を使用
                    Log.w(TAG, "getWeather: forecast[0] is not today, using forecast[1]")
                    Pair(tomorrowForecast, "今日")
                } else {
                    // どちらも今日でない場合は最初の予報を使用
                    Log.w(TAG, "getWeather: Neither forecast matches today, using forecast[0]")
                    Pair(todayForecast ?: throw IOException("No forecast data available"), "今日")
                }
            }

            Log.d(TAG, "getWeather: dateLabel=$dateLabel")
            Log.d(TAG, "getWeather: forecast.telop=${forecast.telop}")
            Log.d(TAG, "getWeather: forecast.temperature.max=${forecast.temperature.max?.celsius}")
            Log.d(TAG, "getWeather: forecast.temperature.min=${forecast.temperature.min?.celsius}")
            Log.d(TAG, "getWeather: forecast.chanceOfRain.t06_12=${forecast.chanceOfRain?.t06_12}")
            Log.d(TAG, "getWeather: forecast.chanceOfRain.t12_18=${forecast.chanceOfRain?.t12_18}")

            // DTOをEntityに変換
            val weather = forecast.toEntity(dateLabel)
            Log.d(TAG, "getWeather: weather converted - dateLabel=${weather.dateLabel}, currentTemp=${weather.currentTemperature}, maxTemp=${weather.maxTemperature}, minTemp=${weather.minTemperature}, precipProb=${weather.precipitationProbability}")

            Result.Success(weather)
        } catch (e: IOException) {
            Log.e(TAG, "getWeather: Network error", e)
            Result.Error(AppException.NetworkException(e.message ?: "Network error"))
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "getWeather: Invalid postal code", e)
            Result.Error(AppException.ApiException(400, e.message ?: "Invalid postal code"))
        } catch (e: Exception) {
            Log.e(TAG, "getWeather: Parse error", e)
            Result.Error(AppException.ParseException(e.message ?: "Failed to parse weather data"))
        }
    }
}
