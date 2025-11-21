package com.tinygc.asachiru.domain.usecase.settings

import com.tinygc.asachiru.domain.entity.Settings
import com.tinygc.asachiru.domain.repository.SettingsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class GetSettingsUseCaseTest {

    @Mock
    private lateinit var settingsRepository: SettingsRepository

    private lateinit var useCase: GetSettingsUseCase

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        useCase = GetSettingsUseCase(settingsRepository)
    }

    @Test
    fun `invoke should return settings from repository`() = runTest {
        // Arrange
        val expectedSettings = Settings(
            postalCode = "1000001",
            newsIntervalMinutes = 30,
            rssUrl = "https://test.com/rss",
            enableTts = false,
            rssPreset = null
        )
        whenever(settingsRepository.getSettings()).thenReturn(expectedSettings)

        // Act
        val result = useCase()

        // Assert
        assertEquals(expectedSettings, result)
        verify(settingsRepository).getSettings()
    }

    @Test
    fun `invoke should return default settings when repository returns default`() = runTest {
        // Arrange
        val defaultSettings = Settings.DEFAULT
        whenever(settingsRepository.getSettings()).thenReturn(defaultSettings)

        // Act
        val result = useCase()

        // Assert
        assertEquals(Settings.DEFAULT, result)
        assertEquals("", result.postalCode)
        assertEquals(5, result.newsIntervalMinutes)
    }

    @Test
    fun `invoke should return settings with different postal codes`() = runTest {
        // Arrange
        val postalCodes = listOf("1000001", "5300001", "0600000", "9999999")

        postalCodes.forEach { postalCode ->
            val settings = Settings(
                postalCode = postalCode,
                newsIntervalMinutes = 30,
                rssUrl = "https://test.com/rss",
                enableTts = false,
                rssPreset = null
            )
            whenever(settingsRepository.getSettings()).thenReturn(settings)

            // Act
            val result = useCase()

            // Assert
            assertEquals(postalCode, result.postalCode)
        }
    }

    @Test
    fun `invoke should return settings with different intervals`() = runTest {
        // Arrange
        val intervals = listOf(1, 15, 30, 45, 60)

        intervals.forEach { interval ->
            val settings = Settings(
                postalCode = "1000001",
                newsIntervalMinutes = interval,
                rssUrl = "https://test.com/rss",
                enableTts = false,
                rssPreset = null
            )
            whenever(settingsRepository.getSettings()).thenReturn(settings)

            // Act
            val result = useCase()

            // Assert
            assertEquals(interval, result.newsIntervalMinutes)
        }
    }

    @Test
    fun `invoke should call repository getSettings method`() = runTest {
        // Arrange
        val settings = Settings(
            postalCode = "1000001",
            newsIntervalMinutes = 30,
            rssUrl = "https://test.com/rss",
            enableTts = false,
            rssPreset = null
        )
        whenever(settingsRepository.getSettings()).thenReturn(settings)

        // Act
        useCase()

        // Assert
        verify(settingsRepository).getSettings()
    }

    @Test
    fun `invoke should directly delegate to repository`() = runTest {
        // Arrange
        val settings = Settings(
            postalCode = "1234567",
            newsIntervalMinutes = 45,
            rssUrl = "https://test.com/rss",
            enableTts = false,
            rssPreset = null
        )
        whenever(settingsRepository.getSettings()).thenReturn(settings)

        // Act
        val result = useCase()

        // Assert
        assertEquals(settings, result)
        verify(settingsRepository).getSettings()
    }

    @Test
    fun `invoke should return correct settings data structure`() = runTest {
        // Arrange
        val settings = Settings(
            postalCode = "1000001",
            newsIntervalMinutes = 45
        )
        whenever(settingsRepository.getSettings()).thenReturn(settings)

        // Act
        val result = useCase()

        // Assert
        assertEquals("1000001", result.postalCode)
        assertEquals(45, result.newsIntervalMinutes)
    }

    @Test
    fun `invoke should handle consecutive calls correctly`() = runTest {
        // Arrange
        val settings1 = Settings(
            postalCode = "1000001",
            newsIntervalMinutes = 30,
            rssUrl = "https://test.com/rss",
            enableTts = false,
            rssPreset = null
        )
        val settings2 = Settings(
            postalCode = "5300001",
            newsIntervalMinutes = 45,
            rssUrl = "https://test.com/rss",
            enableTts = false,
            rssPreset = null
        )

        whenever(settingsRepository.getSettings())
            .thenReturn(settings1)
            .thenReturn(settings2)

        // Act
        val result1 = useCase()
        val result2 = useCase()

        // Assert
        assertEquals(settings1, result1)
        assertEquals(settings2, result2)
        assertNotEquals(result1, result2)
    }

    @Test
    fun `invoke should return settings regardless of validity`() = runTest {
        // Arrange - Invalid settings (but repository can return them)
        val invalidSettings = Settings(
            postalCode = "123",
            newsIntervalMinutes = 100,
            rssUrl = "https://test.com/rss",
            enableTts = false,
            rssPreset = null
        )
        whenever(settingsRepository.getSettings()).thenReturn(invalidSettings)

        // Act
        val result = useCase()

        // Assert
        assertEquals(invalidSettings, result)
        assertEquals("123", result.postalCode)
        assertEquals(100, result.newsIntervalMinutes)
    }

    @Test
    fun `invoke should handle empty postal code from repository`() = runTest {
        // Arrange
        val settings = Settings(
            postalCode = "",
            newsIntervalMinutes = 30,
            rssUrl = "https://test.com/rss",
            enableTts = false,
            rssPreset = null
        )
        whenever(settingsRepository.getSettings()).thenReturn(settings)

        // Act
        val result = useCase()

        // Assert
        assertEquals("", result.postalCode)
        assertEquals(30, result.newsIntervalMinutes)
    }
}
