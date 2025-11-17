package com.tinygc.asachiru.data.datasource.local

import com.tinygc.asachiru.R
import com.tinygc.asachiru.domain.entity.Music

/**
 * ローカルの音源データを管理するデータソース
 */
class MusicLocalDataSource {
    /**
     * すべての曲を取得
     * @return 曲のリスト
     */
    fun getAllTracks(): List<Music> {
        // TODO: Add actual music files to res/raw/ directory
        // Uncomment when music files are added to res/raw/
        return listOf(
            Music(
                id = "lofi_01",
                title = "Chill Morning",
                artist = "Unknown Artist",
                resourceId = R.raw.lofi_01,
                durationMs = 180_000L // 3分
            ),
            Music(
                id = "lofi_02",
                title = "Peaceful Vibes",
                artist = "Unknown Artist",
                resourceId = R.raw.lofi_02,
                durationMs = 200_000L // 3分20秒
            ),
            Music(
                id = "lofi_03",
                title = "Relaxing Beats",
                artist = "Unknown Artist",
                resourceId = R.raw.lofi_03,
                durationMs = 190_000L // 3分10秒
            )
        )
    }
}
