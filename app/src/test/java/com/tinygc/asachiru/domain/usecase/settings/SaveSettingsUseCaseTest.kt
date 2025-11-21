package com.tinygc.asachiru.domain.usecase.settings

import com.tinygc.asachiru.domain.common.AppException
import com.tinygc.asachiru.domain.common.Result
import com.tinygc.asachiru.domain.entity.Settings
import com.tinygc.asachiru.domain.repository.SettingsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SaveSettingsUseCaseTest {

    @Mock
    private lateinit var settingsRepository: SettingsRepository

    private lateinit var useCase: SaveSettingsUseCase

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        useCase = SaveSettingsUseCase(settingsRepository)
    }

    @Test
    fun `invoke should save settings when settings are valid`() = runTest {
        // Arrange
        val validSettings = Settings(
            postalCode = "1000001",
            newsIntervalMinutes = 30,
            rssUrl = "https://test.com/rss",
            enableTts = false,
            rssPreset = null
        )
        whenever(settingsRepository.saveSettings(validSettings))
            .thenReturn(Result.Success(Unit))

        // Act
        val result = useCase(validSettings)

        // Assert
        assertTrue(result is Result.Success)
        verify(settingsRepository).saveSettings(validSettings)
    }

    @Test
    fun `invoke should return error when postal code is invalid`() = runTest {
        // Arrange
        val invalidSettings = Settings(
            postalCode = "123",
            newsIntervalMinutes = 30,
            rssUrl = "https://test.com/rss",
            enableTts = false,
            rssPreset = null
        ) // Invalid postal code (not 7 digits)

        // Act
        val result = useCase(invalidSettings)

        // Assert
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.exception is AppException.SettingsException)
        assertEquals("Invalid settings", error.exception.message)
        verify(settingsRepository, never()).saveSettings(any())
    }

    @Test
    fun `invoke should return error when news interval is invalid`() = runTest {
        // Arrange
        val invalidSettings = Settings(
            postalCode = "1000001",
            newsIntervalMinutes = 100,
            rssUrl = "https://test.com/rss",
            enableTts = false,
            rssPreset = null
        ) // Invalid interval (out of range)

        // Act
        val result = useCase(invalidSettings)

        // Assert
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.exception is AppException.SettingsException)
        assertEquals("Invalid settings", error.exception.message)
        verify(settingsRepository, never()).saveSettings(any())
    }

    @Test
    fun `invoke should return error when both postal code and news interval are invalid`() = runTest {
        // Arrange
        val invalidSettings = Settings(
            postalCode = "12",
            newsIntervalMinutes = 0,
            rssUrl = "https://test.com/rss",
            enableTts = false,
            rssPreset = null
        ) // Both invalid

        // Act
        val result = useCase(invalidSettings)

        // Assert
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.exception is AppException.SettingsException)
        verify(settingsRepository, never()).saveSettings(any())
    }

    @Test
    fun `invoke should handle repository save error`() = runTest {
        // Arrange
        val validSettings = Settings(
            postalCode = "1000001",
            newsIntervalMinutes = 30,
            rssUrl = "https://test.com/rss",
            enableTts = false,
            rssPreset = null
        )
        val saveException = AppException.SettingsException("Failed to save")
        whenever(settingsRepository.saveSettings(validSettings))
            .thenReturn(Result.Error(saveException))

        // Act
        val result = useCase(validSettings)

        // Assert
        assertTrue(result is Result.Error)
        assertEquals(saveException, (result as Result.Error).exception)
    }

    @Test
    fun `invoke should validate postal code format - must be 7 digits`() = runTest {
        // Arrange
        val invalidCodes = listOf(
            Settings("", 30, "https://test.com/rss", false, null),           // Empty
            Settings("123", 30, "https://test.com/rss", false, null),        // Too short
            Settings("12345678", 30, "https://test.com/rss", false, null),   // Too long
            Settings("12345ab", 30, "https://test.com/rss", false, null),    // Contains letters
            Settings("123-4567", 30, "https://test.com/rss", false, null)    // Contains hyphen
        )

        invalidCodes.forEach { settings ->
            // Act
            val result = useCase(settings)

            // Assert
            assertTrue("Settings with postal code '${settings.postalCode}' should be invalid",
                result is Result.Error)
        }
    }

    @Test
    fun `invoke should validate news interval range - must be 1 to 60`() = runTest {
        // Arrange
        val invalidIntervals = listOf(
            Settings("1000001", 0, "https://test.com/rss", false, null),      // Too low
            Settings("1000001", -1, "https://test.com/rss", false, null),     // Negative
            Settings("1000001", 61, "https://test.com/rss", false, null),     // Too high
            Settings("1000001", 100, "https://test.com/rss", false, null),    // Way too high
            Settings("1000001", 1000, "https://test.com/rss", false, null)    // Extremely high
        )

        invalidIntervals.forEach { settings ->
            // Act
            val result = useCase(settings)

            // Assert
            assertTrue("Settings with interval ${settings.newsIntervalMinutes} should be invalid",
                result is Result.Error)
        }
    }

    @Test
    fun `invoke should accept valid settings with boundary values`() = runTest {
        // Arrange
        val boundarySettings = listOf(
            Settings("0000000", 1, "https://test.com/rss", false, null),     // Min interval
            Settings("9999999", 60, "https://test.com/rss", false, null),    // Max interval
            Settings("1234567", 30, "https://test.com/rss", false, null)     // Middle values
        )

        boundarySettings.forEach { settings ->
            whenever(settingsRepository.saveSettings(settings))
                .thenReturn(Result.Success(Unit))

            // Act
            val result = useCase(settings)

            // Assert
            assertTrue("Valid settings should succeed: postal=${settings.postalCode}, interval=${settings.newsIntervalMinutes}",
                result is Result.Success)
            verify(settingsRepository).saveSettings(settings)
        }
    }

    @Test
    fun `invoke should propagate repository success result`() = runTest {
        // Arrange
        val validSettings = Settings(
            postalCode = "1000001",
            newsIntervalMinutes = 45,
            rssUrl = "https://test.com/rss",
            enableTts = false,
            rssPreset = null
        )
        whenever(settingsRepository.saveSettings(validSettings))
            .thenReturn(Result.Success(Unit))

        // Act
        val result = useCase(validSettings)

        // Assert
        assertTrue(result is Result.Success)
    }

    @Test
    fun `invoke should check validation before calling repository`() = runTest {
        // Arrange
        val invalidSettings = Settings(
            postalCode = "abc",
            newsIntervalMinutes = 30,
            rssUrl = "https://test.com/rss",
            enableTts = false,
            rssPreset = null
        )

        // Act
        val result = useCase(invalidSettings)

        // Assert
        assertTrue(result is Result.Error)
        verify(settingsRepository, never()).saveSettings(any())
    }
}
