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
}
