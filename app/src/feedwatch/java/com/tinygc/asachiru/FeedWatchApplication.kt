package com.tinygc.asachiru

import android.app.Application
import com.tinygc.asachiru.data.util.PostalCodeConverter

/**
 * FeedWatchアプリケーションクラス
 *
 * アプリ全体の初期化処理を行います。
 */
class FeedWatchApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // PostalCodeConverterの初期化
        // 郵便番号→地域コード変換に必要なマッピングテーブルを読み込む
        PostalCodeConverter.initialize(this)
    }
}
