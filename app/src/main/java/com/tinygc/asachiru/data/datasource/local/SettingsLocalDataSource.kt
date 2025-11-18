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
        private const val KEY_RSS_URL = "rss_url"
        private const val KEY_ENABLE_TTS = "enable_tts"
        private const val KEY_RSS_PRESET = "rss_preset"
    }

    /**
     * 設定を取得
     * @return 設定
     */
    suspend fun loadSettings(): Settings = withContext(Dispatchers.IO) {
        val postalCode = sharedPreferences.getString(KEY_POSTAL_CODE, "") ?: ""
        val newsInterval = sharedPreferences.getInt(KEY_NEWS_INTERVAL, 30)
        val rssUrl = sharedPreferences.getString(KEY_RSS_URL, null)
        val enableTts = sharedPreferences.getBoolean(KEY_ENABLE_TTS, false)
        val rssPreset = sharedPreferences.getString(KEY_RSS_PRESET, null)

        Settings(
            postalCode = postalCode,
            newsIntervalMinutes = newsInterval,
            rssUrl = rssUrl,
            enableTts = enableTts,
            rssPreset = rssPreset
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
            .putString(KEY_RSS_URL, settings.rssUrl)
            .putBoolean(KEY_ENABLE_TTS, settings.enableTts)
            .putString(KEY_RSS_PRESET, settings.rssPreset)
            .apply()
    }

    /**
     * 設定が存在するかチェック
     * 郵便番号とRSS URLの両方が設定されている場合のみtrue
     * @return 設定が存在する場合true
     */
    suspend fun hasSettings(): Boolean = withContext(Dispatchers.IO) {
        val hasPostalCode = sharedPreferences.contains(KEY_POSTAL_CODE)
        val hasRssUrl = sharedPreferences.contains(KEY_RSS_URL)
        hasPostalCode && hasRssUrl
    }
}
