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
