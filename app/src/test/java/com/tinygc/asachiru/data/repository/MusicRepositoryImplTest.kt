package com.tinygc.asachiru.data.repository

import com.tinygc.asachiru.data.datasource.local.MusicLocalDataSource
import com.tinygc.asachiru.domain.common.IMusicPlayer
import com.tinygc.asachiru.domain.entity.Music
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class MusicRepositoryImplTest {

    private lateinit var musicLocalDataSource: MusicLocalDataSource
    private lateinit var musicPlayer: IMusicPlayer
    private lateinit var repository: MusicRepositoryImpl

    @Before
    fun setUp() {
        musicLocalDataSource = mock()
        musicPlayer = mock()
        repository = MusicRepositoryImpl(musicLocalDataSource, musicPlayer)
    }

    @Test
    fun `getAllTracks should return all tracks from data source`() {
        // Given
        val mockTracks = listOf(
            Music("1", "Track 1", "Artist 1", 1, 180_000L),
            Music("2", "Track 2", "Artist 2", 2, 200_000L),
            Music("3", "Track 3", "Artist 3", 3, 190_000L)
        )
        whenever(musicLocalDataSource.getAllTracks()).thenReturn(mockTracks)

        // When
        val result = repository.getAllTracks()

        // Then
        assertEquals(3, result.size)
        assertEquals(mockTracks, result)
    }

    @Test
    fun `getAllTracks should return empty list when no tracks available`() {
        // Given
        whenever(musicLocalDataSource.getAllTracks()).thenReturn(emptyList())

        // When
        val result = repository.getAllTracks()

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getAllTracks should call data source exactly once`() {
        // Given
        val mockTracks = listOf(Music("1", "Track 1", "Artist 1", 1, 180_000L))
        whenever(musicLocalDataSource.getAllTracks()).thenReturn(mockTracks)

        // When
        repository.getAllTracks()

        // Then
        verify(musicLocalDataSource, times(1)).getAllTracks()
    }

    @Test
    fun `playTrack should play track when track exists`() {
        // Given
        val mockTracks = listOf(
            Music("1", "Track 1", "Artist 1", 1, 180_000L),
            Music("2", "Track 2", "Artist 2", 2, 180_000L)
        )
        whenever(musicLocalDataSource.getAllTracks()).thenReturn(mockTracks)

        // When
        repository.playTrack("1")

        // Then
        verify(musicPlayer).setTrackList(mockTracks)
        verify(musicPlayer).play(mockTracks[0])
    }

    @Test
    fun `playTrack should play correct track by id`() {
        // Given
        val track1 = Music("1", "Track 1", "Artist 1", 1, 180_000L)
        val track2 = Music("2", "Track 2", "Artist 2", 2, 180_000L)
        val mockTracks = listOf(track1, track2)
        whenever(musicLocalDataSource.getAllTracks()).thenReturn(mockTracks)

        // When
        repository.playTrack("2")

        // Then
        verify(musicPlayer).setTrackList(mockTracks)
        verify(musicPlayer).play(track2)
        verify(musicPlayer, never()).play(track1)
    }

    @Test
    fun `playTrack should not play when track does not exist`() {
        // Given
        val mockTracks = listOf(
            Music("1", "Track 1", "Artist 1", 1, 180_000L)
        )
        whenever(musicLocalDataSource.getAllTracks()).thenReturn(mockTracks)

        // When
        repository.playTrack("999")

        // Then
        verify(musicPlayer, never()).setTrackList(any())
        verify(musicPlayer, never()).play(any())
    }

    @Test
    fun `playTrack should not play when track list is empty`() {
        // Given
        whenever(musicLocalDataSource.getAllTracks()).thenReturn(emptyList())

        // When
        repository.playTrack("1")

        // Then
        verify(musicPlayer, never()).setTrackList(any())
        verify(musicPlayer, never()).play(any())
    }

    @Test
    fun `stopTrack should call music player stop`() {
        // When
        repository.stopTrack()

        // Then
        verify(musicPlayer).stop()
    }

    @Test
    fun `stopTrack should call stop exactly once`() {
        // When
        repository.stopTrack()

        // Then
        verify(musicPlayer, times(1)).stop()
    }

    @Test
    fun `getCurrentTrack should return current track from music player`() {
        // Given
        val currentTrack = Music("1", "Current Track", "Artist", 1, 180_000L)
        whenever(musicPlayer.getCurrentTrack()).thenReturn(currentTrack)

        // When
        val result = repository.getCurrentTrack()

        // Then
        assertEquals(currentTrack, result)
    }

    @Test
    fun `getCurrentTrack should return null when no track is playing`() {
        // Given
        whenever(musicPlayer.getCurrentTrack()).thenReturn(null)

        // When
        val result = repository.getCurrentTrack()

        // Then
        assertNull(result)
    }

    @Test
    fun `getCurrentTrack should call music player exactly once`() {
        // Given
        whenever(musicPlayer.getCurrentTrack()).thenReturn(null)

        // When
        repository.getCurrentTrack()

        // Then
        verify(musicPlayer, times(1)).getCurrentTrack()
    }

    @Test
    fun `playTrack should handle multiple plays sequentially`() {
        // Given
        val track1 = Music("1", "Track 1", "Artist 1", 1, 180_000L)
        val track2 = Music("2", "Track 2", "Artist 2", 2, 180_000L)
        val mockTracks = listOf(track1, track2)
        whenever(musicLocalDataSource.getAllTracks()).thenReturn(mockTracks)

        // When
        repository.playTrack("1")
        repository.playTrack("2")

        // Then
        verify(musicPlayer, times(2)).setTrackList(mockTracks)
        verify(musicPlayer).play(track1)
        verify(musicPlayer).play(track2)
    }

    @Test
    fun `playTrack should retrieve tracks each time it is called`() {
        // Given
        val mockTracks = listOf(Music("1", "Track 1", "Artist 1", 1, 180_000L))
        whenever(musicLocalDataSource.getAllTracks()).thenReturn(mockTracks)

        // When
        repository.playTrack("1")
        repository.playTrack("1")

        // Then
        verify(musicLocalDataSource, times(2)).getAllTracks()
    }

    @Test
    fun `getAllTracks should preserve track order from data source`() {
        // Given
        val track1 = Music("1", "First", "Artist A", 1, 180_000L)
        val track2 = Music("2", "Second", "Artist B", 2, 180_000L)
        val track3 = Music("3", "Third", "Artist C", 3, 180_000L)
        val mockTracks = listOf(track1, track2, track3)
        whenever(musicLocalDataSource.getAllTracks()).thenReturn(mockTracks)

        // When
        val result = repository.getAllTracks()

        // Then
        assertEquals("First", result[0].title)
        assertEquals("Second", result[1].title)
        assertEquals("Third", result[2].title)
    }

    @Test
    fun `playTrack should set track list with all 3 tracks for loop playback`() {
        // Given
        val track1 = Music("nakazaki_cho", "中崎町", "Artist A", 1, 180_000L)
        val track2 = Music("se_no_bi", "背の美", "Artist B", 2, 200_000L)
        val track3 = Music("yoru_no_byoshitsu_electro", "夜の病室 Electro", "Artist C", 3, 190_000L)
        val mockTracks = listOf(track1, track2, track3)
        whenever(musicLocalDataSource.getAllTracks()).thenReturn(mockTracks)

        // When
        repository.playTrack("nakazaki_cho")

        // Then
        verify(musicPlayer).setTrackList(mockTracks)
        verify(musicPlayer).play(track1)
    }

    @Test
    fun `playTrack should set all tracks in the list for looping`() {
        // Given
        val mockTracks = listOf(
            Music("1", "Track 1", "Artist 1", 1, 180_000L),
            Music("2", "Track 2", "Artist 2", 2, 200_000L),
            Music("3", "Track 3", "Artist 3", 3, 190_000L)
        )
        whenever(musicLocalDataSource.getAllTracks()).thenReturn(mockTracks)

        // When - Play second track
        repository.playTrack("2")

        // Then - All tracks should be set for looping, but only track 2 should play
        verify(musicPlayer).setTrackList(mockTracks)
        verify(musicPlayer).play(mockTracks[1])
    }
}
