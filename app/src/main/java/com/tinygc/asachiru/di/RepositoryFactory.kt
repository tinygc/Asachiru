package com.tinygc.asachiru.di

import android.content.Context
import com.tinygc.asachiru.data.repository.MusicRepositoryImpl
import com.tinygc.asachiru.data.repository.NewsRepositoryImpl
import com.tinygc.asachiru.data.repository.SettingsRepositoryImpl
import com.tinygc.asachiru.data.repository.WeatherRepositoryImpl
import com.tinygc.asachiru.domain.common.IMusicPlayer
import com.tinygc.asachiru.domain.repository.MusicRepository
import com.tinygc.asachiru.domain.repository.NewsRepository
import com.tinygc.asachiru.domain.repository.SettingsRepository
import com.tinygc.asachiru.domain.repository.WeatherRepository
import com.tinygc.asachiru.presentation.util.MusicPlayer

/**
 * Repositoryのファクトリークラス
 *
 * 各Repositoryのインスタンスを生成します。
 */
class RepositoryFactory(private val context: Context) {

    private val dataSourceFactory = DataSourceFactory(context)

    companion object {
        // 全アプリで共有するMusicPlayer（VisualizerとTTSの連携用に完全シングルトン化）
        @Volatile
        private var sharedMusicPlayer: IMusicPlayer? = null
    }

    /**
     * MusicPlayerのグローバルシングルトン取得
     * 複数のFactory生成時も同一インスタンスを返す
     */
    fun getMusicPlayer(): IMusicPlayer {
        return sharedMusicPlayer ?: synchronized(RepositoryFactory::class.java) {
            sharedMusicPlayer ?: MusicPlayer(context).also { sharedMusicPlayer = it }
        }
    }

    /**
     * WeatherRepositoryのインスタンスを生成
     */
    fun createWeatherRepository(): WeatherRepository {
        val dataSource = dataSourceFactory.createWeatherApiDataSource()
        return WeatherRepositoryImpl(dataSource)
    }

    /**
     * NewsRepositoryのインスタンスを生成
     */
    fun createNewsRepository(): NewsRepository {
        val dataSource = dataSourceFactory.createNewsRssDataSource()
        val settingsRepository = createSettingsRepository()
        return NewsRepositoryImpl(dataSource, settingsRepository)
    }

    /**
     * SettingsRepositoryのインスタンスを生成
     */
    fun createSettingsRepository(): SettingsRepository {
        val dataSource = dataSourceFactory.createSettingsLocalDataSource()
        return SettingsRepositoryImpl(dataSource)
    }

    /**
     * MusicRepositoryのインスタンスを生成
     */
    fun createMusicRepository(): MusicRepository {
        val dataSource = dataSourceFactory.createMusicLocalDataSource()
        return MusicRepositoryImpl(dataSource, getMusicPlayer())
    }
}
