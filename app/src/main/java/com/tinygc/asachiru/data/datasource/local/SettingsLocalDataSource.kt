package com.tinygc.asachiru.data.datasource.local

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tinygc.asachiru.data.RssFeedMigrator
import com.tinygc.asachiru.domain.entity.RssFeed
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
        private const val KEY_ENABLE_BGM = "enable_bgm"
        private const val KEY_RSS_PRESET = "rss_preset"
        private const val KEY_READ_ARTICLE_IDS = "read_article_ids"
        private const val KEY_RSS_FEEDS = "rss_feeds" // 複数RSS対応（新形式）
    }

    private val gson = Gson()

    /**
     * 設定を取得
     * @return 設定
     */
    suspend fun loadSettings(): Settings = withContext(Dispatchers.IO) {
        val postalCode = sharedPreferences.getString(KEY_POSTAL_CODE, "") ?: ""
        val newsInterval = sharedPreferences.getInt(KEY_NEWS_INTERVAL, 30)
        val enableTts = sharedPreferences.getBoolean(KEY_ENABLE_TTS, false)
        val enableBgm = sharedPreferences.getBoolean(KEY_ENABLE_BGM, true)
        val rssPreset = sharedPreferences.getString(KEY_RSS_PRESET, null)
        // 旧形式のURLは現行のプリセット定義に追従させる
        val rssUrl = sharedPreferences.getString(KEY_RSS_URL, null)
            ?.let { RssFeedMigrator.migrateUrl(it, rssPreset) }
        
        // 複数RSS読み込み（JSON形式）
        val rssFeedsJson = sharedPreferences.getString(KEY_RSS_FEEDS, null)
        val rssFeeds = if (rssFeedsJson != null) {
            RssFeedMigrator.migrate(deserializeRssFeeds(rssFeedsJson))
        } else {
            emptyList()
        }

        Settings(
            postalCode = postalCode,
            newsIntervalMinutes = newsInterval,
            rssUrl = rssUrl,
            enableTts = enableTts,
            enableBgm = enableBgm,
            rssPreset = rssPreset,
            rssFeeds = rssFeeds
        )
    }

    /**
     * 設定を保存
     * @param settings 設定
     */
    suspend fun saveSettings(settings: Settings) = withContext(Dispatchers.IO) {
        // 複数RSSをJSON化
        val rssFeedsJson = serializeRssFeeds(settings.rssFeeds)
        
        sharedPreferences.edit()
            .putString(KEY_POSTAL_CODE, settings.postalCode)
            .putInt(KEY_NEWS_INTERVAL, settings.newsIntervalMinutes)
            .putString(KEY_RSS_URL, settings.rssUrl)
            .putBoolean(KEY_ENABLE_TTS, settings.enableTts)
            .putBoolean(KEY_ENABLE_BGM, settings.enableBgm)
            .putString(KEY_RSS_PRESET, settings.rssPreset)
            .putString(KEY_RSS_FEEDS, rssFeedsJson) // 複数RSS保存
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
        val hasRssFeeds = sharedPreferences.contains(KEY_RSS_FEEDS)
        hasPostalCode && (hasRssUrl || hasRssFeeds)
    }

    /**
     * RssFeedリストをJSON化
     */
    private fun serializeRssFeeds(feeds: List<RssFeed>): String {
        return gson.toJson(feeds)
    }

    /**
     * JSONからRssFeedリストをデシリアライズ
     */
    private fun deserializeRssFeeds(json: String): List<RssFeed> {
        return try {
            val type = object : TypeToken<List<RssFeed>>() {}.type
            gson.fromJson<List<RssFeed>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 既読記事IDを取得
     * @return 既読記事IDのセット
     */
    suspend fun getReadArticleIds(): Set<String> = withContext(Dispatchers.IO) {
        sharedPreferences.getStringSet(KEY_READ_ARTICLE_IDS, emptySet()) ?: emptySet()
    }

    /**
     * 記事を既読としてマーク
     * @param articleId 記事ID
     */
    suspend fun markArticleAsRead(articleId: String) = withContext(Dispatchers.IO) {
        val currentIds = sharedPreferences.getStringSet(KEY_READ_ARTICLE_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        currentIds.add(articleId)
        sharedPreferences.edit()
            .putStringSet(KEY_READ_ARTICLE_IDS, currentIds)
            .apply()
    }

    /**
     * 記事が既読かどうか判定
     * @param articleId 記事ID
     * @return 既読の場合true
     */
    suspend fun isArticleRead(articleId: String): Boolean = withContext(Dispatchers.IO) {
        val readIds = sharedPreferences.getStringSet(KEY_READ_ARTICLE_IDS, emptySet()) ?: emptySet()
        readIds.contains(articleId)
    }

    /**
     * 既読記事をクリア(指定されたIDのみ残す)
     * RSSから消えた記事の既読フラグを削除するために使用
     * @param validArticleIds RSSに存在する記事IDのセット
     */
    suspend fun cleanupReadArticles(validArticleIds: Set<String>) = withContext(Dispatchers.IO) {
        val currentIds = sharedPreferences.getStringSet(KEY_READ_ARTICLE_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        val cleanedIds = currentIds.intersect(validArticleIds)
        sharedPreferences.edit()
            .putStringSet(KEY_READ_ARTICLE_IDS, cleanedIds)
            .apply()
    }

    /**
     * 全ての既読記事情報をクリア
     */
    suspend fun clearReadArticles() = withContext(Dispatchers.IO) {
        sharedPreferences.edit()
            .remove(KEY_READ_ARTICLE_IDS)
            .apply()
    }
}

