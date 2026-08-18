package com.tinygc.asachiru.data.datasource.local

import android.content.SharedPreferences
import com.tinygc.asachiru.domain.entity.Settings
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SettingsLocalDataSourceTest {

    @Mock
    private lateinit var sharedPreferences: SharedPreferences

    @Mock
    private lateinit var editor: SharedPreferences.Editor

    private lateinit var dataSource: SettingsLocalDataSource

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        dataSource = SettingsLocalDataSource(sharedPreferences)

        // Setup editor mock chain
        whenever(sharedPreferences.edit()).thenReturn(editor)
        whenever(editor.putString(org.mockito.kotlin.any(), anyOrNull())).thenReturn(editor)
        whenever(editor.putInt(org.mockito.kotlin.any(), org.mockito.kotlin.any())).thenReturn(editor)
        whenever(editor.putBoolean(org.mockito.kotlin.any(), org.mockito.kotlin.any())).thenReturn(editor)
        whenever(editor.putStringSet(org.mockito.kotlin.any(), anyOrNull())).thenReturn(editor)
    }

    @Test
    fun `loadSettings should return settings from SharedPreferences`() = runTest {
        // Arrange
        whenever(sharedPreferences.getString("postal_code", "")).thenReturn("1000001")
        whenever(sharedPreferences.getInt("news_interval", 30)).thenReturn(45)

        // Act
        val result = dataSource.loadSettings()

        // Assert
        assertEquals("1000001", result.postalCode)
        assertEquals(45, result.newsIntervalMinutes)
    }

    @Test
    fun `loadSettings should return default values when keys not found`() = runTest {
        // Arrange
        whenever(sharedPreferences.getString("postal_code", "")).thenReturn("")
        whenever(sharedPreferences.getInt("news_interval", 30)).thenReturn(30)

        // Act
        val result = dataSource.loadSettings()

        // Assert
        assertEquals("", result.postalCode)
        assertEquals(30, result.newsIntervalMinutes)
    }

    @Test
    fun `loadSettings should handle null postal code`() = runTest {
        // Arrange
        whenever(sharedPreferences.getString("postal_code", "")).thenReturn(null)
        whenever(sharedPreferences.getInt("news_interval", 30)).thenReturn(30)

        // Act
        val result = dataSource.loadSettings()

        // Assert
        assertEquals("", result.postalCode)
    }

    @Test
    fun `saveSettings should save postal code and news interval`() = runTest {
        // Arrange
        val settings = Settings(
            postalCode = "1000001",
            newsIntervalMinutes = 30,
            rssUrl = "https://test.com/rss",
            enableTts = false,
            rssPreset = null
        )

        // Act
        dataSource.saveSettings(settings)

        // Assert
        verify(editor).putString("postal_code", "1000001")
        verify(editor).putInt("news_interval", 30)
        verify(editor).putString("rss_url", "https://test.com/rss")
        verify(editor).putBoolean("enable_tts", false)
        verify(editor).putString("rss_preset", null)
        verify(editor).apply()
    }

    @Test
    fun `saveSettings should save different postal codes`() = runTest {
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

            // Act
            dataSource.saveSettings(settings)
        }

        // Assert
        postalCodes.forEach { postalCode ->
            verify(editor).putString("postal_code", postalCode)
        }
        verify(editor, times(4)).putInt("news_interval", 30)
        verify(editor, times(4)).putString("rss_url", "https://test.com/rss")
        verify(editor, times(4)).putBoolean("enable_tts", false)
        verify(editor, times(4)).putString("rss_preset", null)
        verify(editor, times(4)).apply()
    }

    @Test
    fun `saveSettings should save different intervals`() = runTest {
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

            // Act
            dataSource.saveSettings(settings)
        }

        // Assert
        intervals.forEach { interval ->
            verify(editor).putInt("news_interval", interval)
        }
        verify(editor, times(5)).putString("postal_code", "1000001")
        verify(editor, times(5)).putString("rss_url", "https://test.com/rss")
        verify(editor, times(5)).putBoolean("enable_tts", false)
        verify(editor, times(5)).putString("rss_preset", null)
        verify(editor, times(5)).apply()
    }

    @Test
    fun `saveSettings should use apply instead of commit`() = runTest {
        // Arrange
        val settings = Settings(
            postalCode = "1000001",
            newsIntervalMinutes = 30,
            rssUrl = "https://test.com/rss",
            enableTts = false,
            rssPreset = null
        )

        // Act
        dataSource.saveSettings(settings)

        // Assert
        verify(editor).putString("postal_code", "1000001")
        verify(editor).putInt("news_interval", 30)
        verify(editor).putString("rss_url", "https://test.com/rss")
        verify(editor).putBoolean("enable_tts", false)
        verify(editor).putString("rss_preset", null)
        verify(editor).apply()
        verify(editor, never()).commit()
    }

    @Test
    fun `hasSettings should return true when both postal code and rss url exist`() = runTest {
        // Arrange
        whenever(sharedPreferences.contains("postal_code")).thenReturn(true)
        whenever(sharedPreferences.contains("rss_url")).thenReturn(true)

        // Act
        val result = dataSource.hasSettings()

        // Assert
        assertTrue(result)
    }

    @Test
    fun `hasSettings should return false when postal code does not exist`() = runTest {
        // Arrange
        whenever(sharedPreferences.contains("postal_code")).thenReturn(false)
    whenever(sharedPreferences.contains("rss_url")).thenReturn(false)

        // Act
        val result = dataSource.hasSettings()

        // Assert
        assertFalse(result)
    }

    @Test
    fun `hasSettings should check postal_code key`() = runTest {
        // Arrange
        whenever(sharedPreferences.contains("postal_code")).thenReturn(true)

        // Act
        dataSource.hasSettings()

        // Assert
        verify(sharedPreferences).contains("postal_code")
    }

    @Test
    fun `loadSettings should call SharedPreferences with correct keys`() = runTest {
        // Arrange
        whenever(sharedPreferences.getString("postal_code", "")).thenReturn("1000001")
        whenever(sharedPreferences.getInt("news_interval", 30)).thenReturn(30)

        // Act
        dataSource.loadSettings()

        // Assert
        verify(sharedPreferences).getString("postal_code", "")
        verify(sharedPreferences).getInt("news_interval", 30)
    }

    @Test
    fun `saveSettings should save empty postal code`() = runTest {
        // Arrange
        val settings = Settings(
            postalCode = "",
            newsIntervalMinutes = 30,
            rssUrl = "https://test.com/rss",
            enableTts = false,
            rssPreset = null
        )

        // Act
        dataSource.saveSettings(settings)

        // Assert
        verify(editor).putString("postal_code", "")
        verify(editor).putInt("news_interval", 30)
        verify(editor).putString("rss_url", "https://test.com/rss")
        verify(editor).putBoolean("enable_tts", false)
        verify(editor).putString("rss_preset", null)
        verify(editor).apply()
    }

    @Test
    fun `saveSettings should call editor methods in correct order`() = runTest {
        // Arrange
        val settings = Settings(
            postalCode = "1000001",
            newsIntervalMinutes = 45,
            rssUrl = "https://test.com/rss",
            enableTts = false,
            rssPreset = null
        )

        // Act
        dataSource.saveSettings(settings)

        // Assert
        val inOrder = org.mockito.kotlin.inOrder(editor)
        inOrder.verify(editor).putString("postal_code", "1000001")
        inOrder.verify(editor).putInt("news_interval", 45)
        inOrder.verify(editor).putString("rss_url", "https://test.com/rss")
        inOrder.verify(editor).putBoolean("enable_tts", false)
        inOrder.verify(editor).putString("rss_preset", null)
        inOrder.verify(editor).apply()
    }

    @Test
    fun `loadSettings should handle various postal codes`() = runTest {
        // Arrange
        val testCases = mapOf(
            "1000001" to 30,
            "5300001" to 45,
            "0600000" to 15,
            "" to 30
        )

        testCases.forEach { (postalCode, interval) ->
            whenever(sharedPreferences.getString("postal_code", "")).thenReturn(postalCode)
            whenever(sharedPreferences.getInt("news_interval", 30)).thenReturn(interval)

            // Act
            val result = dataSource.loadSettings()

            // Assert
            assertEquals(postalCode, result.postalCode)
            assertEquals(interval, result.newsIntervalMinutes)
        }
    }

    @Test
    fun `saveSettings and loadSettings should work together`() = runTest {
        // This is more of an integration test pattern
        // We'll simulate the behavior by setting up mocks to return saved values

        // Arrange
        val settings = Settings(
            postalCode = "1234567",
            newsIntervalMinutes = 20,
            rssUrl = "https://test.com/rss",
            enableTts = false,
            rssPreset = null
        )

        // Simulate save
        whenever(sharedPreferences.getString("postal_code", "")).thenReturn("1234567")
        whenever(sharedPreferences.getInt("news_interval", 30)).thenReturn(20)

        // Act - Save
        dataSource.saveSettings(settings)

        // Act - Load
        val result = dataSource.loadSettings()

        // Assert
        assertEquals(settings.postalCode, result.postalCode)
        assertEquals(settings.newsIntervalMinutes, result.newsIntervalMinutes)
    }

    @Test
    fun `loadSettings should migrate legacy NHK rss url`() = runTest {
        // Arrange: 旧形式（rss_url + rss_preset）で保存されたNHK
        whenever(sharedPreferences.getString("postal_code", "")).thenReturn("1000001")
        whenever(sharedPreferences.getInt("news_interval", 30)).thenReturn(30)
        whenever(sharedPreferences.getString("rss_url", null))
            .thenReturn("https://www3.nhk.or.jp/rss/news/cat0.xml")
        whenever(sharedPreferences.getString("rss_preset", null)).thenReturn("NHK")

        // Act
        val result = dataSource.loadSettings()

        // Assert
        assertEquals("https://news.web.nhk/n-data/conf/na/rss/cat0.xml", result.rssUrl)
    }

    @Test
    fun `loadSettings should migrate legacy NHK url in rss feeds`() = runTest {
        // Arrange
        whenever(sharedPreferences.getString("postal_code", "")).thenReturn("1000001")
        whenever(sharedPreferences.getInt("news_interval", 30)).thenReturn(30)
        val feedsJson = """[{"id":"NHK","name":"NHK","url":"https://www3.nhk.or.jp/rss/news/cat0.xml","isCustom":false}]"""
        whenever(sharedPreferences.getString("rss_feeds", null)).thenReturn(feedsJson)

        // Act
        val result = dataSource.loadSettings()

        // Assert
        assertEquals(1, result.rssFeeds.size)
        assertEquals("https://news.web.nhk/n-data/conf/na/rss/cat0.xml", result.rssFeeds[0].url)
        assertEquals("NHK", result.rssFeeds[0].name)
    }

    @Test
    fun `loadSettings should keep non migrated rss urls unchanged`() = runTest {
        // Arrange
        whenever(sharedPreferences.getString("postal_code", "")).thenReturn("1000001")
        whenever(sharedPreferences.getInt("news_interval", 30)).thenReturn(30)
        val feedsJson = """[{"id":"custom-1","name":"My Feed","url":"https://example.com/rss","isCustom":true}]"""
        whenever(sharedPreferences.getString("rss_feeds", null)).thenReturn(feedsJson)

        // Act
        val result = dataSource.loadSettings()

        // Assert
        assertEquals("https://example.com/rss", result.rssFeeds[0].url)
    }
}
