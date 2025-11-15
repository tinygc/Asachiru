package com.tinygc.asachiru.domain.entity

/**
 * 天気情報を表すエンティティ
 */
data class Weather(
    val condition: WeatherCondition,
    val currentTemperature: Int,
    val maxTemperature: Int?, // nullの場合はデータなし
    val minTemperature: Int?, // nullの場合はデータなし
    val precipitationProbability: Int,
    val dateLabel: String, // "今日" or "明日"
    val iconText: String // 天気アイコンテキスト（例：「☀のち☁」）
) {
    companion object {
        /**
         * 空のWeather（初期値用）
         */
        val EMPTY = Weather(
            condition = WeatherCondition.OTHER,
            currentTemperature = 0,
            maxTemperature = null,
            minTemperature = null,
            precipitationProbability = 0,
            dateLabel = "今日",
            iconText = "?"
        )
    }
}
