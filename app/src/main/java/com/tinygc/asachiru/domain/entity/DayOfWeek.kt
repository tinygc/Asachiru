package com.tinygc.asachiru.domain.entity

/**
 * 曜日を表すEnum
 */
enum class DayOfWeek(val shortName: String) {
    SUNDAY("Sun"),
    MONDAY("Mon"),
    TUESDAY("Tue"),
    WEDNESDAY("Wed"),
    THURSDAY("Thu"),
    FRIDAY("Fri"),
    SATURDAY("Sat");

    /**
     * 曜日の色を取得
     * @return 色コード（Android Color）
     */
    fun getColor(): Int {
        return when (this) {
            SUNDAY -> 0xFFFF0000.toInt() // 赤
            SATURDAY -> 0xFF0000FF.toInt() // 青
            else -> 0xFF000000.toInt() // 黒
        }
    }
}
