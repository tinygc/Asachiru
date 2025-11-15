package com.tinygc.asachiru.domain.usecase.settings

import com.tinygc.asachiru.domain.entity.Settings
import com.tinygc.asachiru.domain.repository.SettingsRepository

/**
 * 設定を取得するユースケース
 */
class GetSettingsUseCase(
    private val settingsRepository: SettingsRepository
) {
    /**
     * 設定を取得
     * @return 設定
     */
    suspend operator fun invoke(): Settings {
        return settingsRepository.getSettings()
    }
}
