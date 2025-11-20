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
        // BGMは常時30%固定のため、ダッキング処理は不要
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, text.hashCode().toString())
    }

    /**
     * 読み上げが完了するまで待機
     * UtteranceProgressListenerを使用して読み上げ完了を検知します。
     */
    override suspend fun waitUntilDone() = suspendCancellableCoroutine<Unit> { continuation ->
        val listener = object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                // 何もしない
            }

            override fun onDone(utteranceId: String?) {
                continuation.resume(Unit)
            }

            override fun onError(utteranceId: String?) {
                continuation.resume(Unit)
            }

            @Deprecated("Deprecated in Java")
            @Suppress("OVERRIDE_DEPRECATION")
            override fun onError(utteranceId: String?, errorCode: Int) {
                continuation.resume(Unit)
            }
        }

        tts?.setOnUtteranceProgressListener(listener)

        continuation.invokeOnCancellation {
            tts?.stop()
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
}
