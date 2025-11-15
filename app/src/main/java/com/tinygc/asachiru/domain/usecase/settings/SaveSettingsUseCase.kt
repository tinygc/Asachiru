package com.tinygc.asachiru.domain.usecase.settings

import com.tinygc.asachiru.domain.common.AppException
import com.tinygc.asachiru.domain.common.Result
import com.tinygc.asachiru.domain.entity.Settings
import com.tinygc.asachiru.domain.repository.SettingsRepository

/**
 * 設定を保存するユースケース
 */
class SaveSettingsUseCase(
    private val settingsRepository: SettingsRepository
) {
    /**
     * 設定を保存
     * @param settings 設定
     * @return 保存結果（Result型）
     */
    suspend operator fun invoke(settings: Settings): Result<Unit> {
        return if (settings.isValid()) {
            settingsRepository.saveSettings(settings)
        } else {
            Result.Error(AppException.SettingsException("Invalid settings"))
        }
    }
}
