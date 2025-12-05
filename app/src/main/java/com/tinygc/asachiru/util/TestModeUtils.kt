package com.tinygc.asachiru.util

import com.tinygc.asachiru.domain.entity.Weather
import com.tinygc.asachiru.domain.entity.WeatherCondition
import com.tinygc.asachiru.domain.model.News
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Play Storeスクリーンショット用テストモードのユーティリティ
 * RSS URLに"TEST"が含まれている場合にテストモードとなる
 */
object TestModeUtils {

    /**
     * テストモードかどうかを判定
     * @param rssUrl RSS URL
     * @return "TEST"が含まれていればtrue
     */
    fun isTestMode(rssUrl: String?): Boolean {
        return rssUrl?.uppercase()?.contains("TEST") == true
    }

    /**
     * テスト用のダミー天気データを生成
     * 見栄えの良い晴れの天気を返す
     */
    fun createTestWeather(): Weather {
        return Weather(
            condition = WeatherCondition.SUNNY,
            currentTemperature = 22,
            maxTemperature = 25,
            minTemperature = 18,
            precipitationProbability = 10,
            dateLabel = "今日",
            iconText = "☀"
        )
    }

    /**
     * テスト用のダミー記事データを生成
     * 現在時刻を使って「〇〇時〇〇分、〇〇〇〇～」形式の記事を生成
     */
    fun createTestNews(): List<News> {
        val now = System.currentTimeMillis()
        val timeFormat = SimpleDateFormat("H時mm分", Locale.JAPAN)
        val currentTime = timeFormat.format(Date(now))

        return listOf(
            News(
                id = "test_1",
                title = "${currentTime}、最新ニュースをお届けします",
                description = "これはテスト表示です。実際のアプリでは設定したRSSフィードからニュースを取得して表示します。",
                link = "https://example.com/test1",
                imageUrl = null,
                publishedAt = now
            ),
            News(
                id = "test_2",
                title = "${currentTime}、お天気情報と一緒にニュースをチェック",
                description = "Android TV向けRSSリーダーアプリです。お気に入りのニュースサイトのRSSフィードを登録して、テレビの大画面でニュースを楽しめます。",
                link = "https://example.com/test2",
                imageUrl = null,
                publishedAt = now
            ),
            News(
                id = "test_3",
                title = "${currentTime}、BGM（将来、実装予定）と一緒にリラックスタイム",
                description = "Lo-Fi BGMを聴きながら、ゆったりとニュースをチェック。音声読み上げ機能も搭載しています。",
                link = "https://example.com/test3",
                imageUrl = null,
                publishedAt = now
            )
        )
    }

    /**
     * テスト用のNewsResultを生成
     */
    fun createTestNewsResult(): com.tinygc.asachiru.domain.model.NewsResult {
        val testNews = createTestNews()
        return com.tinygc.asachiru.domain.model.NewsResult(
            allArticles = testNews,
            newArticles = testNews // 全て新規扱い
        )
    }
}
