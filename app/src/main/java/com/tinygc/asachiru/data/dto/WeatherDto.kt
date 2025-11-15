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
 * @param dateLabel 日付ラベル（"今日" or "明日"）
 */
fun ForecastDto.toEntity(dateLabel: String = "今日"): Weather {
    val condition = parseWeatherCondition(telop)
    val iconText = generateWeatherIconText(telop)

    // 気温データの取得（nullの場合でもログに記録）
    val maxTemp = temperature.max?.celsius?.toIntOrNull()
    val minTemp = temperature.min?.celsius?.toIntOrNull()

    // 現在気温の推定ロジック
    // 1. 最高気温と最低気温の両方がある場合は平均
    // 2. どちらか一方しかない場合はその値を使用
    // 3. 両方ない場合は0（データなし）
    val currentTemp = when {
        maxTemp != null && minTemp != null -> (maxTemp + minTemp) / 2
        maxTemp != null -> maxTemp
        minTemp != null -> minTemp
        else -> 0
    }

    // 降水確率は全時間帯から最大値を取得
    // nullや空文字列、"%"のみの文字列を除外
    val precipProb = chanceOfRain?.let { rain ->
        listOfNotNull(
            rain.t00_06?.removeSuffix("%")?.trim()?.toIntOrNull(),
            rain.t06_12?.removeSuffix("%")?.trim()?.toIntOrNull(),
            rain.t12_18?.removeSuffix("%")?.trim()?.toIntOrNull(),
            rain.t18_24?.removeSuffix("%")?.trim()?.toIntOrNull()
        ).maxOrNull() ?: 0
    } ?: 0

    return Weather(
        condition = condition,
        currentTemperature = currentTemp,
        maxTemperature = maxTemp, // nullのまま渡す
        minTemperature = minTemp, // nullのまま渡す
        precipitationProbability = precipProb,
        dateLabel = dateLabel,
        iconText = iconText
    )
}

/**
 * 天気文字列をWeatherConditionに変換
 * 複数の天気が含まれる場合は最初の天気を優先
 */
private fun parseWeatherCondition(telop: String): WeatherCondition {
    // 「のち」「時々」「一時」などで分割して最初の天気を取得
    val firstWeather = telop.split("のち", "時々", "一時", "後", "ところにより").firstOrNull() ?: telop

    return when {
        firstWeather.contains("雪") -> WeatherCondition.SNOWY
        firstWeather.contains("雨") -> WeatherCondition.RAINY
        firstWeather.contains("曇") -> WeatherCondition.CLOUDY
        firstWeather.contains("晴") -> WeatherCondition.SUNNY
        else -> WeatherCondition.OTHER
    }
}

/**
 * 天気文字列からアイコンテキストを生成
 * 例: "晴れのち曇り" → "☀のち☁"
 */
private fun generateWeatherIconText(telop: String): String {
    // 天気の区切り文字
    val separators = listOf("のち", "時々", "一時", "後", "ところにより")

    // 区切り文字で分割しながら、区切り文字も保持
    var result = telop
    separators.forEach { separator ->
        if (result.contains(separator)) {
            val parts = result.split(separator)
            val icons = parts.map { weatherTextToIcon(it.trim()) }
            result = icons.joinToString(separator)
        }
    }

    // 分割されなかった場合は単一の天気
    if (!separators.any { telop.contains(it) }) {
        result = weatherTextToIcon(telop.trim())
    }

    return result
}

/**
 * 天気テキストをアイコンに変換
 */
private fun weatherTextToIcon(weatherText: String): String {
    return when {
        weatherText.contains("雪") -> "⛄"
        weatherText.contains("雨") -> "☂"
        weatherText.contains("曇") -> "☁"
        weatherText.contains("晴") -> "☀"
        else -> "?"
    }
}
