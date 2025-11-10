package com.tinygc.asachiru.data.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 郵便番号を地域コードに変換するユーティリティ
 *
 * 実装方針:
 * - 郵便番号の上位3桁をキーとして地域コードにマッピング
 * - マッピングデータはassets/postal_code_mapping.jsonから読み込み
 * - 全国の郵便番号に対応（約1,000エントリ）
 */
object PostalCodeConverter {

    // マッピングテーブル（遅延初期化）
    private var postalCodeToAreaCodeMap: Map<String, String>? = null

    /**
     * マッピングテーブルを初期化
     * @param context Androidコンテキスト
     */
    fun initialize(context: Context) {
        if (postalCodeToAreaCodeMap != null) return

        // assets/postal_code_mapping.jsonから読み込み
        val json = context.assets.open("postal_code_mapping.json").bufferedReader().use {
            it.readText()
        }

        val type = object : TypeToken<Map<String, String>>() {}.type
        postalCodeToAreaCodeMap = Gson().fromJson(json, type)
    }

    /**
     * 郵便番号を地域コードに変換
     * @param postalCode 郵便番号（7桁）
     * @return 地域コード
     * @throws IllegalArgumentException 郵便番号が不正、または変換できない
     * @throws IllegalStateException initialize()が呼ばれていない
     */
    fun convertToAreaCode(postalCode: String): String {
        require(postalCode.length == 7) {
            "郵便番号は7桁で入力してください（例: 1000001）"
        }
        require(postalCode.all { it.isDigit() }) {
            "郵便番号は数字のみで入力してください"
        }

        val map = postalCodeToAreaCodeMap
            ?: throw IllegalStateException("PostalCodeConverter is not initialized")

        // 上位3桁でマッピング
        val prefix = postalCode.substring(0, 3)
        return map[prefix]
            ?: throw IllegalArgumentException(
                "郵便番号「$postalCode」に対応する地域が見つかりませんでした。\n" +
                "郵便番号が正しいかご確認ください。"
            )
    }

    /**
     * テスト用の初期化メソッド
     * サンプルマッピングを使用する
     */
    fun initializeForTest() {
        postalCodeToAreaCodeMap = getSampleMapping()
    }

    /**
     * サンプルのマッピングテーブル（開発用）
     * 実際の実装では、assets/postal_code_mapping.jsonに以下の形式で格納:
     * {
     *   "001": "016010",  // 北海道札幌市
     *   "002": "016010",
     *   ...
     *   "100": "130010",  // 東京都千代田区
     *   "101": "130010",
     *   ...
     *   "530": "270000",  // 大阪府大阪市
     *   ...
     * }
     *
     * 全国の郵便番号→地域コードマッピングは、以下のリソースを参考に作成:
     * - 日本郵便の郵便番号データ
     * - 気象庁の地域コード一覧
     */
    fun getSampleMapping(): Map<String, String> = mapOf(
        // 北海道
        "001" to "016000", "002" to "016000", "003" to "016000", "004" to "016000",
        "060" to "016000", "061" to "016000", "062" to "016000", "063" to "016000",

        // 東京都
        "100" to "130010", "101" to "130010", "102" to "130010", "103" to "130010",
        "104" to "130010", "105" to "130010", "106" to "130010", "107" to "130010",
        "108" to "130010", "109" to "130010", "110" to "130010", "111" to "130010",

        // 大阪府
        "530" to "270000", "531" to "270000", "532" to "270000", "533" to "270000",
        "534" to "270000", "535" to "270000", "536" to "270000", "540" to "270000"

        // その他主要都市は実装時に追加
        // ... 全国約1,000エントリ
    )
}
