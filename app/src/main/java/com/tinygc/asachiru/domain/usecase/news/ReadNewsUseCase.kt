package com.tinygc.asachiru.domain.usecase.news

import com.tinygc.asachiru.domain.common.ITtsManager
import com.tinygc.asachiru.domain.entity.News

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
        newsList.forEach { news ->
            onNewsChanged(news)
            ttsManager.speak(news.getSpeechText())
            ttsManager.waitUntilDone()
        }
        onComplete()
    }
}
