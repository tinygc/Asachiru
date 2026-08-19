# Issue 122: 一部のユーザーでエッジ ツー エッジ表示が有効にならないことがある

## 概要
Android 15以降、targetSdk 35以上のアプリはデフォルトでエッジ ツー エッジ表示になる。
本アプリは既に`enableEdgeToEdge()`を`MainActivity` / `SplashActivity` /
`SetupActivity`（asachiru・feedwatch両flavor）で呼び出しているが、Issue報告時点で
「一部のユーザー」でエッジ ツー エッジ表示が有効にならないケースがあるとされていた。

## 原因（仮説）
上記4ファイルにあった`isTelevision()`は、`UiModeManager.currentModeType ==
UI_MODE_TYPE_TELEVISION`という単一のシグナルのみでTV/スマホを判定しており、
TVと判定された場合は`enableEdgeToEdge()`の呼び出し自体をスキップしていた。

`UiModeManager.currentModeType`は端末・OEMスキンによって不安定に報告される
ことがあると一般に知られており、スマートフォンであっても一時的に
`UI_MODE_TYPE_TELEVISION`を返すケースが起こり得る。この場合、該当ユーザーの
端末では`enableEdgeToEdge()`が誤ってスキップされ、エッジ ツー エッジ表示が
有効にならない。

**検証状況（重要・正直に明記）:** 上記は妥当性のある仮説だが、本対応時点では
再現端末・実機ログ・Play Consoleのクラッシュ/ANRレポート等の一次情報は
確認できていない。対症療法として、より安全側（誤ってTVと判定しない側）に
倒す修正を先に入れ、実機からの診断ログ（後述）で仮説を裏付ける方針とした。
なお、旦那の手元環境でビルド成功・実機/エミュレータでの動作確認（エッジ
ツー エッジ表示の有効化・QR誘導表示）は取れているが、これは「修正後の
挙動が意図通りであること」の確認であり、「原因の仮説（UiModeManagerの
誤報告）そのものが正しいこと」の裏付けにはならない。仮説の裏付けは
引き続きFR-122-04の診断ログによる実運用ログ収集に委ねる。

## 機能要件（FR）

### FR-122-01: TV判定の安全側フォールバック
- **入力**: `Context`（`PackageManager` / `UiModeManager`が取得可能な状態）
- **処理**: `PackageManager.hasSystemFeature(FEATURE_LEANBACK)`と
  `UiModeManager.currentModeType == UI_MODE_TYPE_TELEVISION`の両方を評価し、
  AND条件でTV判定する（`DeviceUtils.isStrictTelevision()`）
- **出力**: 両方が真の場合のみ`true`。片方でも偽、または`UiModeManager`が
  取得できない場合は`false`（＝TVではない＝edge-to-edgeを有効にする側）を返す

### FR-122-02: edge-to-edge制御用TV判定の一元化
- **入力**: 各Activityの`onCreate()` / `onConfigurationChanged()`
- **処理**: `MainActivity` / `SplashActivity` / `SetupActivity`（両flavor）の
  private `isTelevision()`は、独自にUiModeManagerを参照せず
  `DeviceUtils.isStrictTelevision(applicationContext)`に委譲する
- **出力**: 4ファイルで判定基準が完全に一致する

### FR-122-03: TV向けUI表示判定との整合性維持
- **入力**: `MainActivity`内のQRコード事前生成判定（`onCreate()`）とQR誘導
  表示判定（`updateQrPromotion()`、`DeviceUtils.isTV()`基準）
- **処理**: 生成判定と表示判定を同じ`DeviceUtils.isTV()`（OR条件）に揃える
- **出力**: 「生成はスキップされたが表示はされる」という食い違いが発生せず、
  QRコード未生成時は誘導コンテナ自体を表示しない
- **背景**: `isTelevision()`をAND条件へ変更した初版の修正で、QR事前生成
  （旧`isTelevision()`基準）とQR誘導表示（`DeviceUtils.isTV()`基準）の判定が
  食い違い、Issue #122の対象端末（Leanbackなし/UIモードのみTV）で
  QRコードが生成されないまま誘導コンテナだけ表示される回帰が発生した
  （コードレビューで検出、コミット`e5928f1`で修正）。この回帰を再発させない
  ための要件として明記する。

### FR-122-04: 診断ログによる仮説検証
- **入力**: `DeviceUtils.isStrictTelevision()`呼び出し時の
  `hasLeanback` / `isTvMode`の評価結果
- **処理**: 2つのシグナルが食い違う場合（`hasLeanback != isTvMode`）のみ、
  `Log.d("DeviceUtils", ...)`で機種情報（`Build.MANUFACTURER` /
  `Build.MODEL` / `Build.VERSION.SDK_INT`）とともに記録する
- **出力**: 実機ログ（Logcat / Firebase Crashlytics等の非致命ログ収集経由）
  で、上記「原因（仮説）」の発生頻度・対象端末を将来的に裏付けられる状態にする
- **完了条件**: 十分なサンプルが集まった時点で、本ドキュメントの「検証状況」を
  実測結果で更新すること（未着手・フォローアップ）

## 非機能要件（NFR）

### NFR-122-01: 安全側フォールバック
- `UiModeManager`が`null`または取得失敗した場合、`isStrictTelevision()`は
  必ず`false`を返す（＝edge-to-edgeを有効なままにする）こと

## 制約事項
- 実装はGradle/AGPのプラグインリポジトリにアクセスできないサンドボックス
  環境で行ったため、実装時点ではビルド・実機/実エミュレータでの動作確認は
  未実施だった。その後、旦那の手元環境で **アプリのビルド成功** と
  **実機/エミュレータでの動作確認**（エッジ ツー エッジ表示が有効になる
  こと・QR誘導が空表示にならないことを含む）を実施済み。
- 一方、`DeviceUtilsTest`（`isStrictTelevision()`向けに追加した5件を含む）
  を`gradlew testAsachiruDebugUnitTest`等で実行してPASSすることは、
  本ドキュメント作成時点ではまだ確認できていない。マージ前に実行を推奨。

## スコープ外
- `DeviceUtils.isTV()` / `isPhone()`自体の判定基準（OR条件）は変更しない
  （広告非表示・キー操作UIなど既存のTV向けUI全体に影響するため、本Issueの
  スコープであるedge-to-edge制御のみに限定する）
- Domain層（`domain/util/DeviceUtils`）がAndroid SDK
  （`UiModeManager`/`PackageManager`/`Log`/`Build`）に直接依存している
  既存のレイヤー制約違反の解消（`design/LayerDefinition.md` 5.4節に
  既知の技術的負債として記録し、別Issue化を推奨する）

## 用語集
- **エッジ ツー エッジ表示（Edge-to-edge）**: システムバー（ステータスバー・
  ナビゲーションバー）の背後までアプリのコンテンツを描画する表示方式
- **Leanback機能**: Android TVアプリであることを示す`PackageManager`の
  システム機能フラグ（`FEATURE_LEANBACK`）。実TV端末は原則として保持する
- **UIモード（UI Mode）**: `UiModeManager.currentModeType`が返す、現在の
  UIコンテキスト種別（通常/TV/デスク/カー等）。TVボックス以外の要因
  （キャスト接続、外部ディスプレイ出力等）でも変化しうる、動的なシグナル

## トレーサビリティ
テストケースとの対応は `test/DeviceUtils_test_result.md` を参照。
