package com.tinygc.asachiru.data.repository

import com.tinygc.asachiru.data.datasource.local.SettingsLocalDataSource
import com.tinygc.asachiru.domain.common.AppException
import com.tinygc.asachiru.domain.common.Result
import com.tinygc.asachiru.domain.entity.Settings
import com.tinygc.asachiru.domain.repository.SettingsRepository

/**
 * SettingsRepositoryの実装
 */
class SettingsRepositoryImpl(
    private val settingsLocalDataSource: SettingsLocalDataSource
) : SettingsRepository {

    override suspend fun getSettings(): Settings {
        return settingsLocalDataSource.loadSettings()
    }

    override suspend fun saveSettings(settings: Settings): Result<Unit> {
        return try {
            settingsLocalDataSource.saveSettings(settings)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppException.SettingsException(e.message ?: "Failed to save settings"))
        }
    }

    override suspend fun hasSettings(): Boolean {
        return settingsLocalDataSource.hasSettings()
    }
}
