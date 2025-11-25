package com.tinygc.asachiru.data.util

import com.tinygc.asachiru.data.dto.NewsDto
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream

/**
 * RSSをパースするユーティリティ
 */
object RssParser {
    fun parse(inputStream: InputStream): List<NewsDto> {
        val newsList = mutableListOf<NewsDto>()

        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(inputStream, null)

        var eventType = parser.eventType
        var currentTag: String? = null
        var insideItem = false

        var title: String? = null
        var description: String? = null
        var link: String? = null
        var pubDate: String? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "item") {
                        insideItem = true
                    }
                    currentTag = parser.name
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim()
                    if (text != null && insideItem) {
                        when (currentTag) {
                            "title" -> if (title == null) title = text
                            "description" -> if (description == null) description = text
                            "link" -> if (link == null) link = text
                            "pubDate" -> if (pubDate == null) pubDate = text
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "item") {
                        // itemタグの終了 → NewsDto作成
                        // title, link, pubDateは必須、descriptionはオプショナル
                        if (title != null && link != null && pubDate != null) {
                            newsList.add(
                                NewsDto(
                                    title = title,
                                    description = description ?: "", // descriptionがない場合は空文字列
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
                        insideItem = false
                    }
                }
            }
            eventType = parser.next()
        }

        return newsList
    }
}
