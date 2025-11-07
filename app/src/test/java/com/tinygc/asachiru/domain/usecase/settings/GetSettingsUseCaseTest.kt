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
        val expectedSettings = Settings("1000001", 30)
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
        assertEquals(30, result.newsIntervalMinutes)
    }

    @Test
    fun `invoke should return settings with different postal codes`() = runTest {
        // Arrange
        val postalCodes = listOf("1000001", "5300001", "0600000", "9999999")

        postalCodes.forEach { postalCode ->
            val settings = Settings(postalCode, 30)
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
            val settings = Settings("1000001", interval)
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
        val settings = Settings("1000001", 30)
        whenever(settingsRepository.getSettings()).thenReturn(settings)

        // Act
        useCase()

        // Assert
        verify(settingsRepository).getSettings()
    }

    @Test
    fun `invoke should directly delegate to repository`() = runTest {
        // Arrange
        val settings = Settings("1234567", 45)
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
        val settings1 = Settings("1000001", 30)
        val settings2 = Settings("5300001", 45)

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
        val invalidSettings = Settings("123", 100)
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
        val settings = Settings("", 30)
        whenever(settingsRepository.getSettings()).thenReturn(settings)

        // Act
        val result = useCase()

        // Assert
        assertEquals("", result.postalCode)
        assertEquals(30, result.newsIntervalMinutes)
    }
}
