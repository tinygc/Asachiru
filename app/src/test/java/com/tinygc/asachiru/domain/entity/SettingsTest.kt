package com.tinygc.asachiru.domain.entity

import org.junit.Assert.*
import org.junit.Test

class SettingsTest {

    @Test
    fun `isValid should return true for valid settings`() {
        // Arrange
        val settings = Settings("1000001", 30)

        // Act
        val result = settings.isValid()

        // Assert
        assertTrue(result)
    }

    @Test
    fun `isValid should return false for invalid postal code`() {
        // Arrange
        val settings = Settings("123", 30)

        // Act
        val result = settings.isValid()

        // Assert
        assertFalse(result)
    }

    @Test
    fun `isValid should return false for invalid news interval`() {
        // Arrange
        val settings = Settings("1000001", 100)

        // Act
        val result = settings.isValid()

        // Assert
        assertFalse(result)
    }

    @Test
    fun `isPostalCodeValid should return true for 7 digit numbers`() {
        // Arrange
        val validCodes = listOf("1000001", "0000000", "9999999")

        // Act & Assert
        validCodes.forEach { code ->
            val settings = Settings(code, 30)
            assertTrue("PostalCode $code should be valid", settings.isPostalCodeValid())
        }
    }

    @Test
    fun `isPostalCodeValid should return false for invalid postal codes`() {
        // Arrange
        val invalidCodes = listOf(
            "123",        // Too short
            "12345678",   // Too long
            "12345ab",    // Contains letters
            "123-4567",   // Contains hyphen
            ""            // Empty
        )

        // Act & Assert
        invalidCodes.forEach { code ->
            val settings = Settings(code, 30)
            assertFalse("PostalCode $code should be invalid", settings.isPostalCodeValid())
        }
    }

    @Test
    fun `isNewsIntervalValid should return true for values between 1 and 60`() {
        // Arrange
        val validIntervals = listOf(1, 15, 30, 45, 60)

        // Act & Assert
        validIntervals.forEach { interval ->
            val settings = Settings("1000001", interval)
            assertTrue("Interval $interval should be valid", settings.isNewsIntervalValid())
        }
    }

    @Test
    fun `isNewsIntervalValid should return false for values outside range`() {
        // Arrange
        val invalidIntervals = listOf(0, -1, 61, 100, 1000)

        // Act & Assert
        invalidIntervals.forEach { interval ->
            val settings = Settings("1000001", interval)
            assertFalse("Interval $interval should be invalid", settings.isNewsIntervalValid())
        }
    }

    @Test
    fun `DEFAULT should have default values`() {
        // Act
        val default = Settings.DEFAULT

        // Assert
        assertEquals("", default.postalCode)
        assertEquals(30, default.newsIntervalMinutes)
    }

    @Test
    fun `Settings should be data class with correct equality`() {
        // Arrange
        val settings1 = Settings("1000001", 30)
        val settings2 = Settings("1000001", 30)
        val settings3 = Settings("1000002", 45)

        // Assert
        assertEquals(settings1, settings2)
        assertNotEquals(settings1, settings3)
    }

    @Test
    fun `Settings should hold all settings information`() {
        // Arrange & Act
        val settings = Settings(
            postalCode = "1234567",
            newsIntervalMinutes = 45
        )

        // Assert
        assertEquals("1234567", settings.postalCode)
        assertEquals(45, settings.newsIntervalMinutes)
    }
}
