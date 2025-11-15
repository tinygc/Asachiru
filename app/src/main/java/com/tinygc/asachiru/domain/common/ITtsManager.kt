package com.tinygc.asachiru.domain.common

/**
 * TTS（Text-to-Speech）マネージャーのインターフェース
 */
interface ITtsManager {
    /**
     * テキストを読み上げる（非同期）
     * @param text 読み上げるテキスト
     */
    suspend fun speak(text: String)

    /**
     * 読み上げが完了するまで待機
     */
    suspend fun waitUntilDone()

    /**
     * TTSを停止
     */
    fun stop()
}
