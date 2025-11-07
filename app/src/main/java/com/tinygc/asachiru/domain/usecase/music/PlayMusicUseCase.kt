package com.tinygc.asachiru.domain.usecase.music

import com.tinygc.asachiru.domain.repository.MusicRepository

/**
 * 音楽を再生するユースケース
 */
class PlayMusicUseCase(
    private val musicRepository: MusicRepository
) {
    /**
     * ループ再生を開始
     */
    operator fun invoke() {
        val tracks = musicRepository.getAllTracks()
        if (tracks.isNotEmpty()) {
            // 最初の曲から再生開始
            musicRepository.playTrack(tracks.first().id)
        }
    }
}
