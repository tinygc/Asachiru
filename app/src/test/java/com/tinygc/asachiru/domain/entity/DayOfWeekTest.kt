package com.tinygc.asachiru.domain.entity

import org.junit.Assert.*
import org.junit.Test

class DayOfWeekTest {

    @Test
    fun `Sunday should return red color`() {
        // Arrange
        val sunday = DayOfWeek.SUNDAY

        // Act
        val color = sunday.getColor()

        // Assert
        assertEquals(0xFFFF0000.toInt(), color)
    }

    @Test
    fun `Saturday should return blue color`() {
        // Arrange
        val saturday = DayOfWeek.SATURDAY

        // Act
        val color = saturday.getColor()

        // Assert
        assertEquals(0xFF0000FF.toInt(), color)
    }

    @Test
    fun `Weekdays should return black color`() {
        // Arrange
        val weekdays = listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        )

        // Act & Assert
        weekdays.forEach { day ->
            assertEquals(0xFF000000.toInt(), day.getColor())
        }
    }

    @Test
    fun `All days should have correct short names`() {
        // Assert - 日本語の漢字一文字表記
        assertEquals("日", DayOfWeek.SUNDAY.shortName)
        assertEquals("月", DayOfWeek.MONDAY.shortName)
        assertEquals("火", DayOfWeek.TUESDAY.shortName)
        assertEquals("水", DayOfWeek.WEDNESDAY.shortName)
        assertEquals("木", DayOfWeek.THURSDAY.shortName)
        assertEquals("金", DayOfWeek.FRIDAY.shortName)
        assertEquals("土", DayOfWeek.SATURDAY.shortName)
    }

    @Test
    fun `DayOfWeek values should return all days`() {
        // Act
        val allDays = DayOfWeek.values()

        // Assert
        assertEquals(7, allDays.size)
        assertTrue(allDays.contains(DayOfWeek.SUNDAY))
        assertTrue(allDays.contains(DayOfWeek.MONDAY))
        assertTrue(allDays.contains(DayOfWeek.SATURDAY))
    }
}
