package com.tinygc.asachiru.domain.usecase.news

import com.tinygc.asachiru.domain.common.ITtsManager
import com.tinygc.asachiru.domain.entity.News
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.TimeZone

/**
 * ニュースを読み上げるユースケース
 */
class ReadNewsUseCase(
    private val ttsManager: ITtsManager
) {
    /**
     * ニュースを読み上げる
     * @param newsList 読み上げるニュースリスト
     * @param onNewsChanged 読み上げ中のニュースが変わったときのコールバック
     * @param onComplete すべての読み上げが完了したときのコールバック
     */
    suspend operator fun invoke(
        newsList: List<News>,
        onNewsChanged: (News) -> Unit,
        onComplete: () -> Unit
    ) {
        newsList.forEachIndexed { index, news ->
            // 2つ目以降のニュースの前に間隔を空ける（2秒）
            if (index > 0) {
                delay(2000L)
            }

            // 記事の公開時刻を取得（日本時間）
            val jstTimeZone = TimeZone.getTimeZone("Asia/Tokyo")
            val calendar = Calendar.getInstance(jstTimeZone)
            calendar.timeInMillis = news.publishedAt // 記事の公開時刻を設定
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            // 時刻アナウンスを読み上げ
            val timeAnnouncement = if (index == 0) {
                "${hour}時${minute}分のニュースです。"
            } else {
                "次は、${hour}時${minute}分のニュースです。"
            }
            ttsManager.speak(timeAnnouncement)
            ttsManager.waitUntilDone()

            // アナウンス後の間隔（1秒）
            delay(1000L)

            // ニュースを表示・読み上げ
            onNewsChanged(news)
            ttsManager.speak(news.getSpeechText())
            ttsManager.waitUntilDone()
        }
        onComplete()
    }
}
