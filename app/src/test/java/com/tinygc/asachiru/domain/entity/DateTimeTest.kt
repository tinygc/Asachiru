package com.tinygc.asachiru.domain.entity

import org.junit.Assert.*
import org.junit.Test

class DateTimeTest {

    @Test
    fun `timeString should format time correctly`() {
        // Arrange
        val dateTime = DateTime(2025, 11, 6, 14, 5, 30, DayOfWeek.WEDNESDAY)

        // Act
        val timeString = dateTime.timeString

        // Assert
        assertEquals("14:05", timeString)
    }

    @Test
    fun `timeString should format time with leading zeros`() {
        // Arrange
        val dateTime = DateTime(2025, 1, 1, 9, 3, 0, DayOfWeek.MONDAY)

        // Act
        val timeString = dateTime.timeString

        // Assert
        assertEquals("09:03", timeString)
    }

    @Test
    fun `dateString should format date correctly`() {
        // Arrange
        val dateTime = DateTime(2025, 11, 6, 14, 5, 30, DayOfWeek.WEDNESDAY)

        // Act
        val dateString = dateTime.dateString

        // Assert
        assertEquals("11/06 (Wed)", dateString)
    }

    @Test
    fun `dateString should format date with leading zeros`() {
        // Arrange
        val dateTime = DateTime(2025, 1, 5, 10, 30, 0, DayOfWeek.FRIDAY)

        // Act
        val dateString = dateTime.dateString

        // Assert
        assertEquals("01/05 (Fri)", dateString)
    }

    @Test
    fun `dateString should include day of week short name`() {
        // Arrange
        val sunday = DateTime(2025, 11, 2, 10, 0, 0, DayOfWeek.SUNDAY)
        val saturday = DateTime(2025, 11, 8, 10, 0, 0, DayOfWeek.SATURDAY)

        // Act
        val sundayString = sunday.dateString
        val saturdayString = saturday.dateString

        // Assert
        assertTrue(sundayString.contains("Sun"))
        assertTrue(saturdayString.contains("Sat"))
    }

    @Test
    fun `EMPTY should have zero values`() {
        // Act
        val empty = DateTime.EMPTY

        // Assert
        assertEquals(0, empty.year)
        assertEquals(0, empty.month)
        assertEquals(0, empty.day)
        assertEquals(0, empty.hour)
        assertEquals(0, empty.minute)
        assertEquals(0, empty.second)
        assertEquals(DayOfWeek.SUNDAY, empty.dayOfWeek)
    }

    @Test
    fun `DateTime should be data class with correct properties`() {
        // Arrange
        val dateTime1 = DateTime(2025, 11, 6, 14, 30, 45, DayOfWeek.WEDNESDAY)
        val dateTime2 = DateTime(2025, 11, 6, 14, 30, 45, DayOfWeek.WEDNESDAY)
        val dateTime3 = DateTime(2025, 11, 7, 14, 30, 45, DayOfWeek.THURSDAY)

        // Assert - data class equality
        assertEquals(dateTime1, dateTime2)
        assertNotEquals(dateTime1, dateTime3)

        // Assert - properties
        assertEquals(2025, dateTime1.year)
        assertEquals(11, dateTime1.month)
        assertEquals(6, dateTime1.day)
        assertEquals(14, dateTime1.hour)
        assertEquals(30, dateTime1.minute)
        assertEquals(45, dateTime1.second)
        assertEquals(DayOfWeek.WEDNESDAY, dateTime1.dayOfWeek)
    }
}
