# モジュール設計書 - 時計機能

## 1. 概要

時計機能は、現在の時刻・日付・曜日を表示する機能。デジタル時計形式で、曜日は色分け表示する。

---

## 2. 機能要件（再掲）

### 2.1 表示仕様
- **時刻**: 24時間表示（HH:MM形式、秒は非表示）
- **日付**: MM/DD形式
- **曜日**: 3文字の英語表記（例: Mon）をカッコ付きで表示（例: (Mon)）

### 2.2 曜日の色分け
- 平日（月～金）: 黒
- 土曜: 青
- 日曜: 赤

### 2.3 配置
- 画面左上

### 2.4 更新頻度
- 1秒ごとに更新

---

## 3. アーキテクチャ設計

### 3.1 レイヤー構成

```
[Presentation Layer]
  - ClockView (Custom View)
  - MainViewModel

[Domain Layer]
  - GetCurrentDateTimeUseCase
  - DateTime (Entity)
  - DayOfWeek (Enum)

[Data Layer]
  - なし（システム時刻を使用）
```

---

## 4. クラス設計

### 4.1 Domain Layer

#### 4.1.1 DateTime Entity

```kotlin
package com.tinygc.asachiru.domain.entity

/**
 * 日時を表すエンティティ
 */
data class DateTime(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val second: Int,
    val dayOfWeek: DayOfWeek
) {
    /**
     * 時刻文字列（HH:MM形式）
     */
    val timeString: String
        get() = String.format("%02d:%02d", hour, minute)

    /**
     * 日付文字列（MM/DD (Day)形式）
     */
    val dateString: String
        get() = String.format("%02d/%02d (%s)", month, day, dayOfWeek.shortName)

    companion object {
        /**
         * 空のDateTime（初期値用）
         */
        val EMPTY = DateTime(0, 0, 0, 0, 0, 0, DayOfWeek.SUNDAY)
    }
}
```

#### 4.1.2 DayOfWeek Enum

```kotlin
package com.tinygc.asachiru.domain.entity

/**
 * 曜日を表すEnum
 */
enum class DayOfWeek(val shortName: String) {
    SUNDAY("Sun"),
    MONDAY("Mon"),
    TUESDAY("Tue"),
    WEDNESDAY("Wed"),
    THURSDAY("Thu"),
    FRIDAY("Fri"),
    SATURDAY("Sat");

    /**
     * 曜日の色を取得
     * @return 色コード（Android Color）
     */
    fun getColor(): Int {
        return when (this) {
            SUNDAY -> 0xFFFF0000.toInt() // 赤
            SATURDAY -> 0xFF0000FF.toInt() // 青
            else -> 0xFF000000.toInt() // 黒
        }
    }
}
```

#### 4.1.3 GetCurrentDateTimeUseCase

```kotlin
package com.tinygc.asachiru.domain.usecase.clock

import com.tinygc.asachiru.domain.entity.DateTime
import com.tinygc.asachiru.domain.entity.DayOfWeek
import java.util.Calendar

/**
 * 現在の日時を取得するユースケース
 */
class GetCurrentDateTimeUseCase {
    /**
     * 現在の日時を取得
     * @return 現在のDateTime
     */
    operator fun invoke(): DateTime {
        val calendar = Calendar.getInstance()

        return DateTime(
            year = calendar.get(Calendar.YEAR),
            month = calendar.get(Calendar.MONTH) + 1, // 0-11 → 1-12
            day = calendar.get(Calendar.DAY_OF_MONTH),
            hour = calendar.get(Calendar.HOUR_OF_DAY),
            minute = calendar.get(Calendar.MINUTE),
            second = calendar.get(Calendar.SECOND),
            dayOfWeek = convertCalendarDayToEnum(calendar.get(Calendar.DAY_OF_WEEK))
        )
    }

    /**
     * CalendarのDAY_OF_WEEKをDayOfWeekに変換
     */
    private fun convertCalendarDayToEnum(calendarDay: Int): DayOfWeek {
        return when (calendarDay) {
            Calendar.SUNDAY -> DayOfWeek.SUNDAY
            Calendar.MONDAY -> DayOfWeek.MONDAY
            Calendar.TUESDAY -> DayOfWeek.TUESDAY
            Calendar.WEDNESDAY -> DayOfWeek.WEDNESDAY
            Calendar.THURSDAY -> DayOfWeek.THURSDAY
            Calendar.FRIDAY -> DayOfWeek.FRIDAY
            Calendar.SATURDAY -> DayOfWeek.SATURDAY
            else -> DayOfWeek.SUNDAY // デフォルト
        }
    }
}
```

### 4.2 Presentation Layer

#### 4.2.1 MainViewModel（時計部分のみ抜粋）

```kotlin
package com.tinygc.asachiru.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinygc.asachiru.domain.usecase.clock.GetCurrentDateTimeUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel(
    private val getCurrentDateTimeUseCase: GetCurrentDateTimeUseCase,
    // ... 他のUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        startClockUpdate()
        // ... 他の初期化処理
    }

    /**
     * 時計の定期更新を開始
     */
    private fun startClockUpdate() {
        viewModelScope.launch {
            while (isActive) {
                val dateTime = getCurrentDateTimeUseCase()
                _uiState.update { currentState ->
                    currentState.copy(dateTime = dateTime)
                }
                delay(1000) // 1秒ごとに更新
            }
        }
    }
}
```

#### 4.2.2 ClockView

```kotlin
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
 */
class ClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var currentDateTime: DateTime = DateTime.EMPTY

    private val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 80f
        color = Color.WHITE
        typeface = Typeface.MONOSPACE // ピクセルフォント風（仮）
    }

    private val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 32f
        typeface = Typeface.MONOSPACE
    }

    /**
     * 日時を更新
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
```

#### 4.2.3 MainActivity（時計部分のみ抜粋）

```kotlin
package com.tinygc.asachiru.presentation.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.tinygc.asachiru.R
import com.tinygc.asachiru.presentation.common.ViewModelFactory
import com.tinygc.asachiru.presentation.main.views.ClockView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var clockView: ClockView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this, ViewModelFactory())
            .get(MainViewModel::class.java)

        clockView = findViewById(R.id.clock_view)

        observeViewModel()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                // 時計の更新
                clockView.updateDateTime(state.dateTime)

                // ... 他のUI更新
            }
        }
    }
}
```

---

## 5. テスト設計

### 5.1 単体テスト

#### 5.1.1 GetCurrentDateTimeUseCaseTest

```kotlin
package com.tinygc.asachiru.domain.usecase.clock

import com.tinygc.asachiru.domain.entity.DayOfWeek
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class GetCurrentDateTimeUseCaseTest {

    private val useCase = GetCurrentDateTimeUseCase()

    @Test
    fun `invoke should return current date time`() {
        // Arrange
        val beforeInvoke = Calendar.getInstance()

        // Act
        val result = useCase()

        // Assert
        assertNotNull(result)
        assertEquals(beforeInvoke.get(Calendar.YEAR), result.year)
        assertEquals(beforeInvoke.get(Calendar.MONTH) + 1, result.month)
        assertEquals(beforeInvoke.get(Calendar.DAY_OF_MONTH), result.day)
        assertEquals(beforeInvoke.get(Calendar.HOUR_OF_DAY), result.hour)
        assertEquals(beforeInvoke.get(Calendar.MINUTE), result.minute)
    }

    @Test
    fun `invoke should return correct day of week`() {
        // Act
        val result = useCase()

        // Assert
        assertTrue(result.dayOfWeek in DayOfWeek.values())
    }
}
```

#### 5.1.2 DateTimeTest

```kotlin
package com.tinygc.asachiru.domain.entity

import org.junit.Assert.*
import org.junit.Test

class DateTimeTest {

    @Test
    fun `timeString should format time correctly`() {
        // Arrange
        val dateTime = DateTime(2025, 11, 6, 14, 5, 30, DayOfWeek.WEDNESDAY)

        // Act
        val timeString = dateTime.timeString

        // Assert
        assertEquals("14:05", timeString)
    }

    @Test
    fun `dateString should format date correctly`() {
        // Arrange
        val dateTime = DateTime(2025, 11, 6, 14, 5, 30, DayOfWeek.WEDNESDAY)

        // Act
        val dateString = dateTime.dateString

        // Assert
        assertEquals("11/06 (Wed)", dateString)
    }

    @Test
    fun `EMPTY should have zero values`() {
        // Act
        val empty = DateTime.EMPTY

        // Assert
        assertEquals(0, empty.year)
        assertEquals(0, empty.month)
        assertEquals(0, empty.day)
        assertEquals(0, empty.hour)
        assertEquals(0, empty.minute)
    }
}
```

#### 5.1.3 DayOfWeekTest

```kotlin
package com.tinygc.asachiru.domain.entity

import org.junit.Assert.*
import org.junit.Test

class DayOfWeekTest {

    @Test
    fun `Sunday should return red color`() {
        // Arrange
        val sunday = DayOfWeek.SUNDAY

        // Act
        val color = sunday.getColor()

        // Assert
        assertEquals(0xFFFF0000.toInt(), color)
    }

    @Test
    fun `Saturday should return blue color`() {
        // Arrange
        val saturday = DayOfWeek.SATURDAY

        // Act
        val color = saturday.getColor()

        // Assert
        assertEquals(0xFF0000FF.toInt(), color)
    }

    @Test
    fun `Weekdays should return black color`() {
        // Arrange
        val weekdays = listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        )

        // Act & Assert
        weekdays.forEach { day ->
            assertEquals(0xFF000000.toInt(), day.getColor())
        }
    }
}
```

### 5.2 UIテスト

#### 5.2.1 ClockViewTest

```kotlin
package com.tinygc.asachiru.presentation.main.views

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tinygc.asachiru.domain.entity.DateTime
import com.tinygc.asachiru.domain.entity.DayOfWeek
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClockViewTest {

    private lateinit var clockView: ClockView

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        clockView = ClockView(context)
    }

    @Test
    fun updateDateTime_should_trigger_invalidate() {
        // Arrange
        val dateTime = DateTime(2025, 11, 6, 14, 30, 0, DayOfWeek.WEDNESDAY)

        // Act
        clockView.updateDateTime(dateTime)

        // Assert
        // invalidate()が呼ばれることを確認（Mockitoなどで）
        // または、描画結果をキャプチャして確認
    }
}
```

---

## 6. シーケンス図

```
[MainActivity]       [MainViewModel]       [GetCurrentDateTimeUseCase]     [Calendar]
     |                     |                          |                       |
     |--onCreate()-------->|                          |                       |
     |                     |--init{}----------------->|                       |
     |                     |                          |--getInstance()------->|
     |                     |                          |<---Calendar-----------|
     |                     |<---DateTime--------------|                       |
     |                     |--update uiState--------->|                       |
     |<--StateFlow---------|                          |                       |
     |--updateDateTime()->[ClockView]                 |                       |
     |                     |                          |                       |
     |                     |--delay(1000ms)---------->|                       |
     |                     |                          |                       |
     |                     |--invoke()--------------->|                       |
     |                     |                          |--getInstance()------->|
     |                     |                          |<---Calendar-----------|
     |                     |<---DateTime--------------|                       |
     |                     |--update uiState--------->|                       |
     |<--StateFlow---------|                          |                       |
     |--updateDateTime()->[ClockView]                 |                       |
     |                     |                          |                       |
     |                     | (1秒ごとに繰り返し)         |                       |
```

---

## 7. ファイル構成

```
domain/
├── entity/
│   ├── DateTime.kt
│   └── DayOfWeek.kt
└── usecase/
    └── clock/
        └── GetCurrentDateTimeUseCase.kt

presentation/
└── main/
    ├── MainViewModel.kt
    └── views/
        └── ClockView.kt
```

---

## 8. 承認

- 作成日: 2025-11-06
- 作成者: Claude
- バージョン: 1.0

---

**次のステップ:**
天気機能のモジュール設計書を作成する。
