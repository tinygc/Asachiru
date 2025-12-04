package com.tinygc.asachiru.domain.entity

/**
 * 曜日を表すEnum
 */
enum class DayOfWeek(val shortName: String) {
    SUNDAY("日"),
    MONDAY("月"),
    TUESDAY("火"),
    WEDNESDAY("水"),
    THURSDAY("木"),
    FRIDAY("金"),
    SATURDAY("土");

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
