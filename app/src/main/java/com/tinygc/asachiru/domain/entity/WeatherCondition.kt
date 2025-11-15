package com.tinygc.asachiru.domain.entity

/**
 * 天気状態を表すEnum
 */
enum class WeatherCondition(val displayName: String) {
    SUNNY("晴れ"),
    CLOUDY("曇り"),
    RAINY("雨"),
    SNOWY("雪"),
    OTHER("その他");

    /**
     * アイコンリソースIDを取得
     * 注: 実際のリソースIDはres/drawable/にアイコンを配置後に有効になる
     */
    fun getIconResourceId(): Int {
        // リソースIDはビルド時に生成されるため、仮の値を返す
        // 実際の実装では R.drawable.ic_weather_* を返す
        return when (this) {
            SUNNY -> android.R.drawable.ic_menu_day // 仮
            CLOUDY -> android.R.drawable.ic_menu_day // 仮
            RAINY -> android.R.drawable.ic_menu_day // 仮
            SNOWY -> android.R.drawable.ic_menu_day // 仮
            OTHER -> android.R.drawable.ic_menu_day // 仮
        }
    }
}
