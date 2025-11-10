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
    fun `getAllTracks should return empty list when no music files are present`() {
        // Act
        val result = dataSource.getAllTracks()

        // Assert
        // TODO: Update these tests when music files are added to res/raw/
        assertEquals(0, result.size)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getAllTracks should return same empty list on multiple calls`() {
        // Act
        val result1 = dataSource.getAllTracks()
        val result2 = dataSource.getAllTracks()

        // Assert
        assertEquals(result1.size, result2.size)
        assertTrue(result1.isEmpty())
        assertTrue(result2.isEmpty())
    }

    // TODO: Uncomment and update these tests when music files are added to res/raw/

    /*
    @Test
    fun `getAllTracks should return list of 3 tracks`() {
        val result = dataSource.getAllTracks()
        assertEquals(3, result.size)
    }

    @Test
    fun `getAllTracks should return tracks with correct IDs`() {
        val result = dataSource.getAllTracks()
        assertEquals("lofi_01", result[0].id)
        assertEquals("lofi_02", result[1].id)
        assertEquals("lofi_03", result[2].id)
    }

    @Test
    fun `getAllTracks should return tracks with correct titles`() {
        val result = dataSource.getAllTracks()
        assertEquals("Chill Morning", result[0].title)
        assertEquals("Peaceful Vibes", result[1].title)
        assertEquals("Relaxing Beats", result[2].title)
    }

    @Test
    fun `getAllTracks should return tracks with valid durations`() {
        val result = dataSource.getAllTracks()
        assertEquals(180_000L, result[0].durationMs)
        assertEquals(200_000L, result[1].durationMs)
        assertEquals(190_000L, result[2].durationMs)
    }
    */
}
