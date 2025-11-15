package com.tinygc.asachiru.domain.usecase.music

import com.tinygc.asachiru.domain.entity.Music
import com.tinygc.asachiru.domain.repository.MusicRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class GetCurrentTrackUseCaseTest {

    @Mock
    private lateinit var musicRepository: MusicRepository

    private lateinit var useCase: GetCurrentTrackUseCase

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        useCase = GetCurrentTrackUseCase(musicRepository)
    }

    @Test
    fun `invoke should return current track when music is playing`() {
        // Arrange
        val currentTrack = Music(
            id = "lofi_01",
            title = "Chill Morning",
            artist = "Unknown Artist",
            resourceId = 12345,
            durationMs = 180000L
        )
        whenever(musicRepository.getCurrentTrack()).thenReturn(currentTrack)

        // Act
        val result = useCase()

        // Assert
        assertNotNull(result)
        assertEquals(currentTrack, result)
    }

    @Test
    fun `invoke should return null when no music is playing`() {
        // Arrange
        whenever(musicRepository.getCurrentTrack()).thenReturn(null)

        // Act
        val result = useCase()

        // Assert
        assertNull(result)
    }

    @Test
    fun `invoke should call repository getCurrentTrack method`() {
        // Arrange
        whenever(musicRepository.getCurrentTrack()).thenReturn(null)

        // Act
        useCase()

        // Assert
        verify(musicRepository).getCurrentTrack()
    }

    @Test
    fun `invoke should return correct track data`() {
        // Arrange
        val track = Music(
            id = "test_id_123",
            title = "Test Song Title",
            artist = "Test Artist Name",
            resourceId = 99999,
            durationMs = 250000L
        )
        whenever(musicRepository.getCurrentTrack()).thenReturn(track)

        // Act
        val result = useCase()

        // Assert
        assertNotNull(result)
        assertEquals("test_id_123", result?.id)
        assertEquals("Test Song Title", result?.title)
        assertEquals("Test Artist Name", result?.artist)
        assertEquals(99999, result?.resourceId)
        assertEquals(250000L, result?.durationMs)
    }

    @Test
    fun `invoke should handle different music tracks`() {
        // Arrange
        val tracks = listOf(
            Music("lofi_01", "Chill Morning", "Artist 1", 1001, 180000L),
            Music("lofi_02", "Peaceful Vibes", "Artist 2", 1002, 200000L),
            Music("lofi_03", "Relaxing Beats", "Artist 3", 1003, 190000L)
        )

        tracks.forEach { track ->
            // Arrange
            whenever(musicRepository.getCurrentTrack()).thenReturn(track)

            // Act
            val result = useCase()

            // Assert
            assertEquals(track, result)
        }
    }

    @Test
    fun `invoke should return EMPTY music when repository returns EMPTY`() {
        // Arrange
        whenever(musicRepository.getCurrentTrack()).thenReturn(Music.EMPTY)

        // Act
        val result = useCase()

        // Assert
        assertNotNull(result)
        assertEquals(Music.EMPTY, result)
        assertEquals("", result?.id)
        assertEquals("", result?.title)
        assertEquals("", result?.artist)
        assertEquals(0, result?.resourceId)
        assertEquals(0L, result?.durationMs)
    }

    @Test
    fun `invoke should handle consecutive calls correctly`() {
        // Arrange
        val track1 = Music("id1", "Song 1", "Artist 1", 1, 100000L)
        val track2 = Music("id2", "Song 2", "Artist 2", 2, 200000L)

        whenever(musicRepository.getCurrentTrack())
            .thenReturn(track1)
            .thenReturn(track2)

        // Act
        val result1 = useCase()
        val result2 = useCase()

        // Assert
        assertEquals(track1, result1)
        assertEquals(track2, result2)
        assertNotEquals(result1, result2)
    }

    @Test
    fun `invoke should directly delegate to repository`() {
        // Arrange
        val track = Music("lofi_01", "Chill Morning", "Unknown Artist", 12345, 180000L)
        whenever(musicRepository.getCurrentTrack()).thenReturn(track)

        // Act
        val result = useCase()

        // Assert
        verify(musicRepository).getCurrentTrack()
        assertEquals(track, result)
    }

    @Test
    fun `invoke should handle music with zero duration`() {
        // Arrange
        val track = Music("id", "title", "artist", 123, 0L)
        whenever(musicRepository.getCurrentTrack()).thenReturn(track)

        // Act
        val result = useCase()

        // Assert
        assertNotNull(result)
        assertEquals(0L, result?.durationMs)
    }

    @Test
    fun `invoke should handle music with very long duration`() {
        // Arrange
        val track = Music("id", "title", "artist", 123, Long.MAX_VALUE)
        whenever(musicRepository.getCurrentTrack()).thenReturn(track)

        // Act
        val result = useCase()

        // Assert
        assertNotNull(result)
        assertEquals(Long.MAX_VALUE, result?.durationMs)
    }
}
