package com.tinygc.asachiru.presentation.util

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import com.tinygc.asachiru.domain.common.IMusicPlayer
import com.tinygc.asachiru.domain.entity.Music

/**
 * 音楽再生を管理するプレイヤークラス
 *
 * MediaPlayerを使ってループ再生を実現します。
 * クロスフェード処理により、曲間をスムーズに繋ぎます。
 */
class MusicPlayer(private val context: Context) : IMusicPlayer {

    companion object {
        private const val FADE_DURATION_MS = 3000L // 3秒
        private const val FADE_STEPS = 20 // フェードのステップ数
    }

    private var currentPlayer: MediaPlayer? = null
    private var nextPlayer: MediaPlayer? = null
    private var currentTrack: Music? = null
    private var trackList: List<Music> = emptyList()
    private var currentTrackIndex = 0
    private val handler = Handler(Looper.getMainLooper())
    private var baseVolume = 1.0f // 基本音量

    /**
     * トラックリストを設定
     * @param tracks トラックリスト
     */
    fun setTrackList(tracks: List<Music>) {
        this.trackList = tracks
    }

    /**
     * 曲を再生
     * トラックリストが設定されている場合は、ループ再生を開始します。
     * @param music 再生する曲
     */
    override fun play(music: Music) {
        stop()

        currentTrack = music
        currentTrackIndex = trackList.indexOf(music)

        currentPlayer = MediaPlayer.create(context, music.resourceId)?.apply {
            setVolume(baseVolume, baseVolume)
            setOnCompletionListener {
                playNext()
            }
            start()
        }

        // 次の曲を準備（クロスフェード用）
        if (trackList.isNotEmpty()) {
            prepareNextTrack()
        }
    }

    /**
     * 次の曲を準備
     */
    private fun prepareNextTrack() {
        if (trackList.isEmpty()) return

        val nextIndex = (currentTrackIndex + 1) % trackList.size
        val nextTrack = trackList[nextIndex]

        nextPlayer?.release()
        nextPlayer = MediaPlayer.create(context, nextTrack.resourceId)
    }

    /**
     * 次の曲を再生（クロスフェード）
     */
    private fun playNext() {
        if (trackList.isEmpty()) return

        // クロスフェード処理
        fadeOut(currentPlayer, FADE_DURATION_MS)

        // 次の曲に移行
        currentPlayer = nextPlayer
        currentTrackIndex = (currentTrackIndex + 1) % trackList.size
        currentTrack = trackList.getOrNull(currentTrackIndex)

        currentPlayer?.apply {
            setVolume(baseVolume, baseVolume)
            setOnCompletionListener {
                playNext()
            }
            start()
        }

        // さらに次の曲を準備
        prepareNextTrack()
    }

    /**
     * フェードアウト処理
     * 指定された時間で音量を徐々に下げる
     * @param player 対象のMediaPlayer
     * @param durationMs フェードアウト時間（ミリ秒）
     */
    private fun fadeOut(player: MediaPlayer?, durationMs: Long) {
        if (player == null) return

        val stepDuration = durationMs / FADE_STEPS
        var currentStep = 0

        val fadeRunnable = object : Runnable {
            override fun run() {
                if (currentStep < FADE_STEPS) {
                    try {
                        if (player.isPlaying) {
                            // 音量を徐々に下げる（1.0 → 0.0）
                            val volume = 1.0f - (currentStep.toFloat() / FADE_STEPS)
                            player.setVolume(volume, volume)
                            currentStep++
                            handler.postDelayed(this, stepDuration)
                        } else {
                            // 既に停止している場合
                            releasePlayer(player)
                        }
                    } catch (e: IllegalStateException) {
                        // MediaPlayerが既に解放されている場合
                        releasePlayer(player)
                    }
                } else {
                    // フェードアウト完了
                    releasePlayer(player)
                }
            }
        }
        handler.post(fadeRunnable)
    }

    /**
     * MediaPlayerを安全に解放
     */
    private fun releasePlayer(player: MediaPlayer?) {
        try {
            player?.setVolume(0f, 0f)
            player?.stop()
            player?.release()
        } catch (e: IllegalStateException) {
            // 既に解放されている場合は無視
        }
    }

    /**
     * 停止
     */
    override fun stop() {
        handler.removeCallbacksAndMessages(null)

        releasePlayer(currentPlayer)
        currentPlayer = null

        releasePlayer(nextPlayer)
        nextPlayer = null

        currentTrack = null
    }

    /**
     * 現在再生中の曲を取得
     * @return 現在再生中の曲（再生していない場合null）
     */
    override fun getCurrentTrack(): Music? {
        return currentTrack
    }

    /**
     * オーディオセッションIDを取得（ビジュアライザー用）
     * @return オーディオセッションID
     */
    override fun getAudioSessionId(): Int {
        return try {
            currentPlayer?.audioSessionId ?: 0
        } catch (e: IllegalStateException) {
            // MediaPlayerが既に解放されている場合
            0
        }
    }

    /**
     * リソース解放
     * アプリ終了時に呼び出してください。
     */
    fun release() {
        stop()
    }

    /**
     * ボリュームを設定
     * @param volume ボリューム（0.0～1.0）
     */
    override fun setVolume(volume: Float) {
        baseVolume = volume.coerceIn(0f, 1f)
        currentPlayer?.setVolume(baseVolume, baseVolume)
    }

    /**
     * 再生中かどうかを取得
     * @return 再生中ならtrue
     */
    override fun isPlaying(): Boolean {
        return try {
            currentPlayer?.isPlaying == true
        } catch (e: IllegalStateException) {
            false
        }
    }

    /**
     * 現在の再生位置を取得（ミリ秒）
     * @return 現在の再生位置（再生していない場合は0）
     */
    override fun getCurrentPosition(): Long {
        return try {
            currentPlayer?.currentPosition?.toLong() ?: 0L
        } catch (e: IllegalStateException) {
            0L
        }
    }
}
