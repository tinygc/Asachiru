package com.tinygc.asachiru.presentation.main.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import com.tinygc.asachiru.domain.entity.DateTime

/**
 * 時計を表示するカスタムビュー
 *
 * 時刻（HH:MM）と日付（MM/DD (Day)）を表示します。
 * 曜日は色分け表示されます：
 * - 日曜: 赤
 * - 土曜: 青
 * - 平日: 黒
 */
class ClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var currentDateTime: DateTime = DateTime.EMPTY

    private val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 120f
        color = Color.WHITE
        typeface = Typeface.MONOSPACE
    }

    private val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 48f
        typeface = Typeface.MONOSPACE
    }

    /**
     * 日時を更新
     * 即座に再描画が行われます。
     * @param dateTime 表示する日時
     */
    fun updateDateTime(dateTime: DateTime) {
        this.currentDateTime = dateTime
        invalidate() // 再描画をリクエスト
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (currentDateTime == DateTime.EMPTY) {
            return // 初期値の場合は何も描画しない
        }

        drawTime(canvas)
        drawDate(canvas)
    }

    /**
     * 時刻を描画
     */
    private fun drawTime(canvas: Canvas) {
        val x = 50f
        val y = 100f

        canvas.drawText(currentDateTime.timeString, x, y, timePaint)
    }

    /**
     * 日付を描画（曜日の色分けあり）
     */
    private fun drawDate(canvas: Canvas) {
        val x = 50f
        val y = 150f

        // 曜日の色を設定
        datePaint.color = currentDateTime.dayOfWeek.getColor()

        canvas.drawText(currentDateTime.dateString, x, y, datePaint)
    }
}
