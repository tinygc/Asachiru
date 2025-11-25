package com.tinygc.asachiru.data.dto

import com.tinygc.asachiru.domain.entity.News
import java.text.SimpleDateFormat
import java.util.*

/**
 * RSS ItemをパースしたDTO
 */
data class NewsDto(
    val title: String,
    val description: String,
    val link: String,
    val pubDate: String // RFC 822形式 (RSS 2.0) または ISO 8601形式 (RSS 1.0/Atom)
) {
    /**
     * NewsエンティティへConverter
     */
    fun toEntity(): News {
        return News(
            id = link, // リンクをIDとして使用
            title = title,
            description = description,
            publishedAt = parsePubDate(pubDate)
        )
    }

    /**
     * pubDateをUnixタイムスタンプに変換
     * RSS 2.0 (RFC 822), RSS 1.0 (ISO 8601), Atom (ISO 8601) の各形式に対応
     */
    private fun parsePubDate(pubDate: String): Long {
        // 試行する日付フォーマットのリスト
        val formats = listOf(
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH), // RSS 2.0 (RFC 822)
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ENGLISH),    // ISO 8601 with timezone
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)     // ISO 8601 UTC
        )

        // 各フォーマットでパースを試みる
        for (format in formats) {
            try {
                val date = format.parse(pubDate)
                if (date != null) {
                    return date.time
                }
            } catch (e: Exception) {
                // 次のフォーマットを試す
                continue
            }
        }

        // すべてのフォーマットでパースに失敗した場合は0Lを返す
        return 0L
    }
}
