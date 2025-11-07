package com.tinygc.asachiru.domain.common

/**
 * 処理結果を表すsealed class
 * Success: 成功時のデータを保持
 * Error: 失敗時の例外を保持
 */
sealed class Result<out T> {
    /**
     * 成功結果
     * @param data 成功時のデータ
     */
    data class Success<T>(val data: T) : Result<T>()

    /**
     * エラー結果
     * @param exception エラー情報
     */
    data class Error(val exception: Exception) : Result<Nothing>()
}
