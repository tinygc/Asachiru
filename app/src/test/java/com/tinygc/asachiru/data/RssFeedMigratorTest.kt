package com.tinygc.asachiru.data

import com.tinygc.asachiru.domain.entity.RssFeed
import org.junit.Assert.*
import org.junit.Test

class RssFeedMigratorTest {

    private val newNhkUrl = "https://news.web.nhk/n-data/conf/na/rss/cat0.xml"
    private val oldNhkUrl = "https://www3.nhk.or.jp/rss/news/cat0.xml"

    @Test
    fun `preset feed should follow current preset url`() {
        // Arrange: 旧URLで保存されたプリセット由来のフィード
        val saved = RssFeed(id = "NHK", name = "NHK", url = oldNhkUrl, isCustom = false)

        // Act
        val result = RssFeedMigrator.migrate(saved)

        // Assert
        assertEquals(newNhkUrl, result.url)
        assertEquals("NHK", result.id)
        assertEquals("NHK", result.name)
        assertFalse(result.isCustom)
    }

    @Test
    fun `preset feed with current url should be returned as is`() {
        // Arrange
        val saved = RssFeed(id = "NHK", name = "NHK", url = newNhkUrl, isCustom = false)

        // Act
        val result = RssFeedMigrator.migrate(saved)

        // Assert
        assertSame(saved, result)
    }

    @Test
    fun `every preset feed should resolve to the preset definition`() {
        // Arrange: 全プリセットを古いURLで保存していたと仮定する
        val saved = RssPresets.PRESETS.keys.map { name ->
            RssFeed(id = name, name = name, url = "https://example.com/outdated", isCustom = false)
        }

        // Act
        val result = RssFeedMigrator.migrate(saved)

        // Assert: プリセット定義が正となる
        result.forEach { feed ->
            assertEquals(RssPresets.getUrl(feed.id), feed.url)
        }
    }

    @Test
    fun `custom feed with legacy url should be replaced`() {
        // Arrange: 旧URLをカスタムURLとして登録していたケース
        val saved = RssFeed(id = "uuid-1", name = "NHKニュース", url = oldNhkUrl, isCustom = true)

        // Act
        val result = RssFeedMigrator.migrate(saved)

        // Assert
        assertEquals(newNhkUrl, result.url)
        assertTrue(result.isCustom)
        assertEquals("NHKニュース", result.name)
    }

    @Test
    fun `custom feed without legacy url should be kept`() {
        // Arrange
        val saved = RssFeed(id = "uuid-2", name = "My Feed", url = "https://example.com/rss", isCustom = true)

        // Act
        val result = RssFeedMigrator.migrate(saved)

        // Assert
        assertSame(saved, result)
    }

    @Test
    fun `removed preset should fall back to legacy url replacement`() {
        // Arrange: プリセットから削除された名前で保存されているケース
        val saved = RssFeed(id = "廃止プリセット", name = "廃止プリセット", url = oldNhkUrl, isCustom = false)

        // Act
        val result = RssFeedMigrator.migrate(saved)

        // Assert
        assertEquals(newNhkUrl, result.url)
    }

    @Test
    fun `unknown preset with unknown url should be kept`() {
        // Arrange
        val saved = RssFeed(id = "廃止プリセット", name = "廃止プリセット", url = "https://example.com/rss", isCustom = false)

        // Act
        val result = RssFeedMigrator.migrate(saved)

        // Assert
        assertSame(saved, result)
    }

    @Test
    fun `migrate should keep list order and size`() {
        // Arrange
        val saved = listOf(
            RssFeed(id = "NHK", name = "NHK", url = oldNhkUrl, isCustom = false),
            RssFeed(id = "uuid-3", name = "My Feed", url = "https://example.com/rss", isCustom = true)
        )

        // Act
        val result = RssFeedMigrator.migrate(saved)

        // Assert
        assertEquals(2, result.size)
        assertEquals(newNhkUrl, result[0].url)
        assertEquals("https://example.com/rss", result[1].url)
    }

    @Test
    fun `legacy url with preset name should follow preset definition`() {
        // Act: 旧形式（rssUrl + rssPreset）で保存されたケース
        val result = RssFeedMigrator.migrateUrl(oldNhkUrl, "NHK")

        // Assert
        assertEquals(newNhkUrl, result)
    }

    @Test
    fun `legacy url with custom preset should use replacement map`() {
        // Act
        val result = RssFeedMigrator.migrateUrl(oldNhkUrl, RssPresets.CUSTOM_URL)

        // Assert
        assertEquals(newNhkUrl, result)
    }

    @Test
    fun `legacy url without preset name should use replacement map`() {
        // Act
        val migrated = RssFeedMigrator.migrateUrl(oldNhkUrl)
        val untouched = RssFeedMigrator.migrateUrl("https://example.com/rss")

        // Assert
        assertEquals(newNhkUrl, migrated)
        assertEquals("https://example.com/rss", untouched)
    }

    @Test
    fun `legacy url replacements should not point to obsolete urls`() {
        // Assert: 置き換え先がさらに旧URLになっていないこと（多段移行の取りこぼし防止）
        RssFeedMigrator.LEGACY_URL_REPLACEMENTS.forEach { (old, new) ->
            assertFalse(
                "移行先 $new がさらに移行対象になっている (元: $old)",
                RssFeedMigrator.LEGACY_URL_REPLACEMENTS.containsKey(new)
            )
        }
    }
}
