package com.tinygc.asachiru.data.datasource.local

import android.content.SharedPreferences
import com.tinygc.asachiru.domain.entity.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 設定をローカルに保存・取得するデータソース
 */
class SettingsLocalDataSource(
    private val sharedPreferences: SharedPreferences
) {
    companion object {
        private const val KEY_POSTAL_CODE = "postal_code"
        private const val KEY_NEWS_INTERVAL = "news_interval"
    }

    /**
     * 設定を取得
     * @return 設定
     */
    suspend fun loadSettings(): Settings = withContext(Dispatchers.IO) {
        val postalCode = sharedPreferences.getString(KEY_POSTAL_CODE, "") ?: ""
        val newsInterval = sharedPreferences.getInt(KEY_NEWS_INTERVAL, 30)

        Settings(
            postalCode = postalCode,
            newsIntervalMinutes = newsInterval
        )
    }

    /**
     * 設定を保存
     * @param settings 設定
     */
    suspend fun saveSettings(settings: Settings) = withContext(Dispatchers.IO) {
        sharedPreferences.edit()
            .putString(KEY_POSTAL_CODE, settings.postalCode)
            .putInt(KEY_NEWS_INTERVAL, settings.newsIntervalMinutes)
            .apply()
    }

    /**
     * 設定が存在するかチェック
     * @return 設定が存在する場合true
     */
    suspend fun hasSettings(): Boolean = withContext(Dispatchers.IO) {
        sharedPreferences.contains(KEY_POSTAL_CODE)
    }
}
