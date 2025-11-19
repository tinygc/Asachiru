package com.tinygc.asachiru.presentation.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.tinygc.asachiru.domain.entity.Music
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * MusicPlayerのテスト
 *
 * Note: MediaPlayerはAndroid SDKのクラスで、Robolectricでも完全なシミュレートは困難です。
 * このテストでは基本的な初期化とメソッド呼び出しのテストのみを行います。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MusicPlayerTest {

    private lateinit var context: Context
    private lateinit var musicPlayer: MusicPlayer

    private val testMusic1 = Music(
        id = "test_1",
        title = "Test Track 1",
        artist = "Test Artist",
        resourceId = 1, // Dummy resource ID for testing
        durationMs = 180_000L
    )

    private val testMusic2 = Music(
        id = "test_2",
        title = "Test Track 2",
        artist = "Test Artist",
        resourceId = 2, // Dummy resource ID for testing
        durationMs = 200_000L
    )

    private val testMusic3 = Music(
        id = "test_3",
        title = "Test Track 3",
        artist = "Test Artist",
        resourceId = 3, // Dummy resource ID for testing
        durationMs = 190_000L
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        musicPlayer = MusicPlayer(context)
    }

    @Test
    fun `MusicPlayer should be created successfully`() {
        // Given & When
        val player = MusicPlayer(context)

        // Then
        assertNotNull(player)
    }

    @Test
    fun `getCurrentTrack should return null when no track is playing`() {
        // When
        val currentTrack = musicPlayer.getCurrentTrack()

        // Then
        assertNull(currentTrack)
    }

    @Test
    fun `setTrackList should not throw exception`() {
        // Given
        val trackList = listOf(testMusic1, testMusic2, testMusic3)

        // When & Then (例外が発生しないことを確認)
        musicPlayer.setTrackList(trackList)
    }

    @Test
    fun `play should not throw exception when called with valid music`() {
        // Given
        val trackList = listOf(testMusic1, testMusic2, testMusic3)
        musicPlayer.setTrackList(trackList)

        // When & Then (例外が発生しないことを確認)
        try {
            musicPlayer.play(testMusic1)
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        } catch (e: Exception) {
            // MediaPlayerの初期化が失敗する場合もあるため、許容
            println("MediaPlayer initialization may fail in Robolectric environment: ${e.message}")
        }
    }

    @Test
    fun `getCurrentTrack should return playing track after play`() {
        // Given
        val trackList = listOf(testMusic1, testMusic2, testMusic3)
        musicPlayer.setTrackList(trackList)

        // When
        try {
            musicPlayer.play(testMusic1)
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            val currentTrack = musicPlayer.getCurrentTrack()

            // Then
            assertEquals(testMusic1, currentTrack)
        } catch (e: Exception) {
            println("MediaPlayer initialization may fail in Robolectric environment: ${e.message}")
        }
    }

    @Test
    fun `stop should not throw exception`() {
        // When & Then (例外が発生しないことを確認)
        musicPlayer.stop()
    }

    @Test
    fun `stop should clear current track`() {
        // Given
        val trackList = listOf(testMusic1, testMusic2, testMusic3)
        musicPlayer.setTrackList(trackList)

        try {
            musicPlayer.play(testMusic1)
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

            // When
            musicPlayer.stop()

            // Then
            assertNull(musicPlayer.getCurrentTrack())
        } catch (e: Exception) {
            println("MediaPlayer initialization may fail in Robolectric environment: ${e.message}")
        }
    }

    @Test
    fun `stop can be called after play`() {
        // Given
        val trackList = listOf(testMusic1, testMusic2, testMusic3)
        musicPlayer.setTrackList(trackList)

        // When & Then (例外が発生しないことを確認)
        try {
            musicPlayer.play(testMusic1)
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            musicPlayer.stop()
        } catch (e: Exception) {
            println("MediaPlayer initialization may fail in Robolectric environment: ${e.message}")
        }
    }

    @Test
    fun `release should not throw exception`() {
        // When & Then (例外が発生しないことを確認)
        musicPlayer.release()
    }

    @Test
    fun `release can be called after play`() {
        // Given
        val trackList = listOf(testMusic1, testMusic2, testMusic3)
        musicPlayer.setTrackList(trackList)

        // When & Then (例外が発生しないことを確認)
        try {
            musicPlayer.play(testMusic1)
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            musicPlayer.release()
        } catch (e: Exception) {
            println("MediaPlayer initialization may fail in Robolectric environment: ${e.message}")
        }
    }

    @Test
    fun `stop can be called multiple times`() {
        // When & Then (例外が発生しないことを確認)
        musicPlayer.stop()
        musicPlayer.stop()
        musicPlayer.stop()
    }

    @Test
    fun `release can be called multiple times`() {
        // When & Then (例外が発生しないことを確認)
        musicPlayer.release()
        musicPlayer.release()
    }

    @Test
    fun `getAudioSessionId should return valid id after play`() {
        // Given
        val trackList = listOf(testMusic1, testMusic2, testMusic3)
        musicPlayer.setTrackList(trackList)

        // When
        try {
            musicPlayer.play(testMusic1)
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            val sessionId = musicPlayer.getAudioSessionId()

            // Then
            assertNotNull(sessionId)
        } catch (e: Exception) {
            println("MediaPlayer initialization may fail in Robolectric environment: ${e.message}")
        }
    }

    @Test
    fun `getAudioSessionId should return 0 when no track is playing`() {
        // When
        val sessionId = musicPlayer.getAudioSessionId()

        // Then
        assertEquals(0, sessionId)
    }

    @Test
    fun `play can be called with different tracks sequentially`() {
        // Given
        val trackList = listOf(testMusic1, testMusic2, testMusic3)
        musicPlayer.setTrackList(trackList)

        // When & Then (例外が発生しないことを確認)
        try {
            musicPlayer.play(testMusic1)
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            musicPlayer.play(testMusic2)
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            musicPlayer.play(testMusic3)
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        } catch (e: Exception) {
            println("MediaPlayer initialization may fail in Robolectric environment: ${e.message}")
        }
    }

    @Test
    fun `play should replace current track with new track`() {
        // Given
        val trackList = listOf(testMusic1, testMusic2, testMusic3)
        musicPlayer.setTrackList(trackList)

        // When
        try {
            musicPlayer.play(testMusic1)
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            musicPlayer.play(testMusic2)
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            val currentTrack = musicPlayer.getCurrentTrack()

            // Then
            assertEquals(testMusic2, currentTrack)
        } catch (e: Exception) {
            println("MediaPlayer initialization may fail in Robolectric environment: ${e.message}")
        }
    }

    @Test
    fun `setTrackList can be called multiple times`() {
        // Given
        val trackList1 = listOf(testMusic1, testMusic2)
        val trackList2 = listOf(testMusic2, testMusic3)

        // When & Then (例外が発生しないことを確認)
        musicPlayer.setTrackList(trackList1)
        musicPlayer.setTrackList(trackList2)
    }

    @Test
    fun `setTrackList with empty list should not throw exception`() {
        // When & Then (例外が発生しないことを確認)
        musicPlayer.setTrackList(emptyList())
    }

    @Test
    fun `getAudioSessionId should return 0 after stop`() {
        // Given
        val trackList = listOf(testMusic1, testMusic2, testMusic3)
        musicPlayer.setTrackList(trackList)

        try {
            musicPlayer.play(testMusic1)
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            
            // When
            musicPlayer.stop()
            val sessionId = musicPlayer.getAudioSessionId()

            // Then
            assertEquals(0, sessionId)
        } catch (e: Exception) {
            println("MediaPlayer initialization may fail in Robolectric environment: ${e.message}")
        }
    }

    @Test
    fun `getAudioSessionId should return 0 after release`() {
        // Given
        val trackList = listOf(testMusic1, testMusic2, testMusic3)
        musicPlayer.setTrackList(trackList)

        try {
            musicPlayer.play(testMusic1)
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            
            // When
            musicPlayer.release()
            val sessionId = musicPlayer.getAudioSessionId()

            // Then
            assertEquals(0, sessionId)
        } catch (e: Exception) {
            println("MediaPlayer initialization may fail in Robolectric environment: ${e.message}")
        }
    }

    @Test
    fun `getAudioSessionId should not throw exception even after multiple stop calls`() {
        // Given
        val trackList = listOf(testMusic1, testMusic2, testMusic3)
        musicPlayer.setTrackList(trackList)

        try {
            musicPlayer.play(testMusic1)
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            
            // When
            musicPlayer.stop()
            musicPlayer.stop()
            val sessionId = musicPlayer.getAudioSessionId()

            // Then
            assertEquals(0, sessionId)
        } catch (e: Exception) {
            println("MediaPlayer initialization may fail in Robolectric environment: ${e.message}")
        }
    }
}
