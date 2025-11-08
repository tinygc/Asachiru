package com.tinygc.asachiru.data.repository

import com.tinygc.asachiru.data.datasource.local.MusicLocalDataSource
import com.tinygc.asachiru.domain.common.IMusicPlayer
import com.tinygc.asachiru.domain.entity.Music
import com.tinygc.asachiru.domain.repository.MusicRepository

/**
 * MusicRepositoryの実装
 */
class MusicRepositoryImpl(
    private val musicLocalDataSource: MusicLocalDataSource,
    private val musicPlayer: IMusicPlayer
) : MusicRepository {

    override fun getAllTracks(): List<Music> {
        return musicLocalDataSource.getAllTracks()
    }

    override fun playTrack(trackId: String) {
        val track = getAllTracks().find { it.id == trackId }
        track?.let {
            musicPlayer.play(it)
        }
    }

    override fun stopTrack() {
        musicPlayer.stop()
    }

    override fun getCurrentTrack(): Music? {
        return musicPlayer.getCurrentTrack()
    }
}
