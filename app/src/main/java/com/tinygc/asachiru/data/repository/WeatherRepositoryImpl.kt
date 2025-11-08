package com.tinygc.asachiru.data.repository

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

    override suspend fun getWeather(postalCode: String): Result<Weather> {
        return try {
            // 郵便番号を地域コードに変換
            val areaCode = PostalCodeConverter.convertToAreaCode(postalCode)

            // APIからデータ取得
            val apiResponse = weatherApiDataSource.fetchWeather(areaCode)

            // 今日の予報を取得（forecasts[0]）
            val todayForecast = apiResponse.forecasts.firstOrNull()
                ?: throw IOException("No forecast data available")

            // DTOをEntityに変換
            // 注: 現在気温と降水確率はAPIレスポンスに含まれない場合があるため、
            //     ここでは簡易的に最高気温を現在気温、降水確率を0%としている
            val currentTemp = todayForecast.temperature.max?.celsius?.toIntOrNull() ?: 0
            val weather = todayForecast.toEntity(currentTemp, 0)

            Result.Success(weather)
        } catch (e: IOException) {
            Result.Error(AppException.NetworkException(e.message ?: "Network error"))
        } catch (e: IllegalArgumentException) {
            Result.Error(AppException.ApiException(400, e.message ?: "Invalid postal code"))
        } catch (e: Exception) {
            Result.Error(AppException.ParseException(e.message ?: "Failed to parse weather data"))
        }
    }
}
