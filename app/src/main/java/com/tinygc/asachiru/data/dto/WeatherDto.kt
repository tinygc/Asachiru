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
    val temperature: TemperatureDto,

    @SerializedName("chanceOfRain")
    val chanceOfRain: ChanceOfRainDto?
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

data class ChanceOfRainDto(
    @SerializedName("T00_06")
    val t00_06: String?, // 0-6時

    @SerializedName("T06_12")
    val t06_12: String?, // 6-12時

    @SerializedName("T12_18")
    val t12_18: String?, // 12-18時

    @SerializedName("T18_24")
    val t18_24: String?  // 18-24時
)

/**
 * WeatherDtoからWeatherエンティティへ変換
 */
fun ForecastDto.toEntity(): Weather {
    val condition = parseWeatherCondition(telop)

    // 現在気温は最高気温と最低気温の平均を使用
    val maxTemp = temperature.max?.celsius?.toIntOrNull() ?: 0
    val minTemp = temperature.min?.celsius?.toIntOrNull() ?: 0
    val currentTemp = if (maxTemp > 0 || minTemp > 0) (maxTemp + minTemp) / 2 else 0

    // 降水確率は日中（6-18時）の最大値を使用
    val precipProb = chanceOfRain?.let { rain ->
        listOfNotNull(
            rain.t06_12?.removeSuffix("%")?.toIntOrNull(),
            rain.t12_18?.removeSuffix("%")?.toIntOrNull()
        ).maxOrNull() ?: 0
    } ?: 0

    return Weather(
        condition = condition,
        currentTemperature = currentTemp,
        maxTemperature = maxTemp,
        minTemperature = minTemp,
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
