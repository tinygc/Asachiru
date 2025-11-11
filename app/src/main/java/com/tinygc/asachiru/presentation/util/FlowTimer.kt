package com.tinygc.asachiru.presentation.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 定期的にイベントを発行するFlowを生成するユーティリティ
 */
object FlowTimer {
    /**
     * 指定された間隔でUnitを発行するFlowを生成
     *
     * @param intervalMillis 発行間隔（ミリ秒）
     * @param initialDelayMillis 初回発行までの遅延（ミリ秒、デフォルト0）
     * @return 定期的にUnitを発行するFlow
     */
    fun ticker(
        intervalMillis: Long,
        initialDelayMillis: Long = 0L
    ): Flow<Unit> = flow {
        if (initialDelayMillis > 0) {
            delay(initialDelayMillis)
        }
        while (true) {
            emit(Unit)
            delay(intervalMillis)
        }
    }
}
