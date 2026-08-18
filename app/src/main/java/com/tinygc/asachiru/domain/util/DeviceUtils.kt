package com.tinygc.asachiru.domain.util

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration

/**
 * デバイスタイプ判定ユーティリティ
 *
 * Android TV / スマートフォンを判定して、
 * UI/UXを最適化するためのヘルパー関数を提供します。
 */
object DeviceUtils {

    /**
     * Android TV デバイスかどうかを判定
     *
     * 以下の条件でTVと判定:
     * 1. Leanback機能をサポートしている
     * 2. UIモードがTV
     *
     * TV向けUI（広告非表示、QRコード誘導、キー操作ヒントなど）の表示可否判定に使用する。
     * エッジ ツー エッジ表示の有効化可否の判定には使わないこと。TV誤判定によって
     * `enableEdgeToEdge()` がスキップされる不具合を避けたい場合は [isStrictTelevision] を使う。
     *
     * @param context アプリケーションコンテキスト
     * @return TVデバイスの場合true
     */
    fun isTV(context: Context): Boolean {
        // 1. Leanback機能の有無をチェック
        val hasLeanback = context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)

        // 2. UIモードがTVかチェック
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        val isTvMode = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION

        return hasLeanback || isTvMode
    }

    /**
     * Android TV デバイスかどうかを厳密に判定
     *
     * [isTV] はLeanback機能の有無とUIモードのいずれか一方が真であればtrueを返すが、
     * `UiModeManager.currentModeType` は一部のOEM端末で不安定に報告されることがあり、
     * スマートフォンであっても一時的に `UI_MODE_TYPE_TELEVISION` を返す場合がある。
     * この単一の不安定なシグナルのみでTVと誤判定すると、対象ユーザーで
     * `enableEdgeToEdge()` が呼び出されずエッジ ツー エッジ表示が有効にならない
     * 不具合につながるため、Leanback機能の有無とUIモードの両方が真の場合のみ
     * TVと判定する（AND条件）。
     *
     * Edge-to-edge表示の有効化可否を判定する用途など、TV誤判定を避けたい場面で使用する。
     * 広告非表示・QRコード誘導・キー操作ヒントなどTV向けUIの表示可否判定には使わないこと。
     * それらの判定には[isTV]を使う。
     *
     * @param context アプリケーションコンテキスト
     * @return LeanbackをサポートしていてかつUIモードがTVの場合のみtrue
     */
    fun isStrictTelevision(context: Context): Boolean {
        val hasLeanback = context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)

        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        val isTvMode = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION

        return hasLeanback && isTvMode
    }

    /**
     * スマートフォンデバイスかどうかを判定
     *
     * 以下の条件でスマホと判定:
     * 1. TVデバイスではない
     * 2. タッチスクリーンをサポートしている
     *
     * @param context アプリケーションコンテキスト
     * @return スマホデバイスの場合true
     */
    fun isPhone(context: Context): Boolean {
        // TVでない かつ タッチスクリーンがある
        val hasTouchscreen = context.packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
        return !isTV(context) && hasTouchscreen
    }

    /**
     * タッチスクリーンをサポートしているかを判定
     *
     * @param context アプリケーションコンテキスト
     * @return タッチスクリーンをサポートする場合true
     */
    fun hasTouchscreen(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
    }

    /**
     * 画面サイズカテゴリを取得
     *
     * @param context アプリケーションコンテキスト
     * @return 画面サイズカテゴリ (SCREENLAYOUT_SIZE_SMALL ~ XLARGE)
     */
    fun getScreenSizeCategory(context: Context): Int {
        return context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
    }

    /**
     * 大画面デバイスかどうかを判定
     *
     * @param context アプリケーションコンテキスト
     * @return 大画面（LARGE以上）の場合true
     */
    fun isLargeScreen(context: Context): Boolean {
        val screenSize = getScreenSizeCategory(context)
        return screenSize >= Configuration.SCREENLAYOUT_SIZE_LARGE
    }
}
