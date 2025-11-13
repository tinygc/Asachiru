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

            // 現在時刻を取得
            val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            Log.d(TAG, "getWeather: currentHour=$currentHour")

            // 17時以降は明日の天気を表示、それ以外は今日の天気を表示
            val (forecast, dateLabel) = if (currentHour >= 17) {
                // 明日の予報を取得（forecasts[1]）
                val tomorrowForecast = apiResponse.forecasts.getOrNull(1)
                    ?: throw IOException("No tomorrow forecast data available")
                Pair(tomorrowForecast, "明日")
            } else {
                // 今日の予報を取得（forecasts[0]）
                val todayForecast = apiResponse.forecasts.getOrNull(0)
                    ?: throw IOException("No forecast data available")
                Pair(todayForecast, "今日")
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
