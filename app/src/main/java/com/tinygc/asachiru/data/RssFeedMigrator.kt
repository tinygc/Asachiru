package com.tinygc.asachiru.data

import com.tinygc.asachiru.domain.entity.RssFeed

/**
 * 設定に保存済みのRSSフィードを、現行のプリセット定義に追従させる
 *
 * プリセットのURLは配信元の都合で変更されることがある (例: Issue #120 のNHK)。
 * 保存済みの設定は選択時点のURLを持ち続けるため、そのままでは取得できなくなる。
 * 読み込み時に以下のルールで読み替えることで、ユーザーの再設定を不要にする。
 *
 * 1. プリセット由来のフィード (isCustom = false) は、IDが現行プリセットに存在すれば
 *    常に [RssPresets] のURLへ追従する。
 *    → 今後プリセットのURLが変わっても [RssPresets] の更新だけで反映される。
 * 2. 上記に該当しないもの (カスタムURL / 廃止されたプリセット) は、
 *    [LEGACY_URL_REPLACEMENTS] に登録された旧URLのみ新URLへ読み替える。
 * 3. どちらにも該当しなければ、ユーザーの指定をそのまま尊重する。
 */
object RssFeedMigrator {

    /**
     * 提供終了・移転した旧URLと、現在のURLのマッピング
     *
     * プリセットのURL変更はルール1で自動的に追従するため、ここへの追加は不要。
     * 登録が必要なのは、旧URLをカスタムURLとして保存しているケースや、
     * プリセット自体が廃止・改名され、IDで解決できなくなったケース。
     */
    val LEGACY_URL_REPLACEMENTS = mapOf(
        // NHKニュースのRSSがNHK ONEへ移行 (Issue #120)
        "https://www3.nhk.or.jp/rss/news/cat0.xml" to "https://news.web.nhk/n-data/conf/na/rss/cat0.xml"
    )

    /**
     * フィードリストを現行の定義に追従させる
     */
    fun migrate(feeds: List<RssFeed>): List<RssFeed> {
        return feeds.map { migrate(it) }
    }

    /**
     * フィードを現行の定義に追従させる
     */
    fun migrate(feed: RssFeed): RssFeed {
        // isCustom=falseならルール1(プリセットIDで解決)、trueならルール2(旧URL読み替えのみ)
        val currentUrl = migrateUrl(feed.url, if (feed.isCustom) null else feed.id)

        return if (currentUrl == feed.url) feed else feed.copy(url = currentUrl)
    }

    /**
     * 旧形式で保存されたURL単体を読み替える
     * @param presetName 旧形式で保存されたプリセット名 (カスタムURLの場合はnull相当)
     */
    fun migrateUrl(url: String, presetName: String? = null): String {
        if (presetName != null && presetName != RssPresets.CUSTOM_URL) {
            RssPresets.getUrl(presetName)?.let { return it }
        }
        return LEGACY_URL_REPLACEMENTS[url] ?: url
    }
}
