package com.tinygc.asachiru.domain.usecase.clock

import com.tinygc.asachiru.domain.entity.DateTime
import com.tinygc.asachiru.domain.entity.DayOfWeek
import java.util.Calendar

/**
 * 現在の日時を取得するユースケース
 */
class GetCurrentDateTimeUseCase {
    /**
     * 現在の日時を取得
     * @return 現在のDateTime
     */
    operator fun invoke(): DateTime {
        val calendar = Calendar.getInstance()

        return DateTime(
            year = calendar.get(Calendar.YEAR),
            month = calendar.get(Calendar.MONTH) + 1, // 0-11 → 1-12
            day = calendar.get(Calendar.DAY_OF_MONTH),
            hour = calendar.get(Calendar.HOUR_OF_DAY),
            minute = calendar.get(Calendar.MINUTE),
            second = calendar.get(Calendar.SECOND),
            dayOfWeek = convertCalendarDayToEnum(calendar.get(Calendar.DAY_OF_WEEK))
        )
    }

    /**
     * CalendarのDAY_OF_WEEKをDayOfWeekに変換
     */
    private fun convertCalendarDayToEnum(calendarDay: Int): DayOfWeek {
        return when (calendarDay) {
            Calendar.SUNDAY -> DayOfWeek.SUNDAY
            Calendar.MONDAY -> DayOfWeek.MONDAY
            Calendar.TUESDAY -> DayOfWeek.TUESDAY
            Calendar.WEDNESDAY -> DayOfWeek.WEDNESDAY
            Calendar.THURSDAY -> DayOfWeek.THURSDAY
            Calendar.FRIDAY -> DayOfWeek.FRIDAY
            Calendar.SATURDAY -> DayOfWeek.SATURDAY
            else -> DayOfWeek.SUNDAY // デフォルト
        }
    }
}
