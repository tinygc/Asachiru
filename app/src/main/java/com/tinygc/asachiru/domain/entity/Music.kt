package com.tinygc.asachiru.domain.entity

/**
 * 音楽を表すエンティティ
 */
data class Music(
    val id: String,
    val title: String,
    val artist: String,
    val resourceId: Int, // res/raw/のリソースID
    val durationMs: Long
) {
    companion object {
        /**
         * 空のMusic（初期値用）
         */
        val EMPTY = Music(
            id = "",
            title = "",
            artist = "",
            resourceId = 0,
            durationMs = 0L
        )
    }
}
