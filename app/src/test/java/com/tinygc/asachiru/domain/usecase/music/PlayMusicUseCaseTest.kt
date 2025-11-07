package com.tinygc.asachiru.domain.usecase.music

import com.tinygc.asachiru.domain.entity.Music
import com.tinygc.asachiru.domain.repository.MusicRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class PlayMusicUseCaseTest {

    @Mock
    private lateinit var musicRepository: MusicRepository

    private lateinit var useCase: PlayMusicUseCase

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        useCase = PlayMusicUseCase(musicRepository)
    }

    @Test
    fun `invoke should play first track when tracks are available`() {
        // Arrange
        val tracks = listOf(
            Music("lofi_01", "Chill Morning", "Unknown Artist", 12345, 180000L),
            Music("lofi_02", "Peaceful Vibes", "Unknown Artist", 12346, 200000L),
            Music("lofi_03", "Relaxing Beats", "Unknown Artist", 12347, 190000L)
        )
        whenever(musicRepository.getAllTracks()).thenReturn(tracks)

        // Act
        useCase()

        // Assert
        verify(musicRepository).playTrack("lofi_01")
    }

    @Test
    fun `invoke should not play any track when track list is empty`() {
        // Arrange
        whenever(musicRepository.getAllTracks()).thenReturn(emptyList())

        // Act
        useCase()

        // Assert
        verify(musicRepository, never()).playTrack(org.mockito.kotlin.any())
    }

    @Test
    fun `invoke should always play first track regardless of list size`() {
        // Arrange
        val singleTrack = listOf(
            Music("lofi_01", "Chill Morning", "Unknown Artist", 12345, 180000L)
        )
        whenever(musicRepository.getAllTracks()).thenReturn(singleTrack)

        // Act
        useCase()

        // Assert
        verify(musicRepository).playTrack("lofi_01")
    }

    @Test
    fun `invoke should get all tracks from repository`() {
        // Arrange
        val tracks = listOf(
            Music("lofi_01", "Chill Morning", "Unknown Artist", 12345, 180000L)
        )
        whenever(musicRepository.getAllTracks()).thenReturn(tracks)

        // Act
        useCase()

        // Assert
        verify(musicRepository).getAllTracks()
    }

    @Test
    fun `invoke should handle multiple tracks correctly`() {
        // Arrange
        val tracks = listOf(
            Music("first", "First Song", "Artist 1", 1, 100000L),
            Music("second", "Second Song", "Artist 2", 2, 200000L),
            Music("third", "Third Song", "Artist 3", 3, 300000L),
            Music("fourth", "Fourth Song", "Artist 4", 4, 400000L),
            Music("fifth", "Fifth Song", "Artist 5", 5, 500000L)
        )
        whenever(musicRepository.getAllTracks()).thenReturn(tracks)

        // Act
        useCase()

        // Assert
        verify(musicRepository).playTrack("first")
        verify(musicRepository, never()).playTrack("second")
        verify(musicRepository, never()).playTrack("third")
    }

    @Test
    fun `invoke should play track with correct ID from first music`() {
        // Arrange
        val tracks = listOf(
            Music("unique_id_123", "Test Track", "Test Artist", 999, 150000L)
        )
        whenever(musicRepository.getAllTracks()).thenReturn(tracks)

        // Act
        useCase()

        // Assert
        verify(musicRepository).playTrack("unique_id_123")
    }

    @Test
    fun `invoke should handle repository calls in correct order`() {
        // Arrange
        val tracks = listOf(
            Music("lofi_01", "Chill Morning", "Unknown Artist", 12345, 180000L)
        )
        whenever(musicRepository.getAllTracks()).thenReturn(tracks)

        // Act
        useCase()

        // Assert
        val inOrder = org.mockito.kotlin.inOrder(musicRepository)
        inOrder.verify(musicRepository).getAllTracks()
        inOrder.verify(musicRepository).playTrack("lofi_01")
    }
}
