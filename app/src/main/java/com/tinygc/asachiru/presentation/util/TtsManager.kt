package com.tinygc.asachiru.presentation.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.tinygc.asachiru.domain.common.IMusicPlayer
import com.tinygc.asachiru.domain.common.ITtsManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*
import kotlin.coroutines.resume

/**
 * TTSを管理するマネージャークラス
 *
 * 非同期初期化に対応しており、初期化完了前にspeak()が呼ばれた場合でも
 * 適切に待機してから読み上げを開始します。
 * 読み上げ中は音楽のボリュームを自動的に下げます。
 */
class TtsManager(
    context: Context,
    private val musicPlayer: IMusicPlayer? = null
) : ITtsManager {

    private var tts: TextToSpeech? = null
    private val isReady = CompletableDeferred<Boolean>()

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.JAPANESE
                isReady.complete(true)
            } else {
                isReady.complete(false)
            }
        }
    }

    /**
     * TTS初期化完了を待機
     * @return 初期化成功の場合true
     */
    private suspend fun awaitReady(): Boolean {
        return isReady.await()
    }

    /**
     * テキストを読み上げる（非同期）
     * 初期化完了を待ってから読み上げを開始します。
     * @param text 読み上げるテキスト
     */
    override suspend fun speak(text: String) {
        if (!awaitReady()) {
            // TTS初期化失敗
            return
        }
        // 読み上げ開始前に音楽をダッキング（最初の一回だけ）
        if (musicPlayer != null) {
            // activeUtterancesが0なら新しい読み上げシーケンス開始とみなし音量を下げる
            if (activeUtterances == 0) {
                originalVolume = 1.0f // 現状は常に1.0想定（将来getter導入で置き換え）
                musicPlayer.setVolume(0.3f) // 30%までダッキング（以前20%だったが自然さ優先）
            }
            activeUtterances++
        }
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, text.hashCode().toString())
    }

    /**
     * 読み上げが完了するまで待機
     * UtteranceProgressListenerを使用して読み上げ完了を検知します。
     * 読み上げ中は音楽のボリュームを50%に下げます。
     */
    override suspend fun waitUntilDone() = suspendCancellableCoroutine<Unit> { continuation ->
        val listener = object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                // onStartでは既にダッキング済みなので何もしない
            }

            override fun onDone(utteranceId: String?) {
                restoreVolumeIfNeeded()
                continuation.resume(Unit)
            }

            override fun onError(utteranceId: String?) {
                restoreVolumeIfNeeded()
                continuation.resume(Unit)
            }

            @Deprecated("Deprecated in Java")
            @Suppress("OVERRIDE_DEPRECATION")
            override fun onError(utteranceId: String?, errorCode: Int) {
                restoreVolumeIfNeeded()
                continuation.resume(Unit)
            }
        }

        tts?.setOnUtteranceProgressListener(listener)

        continuation.invokeOnCancellation {
            tts?.stop()
            // キャンセル時もカウンタ調整して必要なら音量復帰
            restoreVolumeIfNeeded()
        }
    }

    /**
     * TTSを停止
     */
    override fun stop() {
        tts?.stop()
    }

    /**
     * リソース解放
     * アプリ終了時に呼び出してください。
     */
    fun shutdown() {
        tts?.shutdown()
        tts = null
    }

    // 読み上げ中管理用（同時読み上げキュー対応）
    private var activeUtterances: Int = 0
    private var originalVolume: Float? = null

    private fun restoreVolumeIfNeeded() {
        if (musicPlayer != null && activeUtterances > 0) {
            activeUtterances--
            if (activeUtterances == 0) {
                // 全ての読み上げが完了したので音量を元に戻す
                musicPlayer.setVolume(originalVolume ?: 1.0f)
                originalVolume = null
            }
        }
    }
}
