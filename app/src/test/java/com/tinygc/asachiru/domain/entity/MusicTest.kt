package com.tinygc.asachiru.domain.entity

import org.junit.Assert.*
import org.junit.Test

class MusicTest {

    @Test
    fun `Music should hold all music information`() {
        // Arrange & Act
        val music = Music(
            id = "lofi_01",
            title = "Chill Morning",
            artist = "Unknown Artist",
            resourceId = 12345,
            durationMs = 180000L
        )

        // Assert
        assertEquals("lofi_01", music.id)
        assertEquals("Chill Morning", music.title)
        assertEquals("Unknown Artist", music.artist)
        assertEquals(12345, music.resourceId)
        assertEquals(180000L, music.durationMs)
    }

    @Test
    fun `EMPTY should have default values`() {
        // Act
        val empty = Music.EMPTY

        // Assert
        assertEquals("", empty.id)
        assertEquals("", empty.title)
        assertEquals("", empty.artist)
        assertEquals(0, empty.resourceId)
        assertEquals(0L, empty.durationMs)
    }

    @Test
    fun `Music should be data class with correct equality`() {
        // Arrange
        val music1 = Music(
            id = "test_01",
            title = "Test Song",
            artist = "Test Artist",
            resourceId = 100,
            durationMs = 200000L
        )
        val music2 = Music(
            id = "test_01",
            title = "Test Song",
            artist = "Test Artist",
            resourceId = 100,
            durationMs = 200000L
        )
        val music3 = Music(
            id = "test_02",
            title = "Another Song",
            artist = "Another Artist",
            resourceId = 200,
            durationMs = 150000L
        )

        // Assert
        assertEquals(music1, music2)
        assertNotEquals(music1, music3)
    }

    @Test
    fun `Music should handle various duration values`() {
        // Arrange
        val durations = listOf(
            0L,
            60000L,      // 1 minute
            180000L,     // 3 minutes
            600000L,     // 10 minutes
            Long.MAX_VALUE
        )

        // Act & Assert
        durations.forEach { duration ->
            val music = Music(
                id = "test",
                title = "test",
                artist = "test",
                resourceId = 0,
                durationMs = duration
            )
            assertEquals(duration, music.durationMs)
        }
    }

    @Test
    fun `Music should handle different resource IDs`() {
        // Arrange & Act
        val music1 = Music("id1", "title1", "artist1", android.R.drawable.ic_menu_info_details, 100L)
        val music2 = Music("id2", "title2", "artist2", 0, 200L)

        // Assert
        assertTrue(music1.resourceId > 0)
        assertEquals(0, music2.resourceId)
    }
}
