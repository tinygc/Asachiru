package com.tinygc.asachiru.presentation.main.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * ドットパターンを表示する静的なカスタムビュー
 *
 * 背景のグラデーションの上に薄い白色のドットを規則的に配置し、
 * 装飾的なテクスチャ効果を追加します。
 */
class DotPatternView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        alpha = (255 * 0.15f).toInt() // 15%の不透明度
        style = Paint.Style.FILL
    }

    // ドットのサイズと間隔
    private val dotRadius = 4f // 4pxの円
    private val dotSpacing = 60f // 60px間隔

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 画面全体にドットを規則的に配置
        var y = dotSpacing
        while (y < height) {
            var x = dotSpacing
            while (x < width) {
                canvas.drawCircle(x, y, dotRadius, paint)
                x += dotSpacing
            }
            y += dotSpacing
        }
    }
}
