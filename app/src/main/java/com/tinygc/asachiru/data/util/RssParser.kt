package com.tinygc.asachiru.data.util

import com.tinygc.asachiru.data.dto.NewsDto
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream

/**
 * RSS/Atomフィードをパースするユーティリティ
 * RSS 2.0, RSS 1.0 (RDF), Atom形式に対応
 */
object RssParser {
    private enum class FeedFormat {
        RSS_20,    // RSS 2.0 (<rss version="2.0">)
        RSS_10,    // RSS 1.0 (<rdf:RDF>)
        ATOM,      // Atom (<feed>)
        UNKNOWN
    }

    /**
     * HTMLタグを検出したらそこで文字列を切断する
     * 例: "これはテキスト<p>これはHTML</p>" → "これはテキスト"
     */
    private fun removeHtmlTagsAndAfter(text: String): String {
        // HTMLタグの開始（< で始まる）を検出
        val htmlTagIndex = text.indexOf('<')
        return if (htmlTagIndex >= 0) {
            // HTMLタグが見つかったら、そこまでの文字列を返す
            text.substring(0, htmlTagIndex).trim()
        } else {
            // HTMLタグがなければそのまま返す
            text
        }
    }

    fun parse(inputStream: InputStream): List<NewsDto> {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true  // 名前空間を認識
        val parser = factory.newPullParser()
        parser.setInput(inputStream, null)

        // フォーマットを検出
        val format = detectFormat(parser)

        // フォーマットに応じてパース
        return when (format) {
            FeedFormat.RSS_20 -> parseRss20(parser)
            FeedFormat.RSS_10 -> parseRss10(parser)
            FeedFormat.ATOM -> parseAtom(parser)
            FeedFormat.UNKNOWN -> emptyList()
        }
    }

    /**
     * フィードのフォーマットを検出
     */
    private fun detectFormat(parser: XmlPullParser): FeedFormat {
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                return when (parser.name) {
                    "rss" -> FeedFormat.RSS_20
                    "RDF" -> FeedFormat.RSS_10
                    "feed" -> FeedFormat.ATOM
                    else -> FeedFormat.UNKNOWN
                }
            }
            eventType = parser.next()
        }
        return FeedFormat.UNKNOWN
    }

    /**
     * RSS 2.0形式のパース
     */
    private fun parseRss20(parser: XmlPullParser): List<NewsDto> {
        val newsList = mutableListOf<NewsDto>()
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
                    if (text != null && text.isNotEmpty() && insideItem) {
                        when (currentTag) {
                            "title" -> if (title == null) title = text
                            "description" -> if (description == null) description = removeHtmlTagsAndAfter(text)
                            "link" -> if (link == null) link = text
                            "pubDate" -> if (pubDate == null) pubDate = text
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "item") {
                        if (title != null && link != null && pubDate != null) {
                            newsList.add(NewsDto(title, description ?: "", link, pubDate))
                        }
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

    /**
     * RSS 1.0 (RDF)形式のパース
     */
    private fun parseRss10(parser: XmlPullParser): List<NewsDto> {
        val newsList = mutableListOf<NewsDto>()
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
                    if (text != null && text.isNotEmpty() && insideItem) {
                        when (currentTag) {
                            "title" -> if (title == null) title = text
                            "description" -> if (description == null) description = removeHtmlTagsAndAfter(text)
                            "link" -> if (link == null) link = text
                            "date" -> if (pubDate == null) pubDate = text  // dc:date
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "item") {
                        if (title != null && link != null && pubDate != null) {
                            newsList.add(NewsDto(title, description ?: "", link, pubDate))
                        }
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

    /**
     * Atom形式のパース
     */
    private fun parseAtom(parser: XmlPullParser): List<NewsDto> {
        val newsList = mutableListOf<NewsDto>()
        var eventType = parser.eventType
        var currentTag: String? = null
        var insideEntry = false

        var title: String? = null
        var description: String? = null
        var link: String? = null
        var pubDate: String? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "entry") {
                        insideEntry = true
                    }
                    currentTag = parser.name

                    // Atomの<link>は属性にhrefを持つ
                    if (currentTag == "link" && insideEntry && link == null) {
                        // rel="alternate"またはrel属性がないlinkを優先
                        val rel = parser.getAttributeValue(null, "rel")
                        if (rel == null || rel == "alternate") {
                            link = parser.getAttributeValue(null, "href")
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim()
                    if (text != null && text.isNotEmpty() && insideEntry) {
                        when (currentTag) {
                            "title" -> if (title == null) title = text
                            "summary" -> if (description == null) description = removeHtmlTagsAndAfter(text)  // Atomではsummary
                            "content" -> if (description == null) description = removeHtmlTagsAndAfter(text)  // contentも使用
                            "updated" -> if (pubDate == null) pubDate = text  // updated
                            "published" -> if (pubDate == null) pubDate = text  // published
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "entry") {
                        if (title != null && link != null && pubDate != null) {
                            newsList.add(NewsDto(title, description ?: "", link, pubDate))
                        }
                        title = null
                        description = null
                        link = null
                        pubDate = null
                        insideEntry = false
                    }
                }
            }
            eventType = parser.next()
        }
        return newsList
    }
}
