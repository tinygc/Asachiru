package com.tinygc.asachiru.domain.repository

import com.tinygc.asachiru.domain.entity.Music

/**
 * 音楽情報を管理するリポジトリのインターフェース
 */
interface MusicRepository {
    /**
     * すべての曲を取得
     * @return 曲のリスト
     */
    fun getAllTracks(): List<Music>

    /**
     * 曲を再生
     * @param trackId 曲ID
     */
    fun playTrack(trackId: String)

    /**
     * 再生を停止
     */
    fun stopTrack()

    /**
     * 現在再生中の曲を取得
     * @return 現在再生中の曲（再生していない場合null）
     */
    fun getCurrentTrack(): Music?
}
