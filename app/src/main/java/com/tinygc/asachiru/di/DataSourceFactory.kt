package com.tinygc.asachiru.di

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.tinygc.asachiru.data.datasource.local.MusicLocalDataSource
import com.tinygc.asachiru.data.datasource.local.SettingsLocalDataSource
import com.tinygc.asachiru.data.datasource.remote.NewsRssDataSource
import com.tinygc.asachiru.data.datasource.remote.WeatherApiDataSource
import okhttp3.Interceptor
import okhttp3.OkHttpClient

/**
 * DataSourceのファクトリークラス
 *
 * 各DataSourceのインスタンスを生成します。
 * シングルトンパターンで共有リソース（OkHttpClient, Gson, SharedPreferences）を管理します。
 */
class DataSourceFactory(private val context: Context) {

    companion object {
        private const val SHARED_PREFERENCES_NAME = "asachiru_preferences"
        private const val APP_NAME = "Asachiru"
        private const val APP_VERSION = "1.2.2"
        private const val USER_AGENT = "$APP_NAME/$APP_VERSION"

        // シングルトンインスタンス
        @Volatile
        private var httpClient: OkHttpClient? = null

        @Volatile
        private var gson: Gson? = null
    }

    /**
     * User-Agentヘッダーを追加するInterceptor
     * 天気API（tsukumijima.net）の利用規約に準拠
     */
    private fun createUserAgentInterceptor(): Interceptor {
        return Interceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", USER_AGENT)
                .build()
            chain.proceed(request)
        }
    }

    /**
     * OkHttpClientのシングルトンインスタンスを取得
     */
    private fun getHttpClient(): OkHttpClient {
        return httpClient ?: synchronized(this) {
            httpClient ?: OkHttpClient.Builder()
                .addInterceptor(createUserAgentInterceptor())
                .build()
                .also { httpClient = it }
        }
    }

    /**
     * Gsonのシングルトンインスタンスを取得
     */
    private fun getGson(): Gson {
        return gson ?: synchronized(this) {
            gson ?: Gson().also { gson = it }
        }
    }

    /**
     * SharedPreferencesのインスタンスを取得
     */
    private fun getSharedPreferences(): SharedPreferences {
        return context.getSharedPreferences(
            SHARED_PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )
    }

    /**
     * WeatherApiDataSourceのインスタンスを生成
     */
    fun createWeatherApiDataSource(): WeatherApiDataSource {
        return WeatherApiDataSource(getHttpClient(), getGson())
    }

    /**
     * NewsRssDataSourceのインスタンスを生成
     */
    fun createNewsRssDataSource(): NewsRssDataSource {
        return NewsRssDataSource(getHttpClient())
    }

    /**
     * SettingsLocalDataSourceのインスタンスを生成
     */
    fun createSettingsLocalDataSource(): SettingsLocalDataSource {
        return SettingsLocalDataSource(getSharedPreferences())
    }

    /**
     * MusicLocalDataSourceのインスタンスを生成
     */
    fun createMusicLocalDataSource(): MusicLocalDataSource {
        return MusicLocalDataSource(context)
    }
}
