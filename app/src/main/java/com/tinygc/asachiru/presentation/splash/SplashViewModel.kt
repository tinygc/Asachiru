package com.tinygc.asachiru.presentation.splash

import androidx.lifecycle.ViewModel
import com.tinygc.asachiru.domain.usecase.settings.CheckSettingsExistUseCase

/**
 * スプラッシュ画面のViewModel
 *
 * 初回起動時に設定の存在をチェックし、適切な画面へ遷移させます。
 */
class SplashViewModel(
    private val checkSettingsExistUseCase: CheckSettingsExistUseCase
) : ViewModel() {

    /**
     * 設定が存在するかチェック
     *
     * @return 設定が存在する場合true、存在しない場合false
     */
    suspend fun checkSettings(): Boolean {
        return checkSettingsExistUseCase()
    }
}
