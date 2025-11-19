package com.tinygc.asachiru.domain.common

import com.tinygc.asachiru.domain.entity.Music

/**
 * 音楽プレイヤーのインターフェース
 */
interface IMusicPlayer {
    /**
     * 曲を再生
     * @param music 再生する曲
     */
    fun play(music: Music)

    /**
     * 再生を停止
     */
    fun stop()

    /**
     * 現在再生中の曲を取得
     * @return 現在再生中の曲（再生していない場合null）
     */
    fun getCurrentTrack(): Music?

    /**
     * ボリュームを設定
     * @param volume ボリューム（0.0～1.0）
     */
    fun setVolume(volume: Float)

    /**
     * オーディオセッションIDを取得
     * Visualizer等のエフェクト連携に使用
     * @return audioSessionId（未初期化時は0）
     */
    fun getAudioSessionId(): Int

    /**
     * 再生中かどうかを取得
     * @return 再生中ならtrue
     */
    fun isPlaying(): Boolean

    /**
     * 現在の再生位置を取得（ミリ秒）
     * @return 現在の再生位置（再生していない場合は0）
     */
    fun getCurrentPosition(): Long
}
