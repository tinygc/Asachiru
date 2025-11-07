package com.tinygc.asachiru.domain.entity

/**
 * 日時を表すエンティティ
 */
data class DateTime(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val second: Int,
    val dayOfWeek: DayOfWeek
) {
    /**
     * 時刻文字列（HH:MM形式）
     */
    val timeString: String
        get() = String.format("%02d:%02d", hour, minute)

    /**
     * 日付文字列（MM/DD (Day)形式）
     */
    val dateString: String
        get() = String.format("%02d/%02d (%s)", month, day, dayOfWeek.shortName)

    companion object {
        /**
         * 空のDateTime（初期値用）
         */
        val EMPTY = DateTime(0, 0, 0, 0, 0, 0, DayOfWeek.SUNDAY)
    }
}
