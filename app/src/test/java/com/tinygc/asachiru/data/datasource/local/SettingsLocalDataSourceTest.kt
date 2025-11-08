package com.tinygc.asachiru.data.datasource.local

import android.content.SharedPreferences
import com.tinygc.asachiru.domain.entity.Settings
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
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
        whenever(editor.putString(org.mockito.kotlin.any(), org.mockito.kotlin.any())).thenReturn(editor)
        whenever(editor.putInt(org.mockito.kotlin.any(), org.mockito.kotlin.any())).thenReturn(editor)
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
        val settings = Settings("1000001", 30)

        // Act
        dataSource.saveSettings(settings)

        // Assert
        verify(editor).putString("postal_code", "1000001")
        verify(editor).putInt("news_interval", 30)
        verify(editor).apply()
    }

    @Test
    fun `saveSettings should save different postal codes`() = runTest {
        // Arrange
        val postalCodes = listOf("1000001", "5300001", "0600000", "9999999")

        postalCodes.forEach { postalCode ->
            val settings = Settings(postalCode, 30)

            // Act
            dataSource.saveSettings(settings)

            // Assert
            verify(editor).putString("postal_code", postalCode)
        }
    }

    @Test
    fun `saveSettings should save different intervals`() = runTest {
        // Arrange
        val intervals = listOf(1, 15, 30, 45, 60)

        intervals.forEach { interval ->
            val settings = Settings("1000001", interval)

            // Act
            dataSource.saveSettings(settings)

            // Assert
            verify(editor).putInt("news_interval", interval)
        }
    }

    @Test
    fun `saveSettings should use apply instead of commit`() = runTest {
        // Arrange
        val settings = Settings("1000001", 30)

        // Act
        dataSource.saveSettings(settings)

        // Assert
        verify(editor).apply()
        verify(editor, org.mockito.kotlin.never()).commit()
    }

    @Test
    fun `hasSettings should return true when postal code exists`() = runTest {
        // Arrange
        whenever(sharedPreferences.contains("postal_code")).thenReturn(true)

        // Act
        val result = dataSource.hasSettings()

        // Assert
        assertTrue(result)
    }

    @Test
    fun `hasSettings should return false when postal code does not exist`() = runTest {
        // Arrange
        whenever(sharedPreferences.contains("postal_code")).thenReturn(false)

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
        val settings = Settings("", 30)

        // Act
        dataSource.saveSettings(settings)

        // Assert
        verify(editor).putString("postal_code", "")
        verify(editor).putInt("news_interval", 30)
        verify(editor).apply()
    }

    @Test
    fun `saveSettings should call editor methods in correct order`() = runTest {
        // Arrange
        val settings = Settings("1000001", 45)

        // Act
        dataSource.saveSettings(settings)

        // Assert
        val inOrder = org.mockito.kotlin.inOrder(editor)
        inOrder.verify(editor).putString("postal_code", "1000001")
        inOrder.verify(editor).putInt("news_interval", 45)
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
        val settings = Settings("1234567", 20)

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
}
