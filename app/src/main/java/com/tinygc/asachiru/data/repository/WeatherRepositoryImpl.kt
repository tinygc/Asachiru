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
        
        /**
         * YYYY-MM-DD形式の日付文字列を数値に変換（比較用）
         * 例: "2025-12-17" → 20251217L
         * @return 変換された数値、パース失敗時はnull
         */
        private fun parseDateToNumber(dateString: String?): Long? {
            return dateString?.replace("-", "")?.toLongOrNull()
        }
        
        /**
         * 予報の日付に基づいて日付ラベル（「今日」「明日」）を決定する
         * @param forecastDate 予報の日付（YYYY-MM-DD形式）
         * @param currentDate 現在の日付（YYYY-MM-DD形式）
         * @param tomorrowDate 明日の日付（YYYY-MM-DD形式）
         * @param isAfter17 17時以降かどうか
         * @return 日付ラベル（「今日」または「明日」）
         */
        private fun determineDateLabel(
            forecastDate: String?,
            currentDate: String,
            tomorrowDate: String,
            isAfter17: Boolean
        ): String {
            return when (forecastDate) {
                tomorrowDate -> "明日"
                currentDate -> "今日"
                else -> {
                    // 日付を数値として比較
                    val forecastDateNum = parseDateToNumber(forecastDate)
                    val currentDateNum = parseDateToNumber(currentDate)
                    val tomorrowDateNum = parseDateToNumber(tomorrowDate)
                    
                    when {
                        forecastDateNum == null || currentDateNum == null || tomorrowDateNum == null -> {
                            // パース失敗時は安全側に倒す
                            Log.w(TAG, "determineDateLabel: Failed to parse date - forecastDate=$forecastDate, currentDate=$currentDate, tomorrowDate=$tomorrowDate")
                            if (isAfter17) "明日" else "今日"
                        }
                        forecastDateNum >= tomorrowDateNum -> {
                            // 明日以降のデータの場合は「明日」とする
                            if (forecastDateNum > tomorrowDateNum) {
                                Log.w(TAG, "determineDateLabel: Forecast is for day after tomorrow or later, using '明日' label")
                            }
                            "明日"
                        }
                        else -> {
                            // 過去または今日以前のデータ
                            Log.w(TAG, "determineDateLabel: Forecast is for past or current date, using '今日' label")
                            "今日"
                        }
                    }
                }
            }
        }
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

            // 現在の日本時間（JST）を取得（YYYY-MM-DD形式）
            // デバイスのタイムゾーンに関係なく、常にJSTで判定する
            val jstTimeZone = TimeZone.getTimeZone("Asia/Tokyo")
            val utcCalendar = java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            val currentUtcTimeMillis = utcCalendar.timeInMillis
            
            // UTCからJSTに変換（+9時間）
            val jstCalendar = java.util.Calendar.getInstance(jstTimeZone)
            jstCalendar.timeInMillis = currentUtcTimeMillis
            
            val currentDate = String.format(
                "%04d-%02d-%02d",
                jstCalendar.get(java.util.Calendar.YEAR),
                jstCalendar.get(java.util.Calendar.MONTH) + 1,
                jstCalendar.get(java.util.Calendar.DAY_OF_MONTH)
            )
            
            // 明日の日付も計算
            val tomorrowCalendar = jstCalendar.clone() as java.util.Calendar
            tomorrowCalendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
            val tomorrowDate = String.format(
                "%04d-%02d-%02d",
                tomorrowCalendar.get(java.util.Calendar.YEAR),
                tomorrowCalendar.get(java.util.Calendar.MONTH) + 1,
                tomorrowCalendar.get(java.util.Calendar.DAY_OF_MONTH)
            )
            
            val currentHour = jstCalendar.get(java.util.Calendar.HOUR_OF_DAY)
            Log.d(TAG, "getWeather: currentDate=$currentDate (JST), tomorrowDate=$tomorrowDate (JST), currentHour=$currentHour (JST), deviceTimeZone=${TimeZone.getDefault().id}")

            // forecasts[0]とforecasts[1]の日付を確認
            val todayForecast = apiResponse.forecasts.getOrNull(0)
            val tomorrowForecast = apiResponse.forecasts.getOrNull(1)

            Log.d(TAG, "getWeather: forecast[0].date=${todayForecast?.date}")
            Log.d(TAG, "getWeather: forecast[1].date=${tomorrowForecast?.date}")

            // 17時以降は明日の天気、それ以外は今日の天気を表示
            val (forecast, dateLabel) = if (currentHour >= 17) {
                // 17時以降: 明日の天気を表示
                if (tomorrowForecast != null) {
                    Pair(tomorrowForecast, "明日")
                } else {
                    Log.w(TAG, "getWeather: No tomorrow forecast available after 17:00, using forecast[0]")
                    // forecasts[1]がない場合はforecasts[0]を使用し、日付に基づいてラベルを決定
                    val label = determineDateLabel(
                        forecastDate = todayForecast?.date,
                        currentDate = currentDate,
                        tomorrowDate = tomorrowDate,
                        isAfter17 = true
                    )
                    Pair(todayForecast ?: throw IOException("No forecast data available"), label)
                }
            } else {
                // 17時前: 今日の天気を表示
                if (todayForecast != null && todayForecast.date == currentDate) {
                    // forecasts[0]が今日の日付
                    Pair(todayForecast, "今日")
                } else if (tomorrowForecast != null && tomorrowForecast.date == currentDate) {
                    // forecasts[0]が昨日または他の日付で、forecasts[1]が今日の日付
                    Log.w(TAG, "getWeather: forecast[0] is not today, but forecast[1] is today")
                    Pair(tomorrowForecast, "今日")
                } else {
                    // どちらも今日でない場合は、forecasts[0]の日付に基づいてラベルを決定
                    Log.w(TAG, "getWeather: Neither forecast matches today (currentDate=$currentDate), using forecast[0] with date-based label")
                    val label = determineDateLabel(
                        forecastDate = todayForecast?.date,
                        currentDate = currentDate,
                        tomorrowDate = tomorrowDate,
                        isAfter17 = false
                    )
                    Pair(todayForecast ?: throw IOException("No forecast data available"), label)
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
