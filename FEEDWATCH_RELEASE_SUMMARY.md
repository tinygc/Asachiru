# RSS FeedWatch - Play Store リリース 完全ガイド

## 📋 概要

このドキュメントは、RSS FeedWatchをGoogle Play Storeにリリースするための完全ガイドです。
すべての手順を順番に実行することで、スムーズにリリースできます。

---

## ✅ リリース準備チェックリスト

### 事前準備

- [ ] Google Play Developer アカウント登録済み（$25 USD、1回限り）
- [ ] Google AdMob アカウント作成済み
- [ ] FeedWatch用の署名鍵（keystore）を作成済み: `feedwatch-release.jks`
- [ ] `local.properties` に署名情報を設定済み

### ドキュメントとアセット

- [x] プライバシーポリシー作成済み（`PRIVACY_POLICY_FEEDWATCH.md`）
- [x] ストアリスティング情報作成済み（`PLAY_STORE_LISTING_FEEDWATCH.md`）
- [x] グラフィックアセット準備済み
  - [x] ハイレゾアイコン（512x512）: `FeedWatch_HiresoIcon.png`
  - [x] フィーチャーグラフィック（1024x500）: `FeedWatch_FeatureGraph.png`
  - [x] TVバナー（320x180）: `FeedWatch_TvBanner.png`
  - [x] スクリーンショット（2枚）: `FeedWatch_Screenshot.png`, `FeedWatch_Screenshot2.png`

### コード設定

- [x] Product Flavors設定済み（asachiru / feedwatch）
- [x] AdMob App IDをflavor毎に分離設定済み
- [ ] **重要**: FeedWatch用のAdMob App IDを取得して設定（後述）

---

## 🚀 リリース手順（ステップバイステップ）

### ステップ1: AdMobの設定 【最優先】

**重要**: FeedWatchは別アプリなので、Asachiruとは**別のAdMob App ID**が必要です。

#### 1.1 AdMobで新しいアプリを登録

詳細な手順は [`ADMOB_SETUP_GUIDE_FEEDWATCH.md`](ADMOB_SETUP_GUIDE_FEEDWATCH.md) を参照してください。

**手順概要**:
1. https://admob.google.com/ にアクセス
2. 「アプリを追加」をクリック
3. アプリ情報を入力:
   - アプリ名: `RSS FeedWatch`
   - プラットフォーム: Android
   - パッケージ名: `com.tinygc.feedwatch`
4. **AdMob App ID**を取得（例: `ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY`）
5. バナー広告ユニットを作成して**広告ユニットID**を取得

#### 1.2 取得したAdMob App IDを設定

`app/build.gradle` の feedwatch flavor を編集:

```gradle
feedwatch {
    dimension "app"
    applicationId "com.tinygc.feedwatch"
    versionCode 1
    versionName "1.0"
    resValue "string", "app_name", "RSS FeedWatch"
    manifestPlaceholders = [
        admobAppId: "ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY"  // ← 取得したIDに置き換え
    ]
}
```

**確認**: `app/src/main/AndroidManifest.xml` で `${admobAppId}` を使用していることを確認済み✅

---

### ステップ2: リリースビルドの作成

詳細な手順は [`RELEASE_BUILD_GUIDE_FEEDWATCH.md`](RELEASE_BUILD_GUIDE_FEEDWATCH.md) を参照してください。

#### 2.1 local.properties の設定確認

プロジェクトルートの `local.properties` に以下が設定されていることを確認:

```properties
RELEASE_STORE_FILE=feedwatch-release.jks
RELEASE_STORE_PASSWORD=<キーストアのパスワード>
RELEASE_KEY_ALIAS=feedwatch
RELEASE_KEY_PASSWORD=<キーのパスワード>
```

#### 2.2 AAB（Android App Bundle）のビルド

Windows PowerShell で実行:

```powershell
# プロジェクトをクリーン
.\gradlew.bat clean

# Java環境変数を設定
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"

# FeedWatch用のリリースAABをビルド
.\gradlew.bat :app:bundleFeedwatchRelease
```

#### 2.3 ビルド成果物の確認

以下の場所にAABファイルが生成されます:

```
app/build/outputs/bundle/feedwatchRelease/app-feedwatch-release.aab
```

ファイルサイズと署名を確認:

```powershell
# ファイルの存在確認
Test-Path app\build\outputs\bundle\feedwatchRelease\app-feedwatch-release.aab

# 署名の確認
& "C:\Program Files\Android\Android Studio\jbr\bin\jarsigner.exe" -verify -verbose app\build\outputs\bundle\feedwatchRelease\app-feedwatch-release.aab
```

**出力**: `jar verified.` と表示されればOK ✅

---

### ステップ3: Google Play Console での登録

詳細な手順は [`GOOGLE_PLAY_CONSOLE_GUIDE_FEEDWATCH.md`](GOOGLE_PLAY_CONSOLE_GUIDE_FEEDWATCH.md) を参照してください。

#### 3.1 新しいアプリを作成

1. https://play.google.com/console にアクセス
2. 「すべてのアプリ」→「アプリを作成」
3. 基本情報を入力:
   ```
   アプリ名: RSS FeedWatch
   デフォルトの言語: 日本語（日本）
   アプリまたはゲーム: アプリ
   無料または有料: 無料
   ```

#### 3.2 アプリのセットアップ（ポリシー関連）

以下のセクションを順番に完了:

1. **アプリのアクセス権**: すべての機能を制限なく利用できる
2. **広告**: はい、広告が含まれています（AdMob使用）
3. **コンテンツレーティング**: アンケートに回答（全ユーザー対象）
4. **対象ユーザーと配信地域**: 13歳以上、日本
5. **ニュースアプリ**: はい（RSSリーダーとして動作）
6. **プライバシーポリシー**:
   ```
   https://github.com/tinygc/asachiru/blob/master/PRIVACY_POLICY_FEEDWATCH.md
   ```
7. **データの安全性**: 広告識別子（AAID）を収集・共有

#### 3.3 ストアの設定

**メインのストア掲載情報**に以下を設定:

| 項目 | 内容 | ファイル |
|-----|------|---------|
| アプリ名 | RSS FeedWatch | - |
| 簡単な説明 | お気に入りのRSSフィードをAndroid TVで快適にチェック。音声読み上げ対応。 | - |
| 詳細な説明 | （`PLAY_STORE_LISTING_FEEDWATCH.md` の内容をコピー） | `PLAY_STORE_LISTING_FEEDWATCH.md` |
| アプリアイコン | 512x512 PNG | `image/FeedWatch_HiresoIcon.png` |
| フィーチャーグラフィック | 1024x500 PNG | `image/FeedWatch_FeatureGraph.png` |
| TVバナー | 320x180 PNG | `image/FeedWatch_TvBanner.png` |
| スクリーンショット | 最低2枚 | `image/FeedWatch_Screenshot.png`<br>`image/FeedWatch_Screenshot2.png` |
| カテゴリ | ニュース＆雑誌 | - |
| メールアドレス | tinygc404@gmail.com | - |
| ウェブサイト | https://github.com/tinygc/asachiru | - |

#### 3.4 リリースの作成

**内部テスト（推奨）**:
1. 「テストとリリース」→「内部テスト」→「新しいリリースを作成」
2. `app-feedwatch-release.aab` をアップロード
3. リリースノートを入力（日本語）
4. テスターを追加（自分のメールアドレス等）
5. 公開

**製品版**:
1. 内部テスト完了後、「製品版」へ昇格
2. または直接製品版にAABをアップロード
3. 段階的公開を選択（推奨: 10% → 50% → 100%）
4. 「製品版に公開」をクリック

---

## 📊 審査と公開

### 審査プロセス

- **審査期間**: 数時間〜数日
- **審査状況**: Play Consoleのダッシュボードで確認可能
- **審査ステータス**: 審査中 → 審査完了 → 公開

### 公開後

アプリが承認されると、以下のURLで公開されます:

```
https://play.google.com/store/apps/details?id=com.tinygc.feedwatch
```

---

## 📝 重要な注意事項

### 1. AdMob App IDについて

- ⚠️ **Asachiruとは別のAdMob App IDが必須**
- 設定場所: `app/build.gradle` の feedwatch flavor
- 設定方法: [`ADMOB_SETUP_GUIDE_FEEDWATCH.md`](ADMOB_SETUP_GUIDE_FEEDWATCH.md) を参照

### 2. 署名鍵（Keystore）について

- ⚠️ **Asachiruとは別の署名鍵を使用**
- ファイル: `feedwatch-release.jks`
- **紛失厳禁**: バックアップを安全な場所に保管
- 紛失すると今後のアップデートができなくなります

### 3. バージョン管理

今後のアップデート時は、以下を更新:

```gradle
feedwatch {
    versionCode 2     // ← 毎回インクリメント（必須）
    versionName "1.1" // ← バージョン番号（ユーザー向け）
}
```

### 4. プライバシーポリシーについて

- GitHubで公開する場合、**必ずpublicリポジトリ**にすること
- または独自ドメインでホスティング
- Play Consoleから直接アクセスできる必要があります

---

## 🔧 トラブルシューティング

### ビルドエラー

```powershell
# Gradleキャッシュのクリア
.\gradlew.bat clean

# Java環境変数を確認
echo $env:JAVA_HOME
```

### 審査で却下された

1. 却下理由をPlay Consoleで確認
2. 問題を修正
3. 新しいリリースを作成して再提出

### 広告が表示されない

1. AdMob App IDが正しく設定されているか確認
2. AdMobの審査完了を待つ（数時間〜1日）
3. Logcatでエラーを確認

---

## 📚 関連ドキュメント

| ドキュメント | 内容 |
|------------|------|
| [`PRIVACY_POLICY_FEEDWATCH.md`](PRIVACY_POLICY_FEEDWATCH.md) | プライバシーポリシー |
| [`PLAY_STORE_LISTING_FEEDWATCH.md`](PLAY_STORE_LISTING_FEEDWATCH.md) | ストアリスティング情報 |
| [`ADMOB_SETUP_GUIDE_FEEDWATCH.md`](ADMOB_SETUP_GUIDE_FEEDWATCH.md) | AdMob設定ガイド |
| [`RELEASE_BUILD_GUIDE_FEEDWATCH.md`](RELEASE_BUILD_GUIDE_FEEDWATCH.md) | リリースビルドガイド |
| [`GOOGLE_PLAY_CONSOLE_GUIDE_FEEDWATCH.md`](GOOGLE_PLAY_CONSOLE_GUIDE_FEEDWATCH.md) | Play Console登録ガイド |

---

## 📞 サポート

質問や問題がある場合:

- **Email**: tinygc404@gmail.com
- **GitHub**: https://github.com/tinygc/asachiru

---

## ✨ まとめ

Play Storeへのリリース手順は以下の通りです：

1. ✅ **AdMob登録**: FeedWatch用の新しいアプリを登録してApp IDを取得
2. ✅ **App ID設定**: `app/build.gradle` に取得したIDを設定
3. ✅ **AABビルド**: リリース用のAABファイルを作成
4. ✅ **Play Console**: アプリ登録、ポリシー設定、ストアリスティング設定
5. ✅ **リリース**: AABをアップロードして公開

すべての手順を完了したら、審査結果を待ちましょう！

**がんばってください！🎉**

---

**作成日**: 2025-11-25
**最終更新**: 2025-11-25
