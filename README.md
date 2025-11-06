# 朝チル (AsaChil)

## 概要

朝の時間につけっぱなしにする用途で、時計・天気・ニュース読み上げをチルい音楽とともに提供するAndroid TVアプリケーションです。

パステルカラーでカラフルなピクセルアート風デザインで、リラックスした朝の時間を演出します。

## 主要機能

### 時計機能
- デジタル時計（24時間表示）
- 日付・曜日表示（曜日の色分け対応）

### 天気機能
- 天気アイコン表示
- 現在気温、最高気温、最低気温、降水確率の表示
- 郵便番号による地域設定

### ニュース読み上げ機能
- NHKニュースRSSから最新10件を自動読み上げ
- Android標準TTS（Text-to-Speech）を使用
- 読み上げ間隔を1〜60分で設定可能

### 音楽再生機能
- Lo-Fiジャンルのフリー音源をループ再生
- 3秒間のクロスフェード対応

### ビジュアライザー機能
- スペクトラムアナライザー（縦のバータイプ）
- パステルレインボーカラー（8色）
- 音楽に反応してリアルタイムに表示

## 技術スタック

- **プラットフォーム**: Android TV
- **開発環境**: Windows 11 PowerShell
- **アーキテクチャ**: Clean Architecture
- **開発手法**: TDD（テスト駆動開発、t-wada方式）
- **プロセス**: V字開発モデル、Issue Driven開発

## 外部API

- 天気予報API（livedoor天気互換）
- NHKニュースRSS

## プロジェクト状況

### 現在のフェーズ: 設計工程完了 ✅

- ✅ 要件定義完了
- ✅ アーキテクチャ設計完了
- ✅ 各モジュール設計完了
- ✅ 設計レビュー完了
- ⏭️ 次: 実装工程

## ディレクトリ構成

```
asachiru/
├── requirement/              # 要件定義ドキュメント
│   ├── BaseRequirements.md  # 基本要件定義書
│   ├── ui_mockup.html        # UIモックアップ
│   └── ui_mockup_pastel.html # パステル版UIモックアップ
├── design/                   # 設計ドキュメント
│   ├── Architecture.md       # アーキテクチャ設計書
│   ├── LayerDefinition.md    # レイヤー構成詳細定義書
│   ├── Module_Clock.md       # 時計機能モジュール設計書
│   ├── Module_Weather.md     # 天気機能モジュール設計書
│   ├── Module_News.md        # ニュース機能モジュール設計書
│   ├── Module_MusicAndVisualizer.md # 音楽・ビジュアライザー機能設計書
│   ├── Module_Setup.md       # 初回設定機能設計書
│   ├── DataFlow.md           # データフロー設計書
│   └── DESIGN_REVIEW.md      # 設計レビュー報告書
├── test/                     # テストドキュメント・スクリーンキャプチャ
└── README.md                 # 本ファイル
```

## 開発者

- GitHub: [@tinygc](https://github.com/tinygc)
- Email: tinygc404@gmail.com

## ライセンス

TBD

---

**文書作成日**: 2025-11-06
