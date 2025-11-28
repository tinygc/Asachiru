# キーボード表示制御 実装結果

## 実装概要
- **実施日時**: 2025-11-29
- **対象Issue**: #98
- **実装内容**: スマホでEditTextフォーカス時にソフトキーボードを自動表示

## 実装ファイル

### 1. asachiru/SetupActivity.kt
**パス**: `app/src/asachiru/java/com/tinygc/asachiru/presentation/setup/SetupActivity.kt`

**追加機能**:
- 郵便番号EditTextフォーカス時にキーボード表示
- カスタムURL EditTextフォーカス時にキーボード表示
- TV/スマホ判定による自動制御

**変更内容**:
```kotlin
// import追加
import android.content.Context
import android.view.inputmethod.InputMethodManager
import com.tinygc.asachiru.domain.util.DeviceUtils

// 郵便番号入力欄のOnFocusChangeListener
binding.postalCodeEditText.setOnFocusChangeListener { view, hasFocus ->
    if (hasFocus) {
        // スマホの場合、フォーカス取得時にキーボードを表示
        if (DeviceUtils.isPhone(applicationContext)) {
            showKeyboard(view)
        }
    } else {
        // バリデーション処理
    }
}

// カスタムURL入力欄のフォーカス取得時
binding.rssCustomUrlEditText.postDelayed({
    binding.rssCustomUrlEditText.requestFocus()
    // スマホの場合、キーボードを表示
    if (DeviceUtils.isPhone(applicationContext)) {
        showKeyboard(binding.rssCustomUrlEditText)
    }
}, 100)

// ヘルパーメソッド追加
private fun showKeyboard(view: View) {
    val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    inputMethodManager?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
}
```

### 2. feedwatch/SetupActivity.kt
**パス**: `app/src/feedwatch/java/com/tinygc/asachiru/presentation/setup/SetupActivity.kt`

**変更内容**: asachiru/と同じ実装を適用（BGMチェックボックス除外）

## デバイスタイプ判定ロジック

### スマホ判定条件
```kotlin
DeviceUtils.isPhone(context)
```

**判定基準**:
1. TVデバイスではない (`!isTV(context)`)
2. タッチスクリーンをサポート (`PackageManager.FEATURE_TOUCHSCREEN`)

### TV判定条件
```kotlin
DeviceUtils.isTV(context)
```

**判定基準**:
1. Leanback機能をサポート (`PackageManager.FEATURE_LEANBACK`)
2. UIモードがTV (`UI_MODE_TYPE_TELEVISION`)

## 動作仕様

### スマートフォンの場合
- EditTextにフォーカスが当たると自動的にソフトキーボードが表示される
- InputMethodManager.SHOW_IMPLICITフラグで自然な表示

### Android TVの場合
- EditTextにフォーカスが当たってもキーボードは表示されない（既存動作維持）
- リモコンの数字キー入力を使用

## ビルド結果

### Asachiru
```
✅ assembleAsachiruDebug: BUILD SUCCESSFUL
✅ assembleAsachiruRelease: BUILD SUCCESSFUL
```

### FeedWatch
```
✅ assembleFeedwatchDebug: BUILD SUCCESSFUL
✅ assembleFeedwatchRelease: BUILD SUCCESSFUL
```

**実行時間**: 各1〜2分
**エラー**: なし

## 影響範囲

### 変更ファイル
- `app/src/asachiru/java/com/tinygc/asachiru/presentation/setup/SetupActivity.kt`
- `app/src/feedwatch/java/com/tinygc/asachiru/presentation/setup/SetupActivity.kt`

### 依存関係
- `domain/util/DeviceUtils` (#97で実装済み)
- `android.view.inputmethod.InputMethodManager` (標準API)

## テスト計画

### 手動テスト項目
1. **スマートフォンでの動作確認**
   - [ ] 郵便番号EditTextタップ時にキーボード表示
   - [ ] カスタムURL選択時にキーボード表示
   - [ ] キーボードで文字入力が可能

2. **Android TVでの動作確認**
   - [ ] 郵便番号EditTextフォーカス時にキーボード非表示
   - [ ] リモコンの数字キーで入力可能
   - [ ] 既存動作が維持されている

## 結論
✅ **キーボード表示制御機能の実装完了**

スマートフォンでEditTextにフォーカスした時にソフトキーボードが自動表示され、
Android TVでは従来通りリモコン入力が使える仕様を実装しました。

## 関連Issue
- #97: デバイスタイプ判定機能追加（前提実装）
- #98: キーボード表示制御（本Issue）

## 次のステップ
Phase 6: テスト & 動作確認
- #99: スマホでの動作テスト
- #100: TV/スマホ両対応の動作確認
