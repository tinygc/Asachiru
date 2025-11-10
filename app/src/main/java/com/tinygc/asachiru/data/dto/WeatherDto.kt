package com.tinygc.asachiru.data.dto

import com.google.gson.annotations.SerializedName
import com.tinygc.asachiru.domain.entity.Weather
import com.tinygc.asachiru.domain.entity.WeatherCondition

/**
 * 天気APIのレスポンスDTO
 */
data class WeatherApiResponse(
    @SerializedName("forecasts")
    val forecasts: List<ForecastDto>
)

data class ForecastDto(
    @SerializedName("date")
    val date: String,

    @SerializedName("telop")
    val telop: String, // "晴れ", "曇り", "雨"など

    @SerializedName("temperature")
    val temperature: TemperatureDto
)

data class TemperatureDto(
    @SerializedName("min")
    val min: TemperatureValueDto?,

    @SerializedName("max")
    val max: TemperatureValueDto?
)

data class TemperatureValueDto(
    @SerializedName("celsius")
    val celsius: String
)

/**
 * WeatherDtoからWeatherエンティティへ変換
 */
fun ForecastDto.toEntity(currentTemp: Int, precipProb: Int): Weather {
    val condition = parseWeatherCondition(telop)

    return Weather(
        condition = condition,
        currentTemperature = currentTemp,
        maxTemperature = temperature.max?.celsius?.toIntOrNull() ?: 0,
        minTemperature = temperature.min?.celsius?.toIntOrNull() ?: 0,
        precipitationProbability = precipProb
    )
}

/**
 * 天気文字列をWeatherConditionに変換
 * 複数の天気が含まれる場合は悪い天気を優先（雪 > 雨 > 曇り > 晴れ）
 */
private fun parseWeatherCondition(telop: String): WeatherCondition {
    return when {
        telop.contains("雪") -> WeatherCondition.SNOWY
        telop.contains("雨") -> WeatherCondition.RAINY
        telop.contains("曇") -> WeatherCondition.CLOUDY
        telop.contains("晴") -> WeatherCondition.SUNNY
        else -> WeatherCondition.OTHER
    }
}
