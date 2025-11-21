package com.tinygc.asachiru.domain.entity

import org.junit.Assert.*
import org.junit.Test

class SettingsTest {

    @Test
    fun `isValid should return true for valid settings`() {
        // Arrange
        val settings = Settings(
            postalCode = "1000001",
            newsIntervalMinutes = 30,
            rssUrl = "https://test.com/rss",
            enableTts = false,
            rssPreset = null
        )

        // Act
        val result = settings.isValid()

        // Assert
        assertTrue(result)
    }

    @Test
    fun `isValid should return false for invalid postal code`() {
        // Arrange
        val settings = Settings(
            postalCode = "123",
            newsIntervalMinutes = 30,
            rssUrl = "https://test.com/rss",
            enableTts = false,
            rssPreset = null
        )

        // Act
        val result = settings.isValid()

        // Assert
        assertFalse(result)
    }

    @Test
    fun `isValid should return false for invalid news interval`() {
        // Arrange
        val settings = Settings(
            postalCode = "1000001",
            newsIntervalMinutes = 100,
            rssUrl = "https://test.com/rss",
            enableTts = false,
            rssPreset = null
        )

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
            val settings = Settings(
                postalCode = code,
                newsIntervalMinutes = 30,
                rssUrl = "https://test.com/rss",
                enableTts = false,
                rssPreset = null
            )
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
            val settings = Settings(
                postalCode = code,
                newsIntervalMinutes = 30,
                rssUrl = "https://test.com/rss",
                enableTts = false,
                rssPreset = null
            )
            assertFalse("PostalCode $code should be invalid", settings.isPostalCodeValid())
        }
    }

    @Test
    fun `isNewsIntervalValid should return true for values between 1 and 60`() {
        // Arrange
        val validIntervals = listOf(1, 15, 30, 45, 60)

        // Act & Assert
        validIntervals.forEach { interval ->
            val settings = Settings(
                postalCode = "1000001",
                newsIntervalMinutes = interval,
                rssUrl = "https://test.com/rss",
                enableTts = false,
                rssPreset = null
            )
            assertTrue("Interval $interval should be valid", settings.isNewsIntervalValid())
        }
    }

    @Test
    fun `isNewsIntervalValid should return false for values outside range`() {
        // Arrange
        val invalidIntervals = listOf(0, -1, 61, 100, 1000)

        // Act & Assert
        invalidIntervals.forEach { interval ->
            val settings = Settings(
                postalCode = "1000001",
                newsIntervalMinutes = interval,
                rssUrl = "https://test.com/rss",
                enableTts = false,
                rssPreset = null
            )
            assertFalse("Interval $interval should be invalid", settings.isNewsIntervalValid())
        }
    }

    @Test
    fun `DEFAULT should have default values`() {
        // Act
        val default = Settings.DEFAULT

        // Assert
        assertEquals("", default.postalCode)
        assertEquals(5, default.newsIntervalMinutes)
        assertNull(default.rssUrl)
        assertFalse(default.enableTts)
        assertNull(default.rssPreset)
    }

    @Test
    fun `Settings should be data class with correct equality`() {
        // Arrange
        val settings1 = Settings(
            postalCode = "1000001",
            newsIntervalMinutes = 30,
            rssUrl = "https://test.com/rss",
            enableTts = false,
            rssPreset = null
        )
        val settings2 = Settings(
            postalCode = "1000001",
            newsIntervalMinutes = 30,
            rssUrl = "https://test.com/rss",
            enableTts = false,
            rssPreset = null
        )
        val settings3 = Settings(
            postalCode = "1000002",
            newsIntervalMinutes = 45,
            rssUrl = "https://test.com/rss",
            enableTts = false,
            rssPreset = null
        )

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
