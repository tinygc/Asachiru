package com.tinygc.asachiru.domain.usecase.settings

import com.tinygc.asachiru.domain.repository.SettingsRepository

/**
 * 設定が存在するかチェックするユースケース
 */
class CheckSettingsExistUseCase(
    private val settingsRepository: SettingsRepository
) {
    /**
     * 設定が存在するかチェック
     * @return 設定が存在する場合true
     */
    suspend operator fun invoke(): Boolean {
        return settingsRepository.hasSettings()
    }
}
