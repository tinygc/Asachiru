package com.tinygc.asachiru.data.repository

import com.tinygc.asachiru.data.datasource.local.SettingsLocalDataSource
import com.tinygc.asachiru.domain.common.AppException
import com.tinygc.asachiru.domain.common.Result
import com.tinygc.asachiru.domain.entity.Settings
import com.tinygc.asachiru.domain.repository.SettingsRepository

/**
 * SettingsRepositoryの実装（feedwatch専用 - BGMは常にOFF）
 */
class SettingsRepositoryImpl(
    private val settingsLocalDataSource: SettingsLocalDataSource
) : SettingsRepository {

    override suspend fun getSettings(): Settings {
        // 設定を読み込んだ後、BGMを強制的にOFFにする
        val settings = settingsLocalDataSource.loadSettings()
        return settings.copy(enableBgm = false)
    }

    override suspend fun saveSettings(settings: Settings): Result<Unit> {
        return try {
            // 保存する前に、BGMを強制的にOFFにする
            val modifiedSettings = settings.copy(enableBgm = false)
            settingsLocalDataSource.saveSettings(modifiedSettings)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppException.SettingsException(e.message ?: "Failed to save settings"))
        }
    }

    override suspend fun hasSettings(): Boolean {
        return settingsLocalDataSource.hasSettings()
    }
}
