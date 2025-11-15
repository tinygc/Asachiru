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
    val pubDate: String // RFC 822形式
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
     */
    private fun parsePubDate(pubDate: String): Long {
        return try {
            val format = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
            val date = format.parse(pubDate)
            date?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}
