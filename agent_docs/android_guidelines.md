# Android開発ガイドライン

## Multi-Flavor テスト戦略
- flavor 毎に異なる実装になる可能性がある
- テストは flavor 毎に分離（`src/{flavor}Test/`）
- 共有テストディレクトリ（`src/test/`）は共通ロジックのみ
- 修正後は `./gradlew clean test` で強制実行

## Mockito テストのベストプラクティス
- data class のテストでは verify() の引数が完全一致必要
- 部分的なマッチは `any()` matcher を使用
- 複数フィールド検証は `assertEquals()` で個別アサート

## StandardTestDispatcher と ViewModel
- init block を持つ ViewModel では `skipAutoStart = true` を使用
- 問題のあるテストはコメントアウトして理由を記載

## UI/UX実装の注意点
- ImageViewとDrawableのサイズ不一致 → マイナスマージンまたはscaleType調整
- 解像度依存を避ける → `dp` を使用
- 透過表示（alpha）で控えめなUI
- ADBでスクリーンショット確認を必ず実施

## Canvas描画とView座標系
- Y座標は下方向が正（画面上部が0）
- View 内の描画は View の範囲内に限定
- View外に表示したい場合は XML に別の View を追加
- 「上」の解釈（Y座標的 vs Z軸的）が曖昧な場合はユーザーに確認

## ユーザー指示の理解
- 「変えない」の範囲を明確に
- 指摘された問題点を理解してから次のコードを書く
- Y座標の計算が「上」か「下」かビルド前に確認

## レイアウト変更が反映されない場合のデバッグ手順 (2025/12/05追加)

### 問題の症状
- XMLでレイアウトサイズを変更してもアプリに反映されない
- クリーンビルド、アンインストール、エミュレータ再起動しても効果なし

### 原因究明の手順
1. **ログで実際の値を確認** - `Log.d()` で `height`, `width`, `density` を出力
2. **APK内のリソースを確認** - `aapt dump xmltree` コマンドでAPKに正しい値が入っているか確認
3. **コードでの上書きを疑う** - `layoutParams` を動的に設定している箇所を検索
   ```kotlin
   // 検索キーワード
   grep -r "layoutParams" --include="*.kt"
   grep -r "LayoutParams" --include="*.kt"
   ```

### 今回の原因
`MainActivity.kt` の `updateNewsViewLayout()` 関数で、XMLの設定を無視してコードで強制的にサイズを設定していた：
```kotlin
// 問題のコード
layoutParams.height = resources.displayMetrics.density.toInt() * 100 // 100dp固定
```

### 教訓
- **XMLだけでなく、Kotlinコードでの動的レイアウト変更を必ず確認する**
- ViewBindingを使っている場合、`layoutParams` の変更箇所を検索
- 「詳細表示モード」「全画面モード」などUIモード切替がある場合は特に注意
- フレーバー別の分岐は `BuildConfig.FLAVOR` で判定

### 正しい実装パターン
レイアウトサイズはXMLで管理し、コードでは一時的な変更（全画面表示など）のみ行う：
```kotlin
// XMLの元のレイアウトを保存
private var originalLayoutParams: ConstraintLayout.LayoutParams? = null

private fun updateLayout(isFullScreen: Boolean) {
    if (isFullScreen) {
        // 元のレイアウトを保存してから全画面に変更
        if (originalLayoutParams == null) {
            originalLayoutParams = ConstraintLayout.LayoutParams(binding.view.layoutParams)
        }
        // 全画面設定...
    } else {
        // XMLで定義されたレイアウトを復元
        originalLayoutParams?.let { original ->
            binding.view.layoutParams = ConstraintLayout.LayoutParams(original)
            originalLayoutParams = null
        }
    }
}
```

### デバッグ用ログの例
```kotlin
android.util.Log.d("ViewName", "height=$height, density=${context.resources.displayMetrics.density}")
```
