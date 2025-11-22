package com.tinygc.asachiru.presentation.common

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tinygc.asachiru.di.RepositoryFactory
import com.tinygc.asachiru.di.UseCaseFactory
import com.tinygc.asachiru.presentation.main.MainViewModel
import com.tinygc.asachiru.presentation.setup.SetupViewModel
import com.tinygc.asachiru.presentation.splash.SplashViewModel

/**
 * ViewModelのファクトリークラス
 *
 * 各ViewModelのインスタンスを生成し、必要な依存関係を注入します。
 */
class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    private val useCaseFactory = UseCaseFactory(context)
    private val repositoryFactory = RepositoryFactory(context)

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(MainViewModel::class.java) -> {
                MainViewModel(
                    getCurrentDateTimeUseCase = useCaseFactory.createGetCurrentDateTimeUseCase(),
                    convertTimestampToDateTimeUseCase = useCaseFactory.createConvertTimestampToDateTimeUseCase(),
                    getWeatherUseCase = useCaseFactory.createGetWeatherUseCase(),
                    refreshWeatherUseCase = useCaseFactory.createRefreshWeatherUseCase(),
                    getLatestNewsUseCase = useCaseFactory.createGetLatestNewsUseCase(),
                    readNewsUseCase = useCaseFactory.createReadNewsUseCase(),
                    playMusicUseCase = useCaseFactory.createPlayMusicUseCase(),
                    getCurrentTrackUseCase = useCaseFactory.createGetCurrentTrackUseCase(),
                    settingsRepository = repositoryFactory.createSettingsRepository(),
                    readArticleRepository = repositoryFactory.createReadArticleRepository(),
                    musicPlayer = repositoryFactory.getMusicPlayer()
                ) as T
            }
            modelClass.isAssignableFrom(SetupViewModel::class.java) -> {
                SetupViewModel(
                    getSettingsUseCase = useCaseFactory.createGetSettingsUseCase(),
                    saveSettingsUseCase = useCaseFactory.createSaveSettingsUseCase()
                ) as T
            }
            modelClass.isAssignableFrom(SplashViewModel::class.java) -> {
                SplashViewModel(
                    checkSettingsExistUseCase = useCaseFactory.createCheckSettingsExistUseCase()
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
