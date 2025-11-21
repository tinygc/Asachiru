package com.tinygc.asachiru.di

import android.content.Context
import com.tinygc.asachiru.domain.common.ITtsManager
import com.tinygc.asachiru.domain.usecase.clock.GetCurrentDateTimeUseCase
import com.tinygc.asachiru.domain.usecase.clock.ConvertTimestampToDateTimeUseCase
import com.tinygc.asachiru.domain.usecase.music.GetCurrentTrackUseCase
import com.tinygc.asachiru.domain.usecase.music.PlayMusicUseCase
import com.tinygc.asachiru.domain.usecase.news.GetLatestNewsUseCase
import com.tinygc.asachiru.domain.usecase.news.ReadNewsUseCase
import com.tinygc.asachiru.domain.usecase.settings.CheckSettingsExistUseCase
import com.tinygc.asachiru.domain.usecase.settings.GetSettingsUseCase
import com.tinygc.asachiru.domain.usecase.settings.SaveSettingsUseCase
import com.tinygc.asachiru.domain.usecase.weather.GetWeatherUseCase
import com.tinygc.asachiru.domain.usecase.weather.RefreshWeatherUseCase
import com.tinygc.asachiru.presentation.util.TtsManager

/**
 * UseCaseのファクトリークラス
 *
 * 各UseCaseのインスタンスを生成します。
 */
class UseCaseFactory(private val context: Context) {

    private val repositoryFactory = RepositoryFactory(context)

    // TtsManagerのシングルトンインスタンス
    @Volatile
    private var ttsManager: ITtsManager? = null

    /**
     * TtsManagerのシングルトンインスタンスを取得
     */
    private fun getTtsManager(): ITtsManager {
        return ttsManager ?: synchronized(this) {
            ttsManager ?: TtsManager(
                context = context,
                musicPlayer = repositoryFactory.getMusicPlayer()
            ).also { ttsManager = it }
        }
    }

    /**
     * GetCurrentDateTimeUseCaseのインスタンスを生成
     */
    fun createGetCurrentDateTimeUseCase(): GetCurrentDateTimeUseCase {
        return GetCurrentDateTimeUseCase()
    }

    /**
     * ConvertTimestampToDateTimeUseCaseのインスタンスを生成
     */
    fun createConvertTimestampToDateTimeUseCase(): ConvertTimestampToDateTimeUseCase {
        return ConvertTimestampToDateTimeUseCase()
    }

    /**
     * GetWeatherUseCaseのインスタンスを生成
     */
    fun createGetWeatherUseCase(): GetWeatherUseCase {
        val weatherRepository = repositoryFactory.createWeatherRepository()
        val settingsRepository = repositoryFactory.createSettingsRepository()
        return GetWeatherUseCase(weatherRepository, settingsRepository)
    }

    /**
     * RefreshWeatherUseCaseのインスタンスを生成
     */
    fun createRefreshWeatherUseCase(): RefreshWeatherUseCase {
        val getWeatherUseCase = createGetWeatherUseCase()
        return RefreshWeatherUseCase(getWeatherUseCase)
    }

    /**
     * GetLatestNewsUseCaseのインスタンスを生成
     */
    fun createGetLatestNewsUseCase(): GetLatestNewsUseCase {
        val newsRepository = repositoryFactory.createNewsRepository()
        val readArticleRepository = repositoryFactory.createReadArticleRepository()
        return GetLatestNewsUseCase(newsRepository, readArticleRepository)
    }

    /**
     * ReadNewsUseCaseのインスタンスを生成
     */
    fun createReadNewsUseCase(): ReadNewsUseCase {
        return ReadNewsUseCase(getTtsManager())
    }

    /**
     * PlayMusicUseCaseのインスタンスを生成
     */
    fun createPlayMusicUseCase(): PlayMusicUseCase {
        val musicRepository = repositoryFactory.createMusicRepository()
        return PlayMusicUseCase(musicRepository)
    }

    /**
     * GetCurrentTrackUseCaseのインスタンスを生成
     */
    fun createGetCurrentTrackUseCase(): GetCurrentTrackUseCase {
        val musicRepository = repositoryFactory.createMusicRepository()
        return GetCurrentTrackUseCase(musicRepository)
    }

    /**
     * SaveSettingsUseCaseのインスタンスを生成
     */
    fun createSaveSettingsUseCase(): SaveSettingsUseCase {
        val settingsRepository = repositoryFactory.createSettingsRepository()
        return SaveSettingsUseCase(settingsRepository)
    }

    /**
     * GetSettingsUseCaseのインスタンスを生成
     */
    fun createGetSettingsUseCase(): GetSettingsUseCase {
        val settingsRepository = repositoryFactory.createSettingsRepository()
        return GetSettingsUseCase(settingsRepository)
    }

    /**
     * CheckSettingsExistUseCaseのインスタンスを生成
     */
    fun createCheckSettingsExistUseCase(): CheckSettingsExistUseCase {
        val settingsRepository = repositoryFactory.createSettingsRepository()
        return CheckSettingsExistUseCase(settingsRepository)
    }
}
