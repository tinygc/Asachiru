package com.tinygc.asachiru.data.util

import com.tinygc.asachiru.data.dto.NewsDto
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream

/**
 * RSSをパースするユーティリティ
 */
object RssParser {
    /**
     * RSS XMLをパースしてNewsDtoリストを返す
     * @param inputStream RSS XMLのInputStream
     * @return NewsDtoリスト
     */
    fun parse(inputStream: InputStream): List<NewsDto> {
        val newsList = mutableListOf<NewsDto>()

        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(inputStream, null)

        var eventType = parser.eventType
        var currentTag: String? = null

        var title: String? = null
        var description: String? = null
        var link: String? = null
        var pubDate: String? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                }
                XmlPullParser.TEXT -> {
                    when (currentTag) {
                        "title" -> title = parser.text
                        "description" -> description = parser.text
                        "link" -> link = parser.text
                        "pubDate" -> pubDate = parser.text
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "item") {
                        // itemタグの終了 → NewsDto作成
                        if (title != null && description != null && link != null && pubDate != null) {
                            newsList.add(
                                NewsDto(
                                    title = title,
                                    description = description,
                                    link = link,
                                    pubDate = pubDate
                                )
                            )
                        }
                        // リセット
                        title = null
                        description = null
                        link = null
                        pubDate = null
                    }
                }
            }
            eventType = parser.next()
        }

        return newsList
    }
}
