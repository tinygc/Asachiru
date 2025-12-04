package com.tinygc.asachiru.data.repository

import com.tinygc.asachiru.data.datasource.local.SettingsLocalDataSource
import com.tinygc.asachiru.domain.common.AppException
import com.tinygc.asachiru.domain.common.Result
import com.tinygc.asachiru.domain.entity.Settings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryImplTest {

    private lateinit var settingsLocalDataSource: SettingsLocalDataSource
    private lateinit var repository: SettingsRepositoryImpl

    @Before
    fun setUp() {
        settingsLocalDataSource = mock()
        repository = SettingsRepositoryImpl(settingsLocalDataSource)
    }

    @Test
    fun `getSettings should return Settings from data source`() = runTest {
        // Given
        val mockSettings = Settings(
            postalCode = "1000001",
            newsIntervalMinutes = 30,
            enableBgm = true
        )
        whenever(settingsLocalDataSource.loadSettings()).thenReturn(mockSettings)

        // When
        val result = repository.getSettings()

        // Then
        // Note: feedwatch flavor forces enableBgm to false
        assertEquals("1000001", result.postalCode)
        assertEquals(30, result.newsIntervalMinutes)
        assertEquals(false, result.enableBgm)
    }

    @Test
    fun `getSettings should call data source exactly once`() = runTest {
        // Given
        val mockSettings = Settings(
            postalCode = "1000001",
            newsIntervalMinutes = 30,
            enableBgm = true
        )
        whenever(settingsLocalDataSource.loadSettings()).thenReturn(mockSettings)

        // When
        repository.getSettings()

        // Then
        verify(settingsLocalDataSource, times(1)).loadSettings()
    }

    @Test
    fun `saveSettings should return Success when data source succeeds`() = runTest {
        // Given
        val settings = Settings(
            postalCode = "1000001",
            newsIntervalMinutes = 30,
            rssUrl = "https://test.com/rss",
            enableTts = false,
            enableBgm = true,
            rssPreset = null
        )
        whenever(settingsLocalDataSource.saveSettings(any())).then { }

        // When
        val result = repository.saveSettings(settings)

        // Then
        assertTrue(result is Result.Success)
        // Note: feedwatch flavor forces enableBgm to false
        verify(settingsLocalDataSource).saveSettings(
            Settings(
                postalCode = "1000001",
                newsIntervalMinutes = 30,
                rssUrl = "https://test.com/rss",
                enableTts = false,
                enableBgm = false,
                rssPreset = null
            )
        )
    }

    @Test
    fun `saveSettings should call data source with correct settings`() = runTest {
        // Given
        val settings = Settings(
            postalCode = "5400001",
            newsIntervalMinutes = 60,
            rssUrl = "https://test.com/rss",
            enableTts = false,
            enableBgm = true,
            rssPreset = null
        )

        // When
        repository.saveSettings(settings)

        // Then
        // Note: feedwatch flavor forces enableBgm to false
        verify(settingsLocalDataSource).saveSettings(
            Settings(
                postalCode = "5400001",
                newsIntervalMinutes = 60,
                rssUrl = "https://test.com/rss",
                enableTts = false,
                enableBgm = false,
                rssPreset = null
            )
        )
    }

    @Test
    fun `saveSettings should return SettingsException on failure`() = runTest {
        // Given
        val settings = Settings(
            postalCode = "1000001",
            newsIntervalMinutes = 30,
            rssUrl = "https://test.com/rss",
            enableTts = false,
            enableBgm = true,
            rssPreset = null
        )
        whenever(settingsLocalDataSource.saveSettings(any()))
            .thenThrow(RuntimeException("Save failed"))

        // When
        val result = repository.saveSettings(settings)

        // Then
        assertTrue(result is Result.Error)
        val exception = (result as Result.Error).exception
        assertTrue(exception is AppException.SettingsException)
        assertEquals("Save failed", exception.message)
    }

    @Test
    fun `saveSettings should handle generic exception with message`() = runTest {
        // Given
        val settings = Settings(
            postalCode = "1000001",
            newsIntervalMinutes = 30,
            rssUrl = "https://test.com/rss",
            enableTts = false,
            enableBgm = true,
            rssPreset = null
        )
        whenever(settingsLocalDataSource.saveSettings(any()))
            .thenThrow(RuntimeException("Custom error message"))

        // When
        val result = repository.saveSettings(settings)

        // Then
        assertTrue(result is Result.Error)
        val exception = (result as Result.Error).exception
        assertTrue(exception is AppException.SettingsException)
        assertEquals("Custom error message", exception.message)
    }

    @Test
    fun `saveSettings should handle exception with null message`() = runTest {
        // Given
        val settings = Settings(
            postalCode = "1000001",
            newsIntervalMinutes = 30,
            rssUrl = "https://test.com/rss",
            enableTts = false,
            enableBgm = true,
            rssPreset = null
        )
        whenever(settingsLocalDataSource.saveSettings(any()))
            .thenThrow(RuntimeException(null as String?))

        // When
        val result = repository.saveSettings(settings)

        // Then
        assertTrue(result is Result.Error)
        val exception = (result as Result.Error).exception
        assertTrue(exception is AppException.SettingsException)
        assertEquals("Failed to save settings", exception.message)
    }

    @Test
    fun `hasSettings should return true when settings exist`() = runTest {
        // Given
        whenever(settingsLocalDataSource.hasSettings()).thenReturn(true)

        // When
        val result = repository.hasSettings()

        // Then
        assertTrue(result)
    }

    @Test
    fun `hasSettings should return false when settings do not exist`() = runTest {
        // Given
        whenever(settingsLocalDataSource.hasSettings()).thenReturn(false)

        // When
        val result = repository.hasSettings()

        // Then
        assertFalse(result)
    }

    @Test
    fun `hasSettings should call data source exactly once`() = runTest {
        // Given
        whenever(settingsLocalDataSource.hasSettings()).thenReturn(true)

        // When
        repository.hasSettings()

        // Then
        verify(settingsLocalDataSource, times(1)).hasSettings()
    }

    @Test
    fun `saveSettings should return Success with Unit value`() = runTest {
        // Given
        val settings = Settings(
            postalCode = "1000001",
            newsIntervalMinutes = 30,
            rssUrl = "https://test.com/rss",
            enableTts = false,
            enableBgm = true,
            rssPreset = null
        )

        // When
        val result = repository.saveSettings(settings)

        // Then
        assertTrue(result is Result.Success)
        assertEquals(Unit, (result as Result.Success).data)
    }

    @Test
    fun `getSettings should propagate different postal codes correctly`() = runTest {
        // Given
        val mockSettings = Settings(
            postalCode = "5400001",
            newsIntervalMinutes = 45,
            rssUrl = "https://test.com/rss",
            enableTts = false,
            enableBgm = true,
            rssPreset = null
        )
        whenever(settingsLocalDataSource.loadSettings()).thenReturn(mockSettings)

        // When
        val result = repository.getSettings()

        // Then
        assertEquals("5400001", result.postalCode)
        assertEquals(45, result.newsIntervalMinutes)
        // Note: feedwatch flavor forces enableBgm to false
        assertEquals(false, result.enableBgm)
    }

    @Test
    fun `saveSettings should handle multiple consecutive saves`() = runTest {
        // Given
        val settings1 = Settings(
            postalCode = "1000001",
            newsIntervalMinutes = 30,
            rssUrl = "https://test.com/rss",
            enableTts = false,
            enableBgm = true,
            rssPreset = null
        )
        val settings2 = Settings(
            postalCode = "5400001",
            newsIntervalMinutes = 60,
            rssUrl = "https://test.com/rss",
            enableTts = false,
            enableBgm = true,
            rssPreset = null
        )

        // When
        val result1 = repository.saveSettings(settings1)
        val result2 = repository.saveSettings(settings2)

        // Then
        assertTrue(result1 is Result.Success)
        assertTrue(result2 is Result.Success)
        // Note: feedwatch flavor forces enableBgm to false
        verify(settingsLocalDataSource).saveSettings(
            Settings(
                postalCode = "1000001",
                newsIntervalMinutes = 30,
                rssUrl = "https://test.com/rss",
                enableTts = false,
                enableBgm = false,
                rssPreset = null
            )
        )
        verify(settingsLocalDataSource).saveSettings(
            Settings(
                postalCode = "5400001",
                newsIntervalMinutes = 60,
                rssUrl = "https://test.com/rss",
                enableTts = false,
                enableBgm = false,
                rssPreset = null
            )
        )
    }
}
