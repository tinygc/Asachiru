package com.tinygc.asachiru.domain.usecase.clock

import com.tinygc.asachiru.domain.entity.DayOfWeek
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class GetCurrentDateTimeUseCaseTest {

    @Test
    fun `invoke should return current DateTime`() {
        // Arrange
        val useCase = GetCurrentDateTimeUseCase()

        // Act
        val result = useCase()

        // Assert
        assertNotNull(result)
        assertTrue(result.year > 2000)
        assertTrue(result.month in 1..12)
        assertTrue(result.day in 1..31)
        assertTrue(result.hour in 0..23)
        assertTrue(result.minute in 0..59)
        assertTrue(result.second in 0..59)
    }

    @Test
    fun `invoke should return DateTime with valid year`() {
        // Arrange
        val useCase = GetCurrentDateTimeUseCase()
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        // Act
        val result = useCase()

        // Assert
        assertEquals(currentYear, result.year)
    }

    @Test
    fun `invoke should return DateTime with correct month conversion`() {
        // Arrange
        val useCase = GetCurrentDateTimeUseCase()
        val calendar = Calendar.getInstance()
        val expectedMonth = calendar.get(Calendar.MONTH) + 1 // 0-11 → 1-12

        // Act
        val result = useCase()

        // Assert
        assertEquals(expectedMonth, result.month)
    }

    @Test
    fun `invoke should return DateTime with correct day of week`() {
        // Arrange
        val useCase = GetCurrentDateTimeUseCase()
        val calendar = Calendar.getInstance()
        val expectedDayOfWeek = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> DayOfWeek.SUNDAY
            Calendar.MONDAY -> DayOfWeek.MONDAY
            Calendar.TUESDAY -> DayOfWeek.TUESDAY
            Calendar.WEDNESDAY -> DayOfWeek.WEDNESDAY
            Calendar.THURSDAY -> DayOfWeek.THURSDAY
            Calendar.FRIDAY -> DayOfWeek.FRIDAY
            Calendar.SATURDAY -> DayOfWeek.SATURDAY
            else -> DayOfWeek.SUNDAY
        }

        // Act
        val result = useCase()

        // Assert
        assertEquals(expectedDayOfWeek, result.dayOfWeek)
    }

    @Test
    fun `invoke should return DateTime with current time within reasonable range`() {
        // Arrange
        val useCase = GetCurrentDateTimeUseCase()
        val beforeCall = Calendar.getInstance()

        // Act
        val result = useCase()
        val afterCall = Calendar.getInstance()

        // Assert
        assertTrue(result.hour >= beforeCall.get(Calendar.HOUR_OF_DAY) - 1)
        assertTrue(result.hour <= afterCall.get(Calendar.HOUR_OF_DAY) + 1)
    }

    @Test
    fun `invoke should return consistent results when called multiple times quickly`() {
        // Arrange
        val useCase = GetCurrentDateTimeUseCase()

        // Act
        val result1 = useCase()
        val result2 = useCase()

        // Assert
        assertEquals(result1.year, result2.year)
        assertEquals(result1.month, result2.month)
        assertEquals(result1.day, result2.day)
        assertEquals(result1.dayOfWeek, result2.dayOfWeek)
        // Hour and minute should be the same or very close
        assertTrue(kotlin.math.abs(result1.hour - result2.hour) <= 1)
        assertTrue(kotlin.math.abs(result1.minute - result2.minute) <= 1)
    }

    @Test
    fun `invoke should return DateTime with valid timeString format`() {
        // Arrange
        val useCase = GetCurrentDateTimeUseCase()

        // Act
        val result = useCase()

        // Assert
        assertNotNull(result.timeString)
        assertTrue(result.timeString.matches(Regex("\\d{2}:\\d{2}")))
    }

    @Test
    fun `invoke should return DateTime with valid dateString format`() {
        // Arrange
        val useCase = GetCurrentDateTimeUseCase()

        // Act
        val result = useCase()

        // Assert
        assertNotNull(result.dateString)
        assertTrue(result.dateString.matches(Regex("\\d{4}/\\d{2}/\\d{2}")))
    }
}
