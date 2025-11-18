package com.tinygc.asachiru.data.datasource.local

import android.content.Context
import android.media.MediaMetadataRetriever
import com.tinygc.asachiru.R
import com.tinygc.asachiru.domain.entity.Music

/**
 * ローカルの音源データを管理するデータソース
 */
class MusicLocalDataSource(private val context: Context) {
    
    // res/raw/内の音楽ファイル（実際に配置されているファイル名に対応）
    private val rawMusicFiles = listOf(
        Pair("nakazaki_cho", R.raw.nakazaki_cho),
        Pair("se_no_bi", R.raw.se_no_bi),
        Pair("yoru_no_byoshitsu_electro", R.raw.yoru_no_byoshitsu_electro)
    )
    
    /**
     * すべての曲を取得
     * MP3のメタデータ（ID3タグ）から曲名・アーティスト名・再生時間を取得
     * @return 曲のリスト
     */
    fun getAllTracks(): List<Music> {
        return rawMusicFiles.mapNotNull { (id, resourceId) ->
            try {
                val retriever = MediaMetadataRetriever()
                val afd = context.resources.openRawResourceFd(resourceId)
                retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                
                val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    ?: "Unknown Title"
                val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?: "Unknown Artist"
                val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
                
                retriever.release()
                
                Music(
                    id = id,
                    title = title,
                    artist = artist,
                    resourceId = resourceId,
                    durationMs = duration
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
