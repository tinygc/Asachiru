package com.tinygc.asachiru.domain.usecase.music

import com.tinygc.asachiru.domain.entity.Music
import com.tinygc.asachiru.domain.repository.MusicRepository

/**
 * 現在再生中の曲を取得するユースケース
 */
class GetCurrentTrackUseCase(
    private val musicRepository: MusicRepository
) {
    /**
     * 現在再生中の曲を取得
     * @return 現在再生中の曲（再生していない場合null）
     */
    operator fun invoke(): Music? {
        return musicRepository.getCurrentTrack()
    }
}
