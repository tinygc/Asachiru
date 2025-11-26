# RSS FeedWatch - リリースビルドガイド

## 概要

このドキュメントでは、RSS FeedWatchのリリースビルド（AAB形式）を作成する手順を説明します。

## 前提条件

### 1. 署名鍵（Keystore）の確認

プロジェクトルートに `feedwatch-release.jks` が存在することを確認してください。

```bash
# ファイルの存在確認
ls feedwatch-release.jks
```

### 2. local.properties の設定

プロジェクトルートの `local.properties` ファイルに以下の署名情報を設定してください：

```properties
RELEASE_STORE_FILE=feedwatch-release.jks
RELEASE_STORE_PASSWORD=<キーストアのパスワード>
RELEASE_KEY_ALIAS=feedwatch
RELEASE_KEY_PASSWORD=<キーのパスワード>
```

**注意**: `local.properties` は `.gitignore` に含まれており、Gitにコミットされません。

### 3. ビルド環境の確認

```bash
# Java バージョン確認（JDK 17が必要）
java -version

# Android SDK の確認
echo $ANDROID_HOME
# または Windows PowerShell の場合
echo $env:ANDROID_HOME
```

## リリースビルドの手順

### ステップ1: プロジェクトのクリーン

古いビルド成果物を削除します。

```bash
# Windows PowerShell
.\gradlew.bat clean

# Linux/Mac
./gradlew clean
```

### ステップ2: AAB（Android App Bundle）のビルド

FeedWatch flavor のリリース用 AAB をビルドします。

```bash
# Windows PowerShell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:bundleFeedwatchRelease

# Linux/Mac
export JAVA_HOME="/path/to/jdk17"
./gradlew :app:bundleFeedwatchRelease
```

### ステップ3: ビルド成果物の確認

ビルドが成功すると、以下の場所に AAB ファイルが生成されます：

```
app/build/outputs/bundle/feedwatchRelease/app-feedwatch-release.aab
```

ファイルの存在確認：

```bash
# Windows PowerShell
Test-Path app\build\outputs\bundle\feedwatchRelease\app-feedwatch-release.aab

# Linux/Mac
ls -lh app/build/outputs/bundle/feedwatchRelease/app-feedwatch-release.aab
```

### ステップ4: AABファイルの検証

AABファイルの署名を確認します。

```bash
# Windows PowerShell
& "C:\Program Files\Android\Android Studio\jbr\bin\jarsigner.exe" -verify -verbose -certs app\build\outputs\bundle\feedwatchRelease\app-feedwatch-release.aab

# Linux/Mac
jarsigner -verify -verbose -certs app/build/outputs/bundle/feedwatchRelease/app-feedwatch-release.aab
```

正常に署名されている場合、以下のようなメッセージが表示されます：

```
jar verified.
```

### ステップ5: バージョン情報の確認

AABファイルに含まれるバージョン情報を確認します。

```bash
# bundletool を使用（事前にダウンロードが必要）
# https://github.com/google/bundletool/releases

java -jar bundletool.jar dump manifest --bundle=app/build/outputs/bundle/feedwatchRelease/app-feedwatch-release.aab | grep version
```

以下の情報が表示されることを確認：
- `versionCode="1"`
- `versionName="1.0"`

## APKの生成（テスト用）

AABから直接APKを生成してテストすることもできます。

```bash
# Universal APKの生成
java -jar bundletool.jar build-apks \
  --bundle=app/build/outputs/bundle/feedwatchRelease/app-feedwatch-release.aab \
  --output=feedwatch-release.apks \
  --mode=universal \
  --ks=feedwatch-release.jks \
  --ks-key-alias=feedwatch

# APKSファイルからAPKを抽出
unzip feedwatch-release.apks -d feedwatch-apks
```

## トラブルシューティング

### エラー1: KeyStore not found

**原因**: `feedwatch-release.jks` が見つからない、または `local.properties` のパスが間違っている

**解決策**:
1. keystoreファイルがプロジェクトルートに存在することを確認
2. `local.properties` の `RELEASE_STORE_FILE` パスを確認

### エラー2: Incorrect password

**原因**: keystoreまたはキーのパスワードが間違っている

**解決策**:
1. `local.properties` のパスワードを確認
2. keystoreとキーのパスワードが異なる場合、両方を正しく設定

### エラー3: Java version mismatch

**原因**: JDK バージョンが 17 ではない

**解決策**:
```bash
# Windows PowerShell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"

# Linux/Mac
export JAVA_HOME="/path/to/jdk17"
```

### エラー4: Build failed with ProGuard

**原因**: コード難読化（minifyEnabled）で問題が発生

**解決策**:
1. `app/build.gradle.kts` の ProGuard ルールを確認
2. 必要に応じて `proguard-rules.pro` にキープルールを追加

## ビルド成果物のチェックリスト

リリース前に以下を確認してください：

- [ ] AABファイルが正常に生成された
- [ ] AABファイルが正しく署名されている（jarsignerで確認）
- [ ] versionCode と versionName が正しい
- [ ] Application ID が `com.tinygc.feedwatch` である
- [ ] アプリ名が「RSS FeedWatch」である
- [ ] ファイルサイズが妥当（通常 5-20MB 程度）
- [ ] テスト用APKで実機動作確認済み

## 次のステップ

AABファイルが正常に作成できたら、次は Google Play Console での登録作業に進みます。

詳細は `GOOGLE_PLAY_CONSOLE_GUIDE_FEEDWATCH.md` を参照してください。

---

**作成日**: 2025-11-25
**最終更新**: 2025-11-25
