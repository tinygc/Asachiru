# 実装タスク一覧

このディレクトリには、実装工程のGitHub Issueを管理用にマークダウンファイルとして保存しています。

## Issue一覧

### セットアップ（#1-3）
- [Issue-001.md](./Issue-001.md) - Android TVプロジェクトのセットアップ
- [Issue-002.md](./Issue-002.md) - Clean Architectureのディレクトリ構造作成
- [Issue-003.md](./Issue-003.md) - 共通クラス実装（Result型、AppException等）

### Domain Layer（#4-13）
- [Issue-004.md](./Issue-004.md) - Entity実装（DateTime, DayOfWeek）
- [Issue-005.md](./Issue-005.md) - Entity実装（Weather, WeatherCondition）
- [Issue-006.md](./Issue-006.md) - Entity実装（News）
- [Issue-007.md](./Issue-007.md) - Entity実装（Music, Settings）
- [Issue-008.md](./Issue-008.md) - Repository Interface実装
- [Issue-009.md](./Issue-009.md) - UseCase実装（時計機能）
- [Issue-010.md](./Issue-010.md) - UseCase実装（天気機能）
- [Issue-011.md](./Issue-011.md) - UseCase実装（ニュース機能）
- [Issue-012.md](./Issue-012.md) - UseCase実装（音楽機能）
- [Issue-013.md](./Issue-013.md) - UseCase実装（設定機能）

### Data Layer（#14-18）
- [Issue-014.md](./Issue-014.md) - 天気API DataSource実装
- [Issue-015.md](./Issue-015.md) - ニュースRSS DataSource実装
- [Issue-016.md](./Issue-016.md) - Settings LocalDataSource実装
- [Issue-017.md](./Issue-017.md) - Music LocalDataSource実装
- [Issue-018.md](./Issue-018.md) - Repository実装（全機能）

### Presentation Layer（#19-29）
- [Issue-019.md](./Issue-019.md) - TtsManager実装
- [Issue-020.md](./Issue-020.md) - MusicPlayer実装
- [Issue-021.md](./Issue-021.md) - Custom View実装（ClockView）
- [Issue-022.md](./Issue-022.md) - Custom View実装（WeatherView）
- [Issue-023.md](./Issue-023.md) - Custom View実装（NewsView）
- [Issue-024.md](./Issue-024.md) - Custom View実装（VisualizerView）
- [Issue-025.md](./Issue-025.md) - MainViewModel実装
- [Issue-026.md](./Issue-026.md) - MainActivity実装
- [Issue-027.md](./Issue-027.md) - SetupActivity/ViewModel実装
- [Issue-028.md](./Issue-028.md) - SplashActivity/ViewModel実装
- [Issue-029.md](./Issue-029.md) - ViewModelFactory実装

### テスト（#30-31）
- [Issue-030.md](./Issue-030.md) - 統合テスト実装
- [Issue-031.md](./Issue-031.md) - E2Eテスト実装

## GitHub Issues

実際のIssueはこちら:
https://github.com/tinygc/asachiru/issues

## ラベル

- `implementation`: 実装タスク
- `setup`: セットアップ
- `domain`: Domain Layer
- `data`: Data Layer
- `presentation`: Presentation Layer
- `test`: テスト

## 進め方

1. セットアップ (#1-3) から順に実装
2. Domain Layer (#4-13) を実装
3. Data Layer (#14-18) を実装
4. Presentation Layer (#19-29) を実装
5. テスト (#30-31) を実施

各Issueは依存関係を持つものがあるので、番号順に進めることを推奨します。
