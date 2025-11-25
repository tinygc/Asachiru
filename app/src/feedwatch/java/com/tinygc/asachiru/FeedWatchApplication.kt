package com.tinygc.asachiru

import android.app.Application
import com.tinygc.asachiru.data.util.PostalCodeConverter
import java.util.TimeZone

/**
 * FeedWatchアプリケーションクラス
 *
 * アプリ全体の初期化処理を行います。
 */
class FeedWatchApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // アプリ全体のデフォルトタイムゾーンを日本時間（JST）に設定
        // これによりSSL証明書の検証やOCSPレスポンスの検証も日本時間で行われる
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))

        // PostalCodeConverterの初期化
        // 郵便番号→地域コード変換に必要なマッピングテーブルを読み込む
        PostalCodeConverter.initialize(this)
    }
}
