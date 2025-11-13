package com.tinygc.asachiru.domain.entity

/**
 * 天気情報を表すエンティティ
 */
data class Weather(
    val condition: WeatherCondition,
    val currentTemperature: Int,
    val maxTemperature: Int,
    val minTemperature: Int,
    val precipitationProbability: Int,
    val dateLabel: String // "今日" or "明日"
) {
    companion object {
        /**
         * 空のWeather（初期値用）
         */
        val EMPTY = Weather(
            condition = WeatherCondition.OTHER,
            currentTemperature = 0,
            maxTemperature = 0,
            minTemperature = 0,
            precipitationProbability = 0,
            dateLabel = "今日"
        )
    }
}
