package com.tinygc.asachiru.data.datasource.local

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MusicLocalDataSourceTest {

    private lateinit var dataSource: MusicLocalDataSource

    @Before
    fun setup() {
        dataSource = MusicLocalDataSource()
    }

    @Test
    fun `getAllTracks should return list of 3 tracks`() {
        // Act
        val result = dataSource.getAllTracks()

        // Assert
        assertEquals(3, result.size)
    }

    @Test
    fun `getAllTracks should return tracks with correct IDs`() {
        // Act
        val result = dataSource.getAllTracks()

        // Assert
        assertEquals("lofi_01", result[0].id)
        assertEquals("lofi_02", result[1].id)
        assertEquals("lofi_03", result[2].id)
    }

    @Test
    fun `getAllTracks should return tracks with correct titles`() {
        // Act
        val result = dataSource.getAllTracks()

        // Assert
        assertEquals("Chill Morning", result[0].title)
        assertEquals("Peaceful Vibes", result[1].title)
        assertEquals("Relaxing Beats", result[2].title)
    }

    @Test
    fun `getAllTracks should return tracks with correct artists`() {
        // Act
        val result = dataSource.getAllTracks()

        // Assert
        result.forEach { track ->
            assertEquals("Unknown Artist", track.artist)
        }
    }

    @Test
    fun `getAllTracks should return tracks with valid resource IDs`() {
        // Act
        val result = dataSource.getAllTracks()

        // Assert
        result.forEach { track ->
            assertTrue("Resource ID should be greater than 0", track.resourceId > 0)
        }
    }

    @Test
    fun `getAllTracks should return tracks with valid durations`() {
        // Act
        val result = dataSource.getAllTracks()

        // Assert
        assertEquals(180_000L, result[0].durationMs) // 3 minutes
        assertEquals(200_000L, result[1].durationMs) // 3 minutes 20 seconds
        assertEquals(190_000L, result[2].durationMs) // 3 minutes 10 seconds
    }

    @Test
    fun `getAllTracks should return tracks with unique IDs`() {
        // Act
        val result = dataSource.getAllTracks()

        // Assert
        val ids = result.map { it.id }
        val uniqueIds = ids.toSet()
        assertEquals(ids.size, uniqueIds.size)
    }

    @Test
    fun `getAllTracks should return tracks with unique resource IDs`() {
        // Act
        val result = dataSource.getAllTracks()

        // Assert
        val resourceIds = result.map { it.resourceId }
        val uniqueResourceIds = resourceIds.toSet()
        assertEquals(resourceIds.size, uniqueResourceIds.size)
    }

    @Test
    fun `getAllTracks should return tracks in correct order`() {
        // Act
        val result = dataSource.getAllTracks()

        // Assert
        assertEquals("lofi_01", result[0].id)
        assertEquals("lofi_02", result[1].id)
        assertEquals("lofi_03", result[2].id)
    }

    @Test
    fun `getAllTracks should return non-empty list`() {
        // Act
        val result = dataSource.getAllTracks()

        // Assert
        assertFalse(result.isEmpty())
    }

    @Test
    fun `getAllTracks should return same list on multiple calls`() {
        // Act
        val result1 = dataSource.getAllTracks()
        val result2 = dataSource.getAllTracks()

        // Assert
        assertEquals(result1.size, result2.size)
        for (i in result1.indices) {
            assertEquals(result1[i].id, result2[i].id)
            assertEquals(result1[i].title, result2[i].title)
            assertEquals(result1[i].artist, result2[i].artist)
            assertEquals(result1[i].resourceId, result2[i].resourceId)
            assertEquals(result1[i].durationMs, result2[i].durationMs)
        }
    }

    @Test
    fun `getAllTracks should return tracks with all required fields`() {
        // Act
        val result = dataSource.getAllTracks()

        // Assert
        result.forEach { track ->
            assertNotNull(track.id)
            assertNotNull(track.title)
            assertNotNull(track.artist)
            assertTrue("Resource ID must be set", track.resourceId != 0)
            assertTrue("Duration must be greater than 0", track.durationMs > 0)
        }
    }

    @Test
    fun `getAllTracks should return tracks with non-empty IDs`() {
        // Act
        val result = dataSource.getAllTracks()

        // Assert
        result.forEach { track ->
            assertTrue("ID should not be empty", track.id.isNotEmpty())
        }
    }

    @Test
    fun `getAllTracks should return tracks with non-empty titles`() {
        // Act
        val result = dataSource.getAllTracks()

        // Assert
        result.forEach { track ->
            assertTrue("Title should not be empty", track.title.isNotEmpty())
        }
    }

    @Test
    fun `getAllTracks should return tracks with durations in reasonable range`() {
        // Act
        val result = dataSource.getAllTracks()

        // Assert
        result.forEach { track ->
            // Durations should be between 1 minute and 10 minutes for Lo-Fi tracks
            assertTrue("Duration should be at least 1 minute", track.durationMs >= 60_000L)
            assertTrue("Duration should not exceed 10 minutes", track.durationMs <= 600_000L)
        }
    }
}
