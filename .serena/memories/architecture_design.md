# アーキテクチャ設計（Clean Architecture）

## レイヤー構成
```
Presentation Layer (UI)
    ↓ (依存)
Domain Layer (UseCase)
    ↓ (依存)
Data Layer (Repository)
```

## 各レイヤーの責務

### Presentation Layer
- **責務**: UI表示、ユーザー入力受付、ViewModelからデータ反映
- **主要コンポーネント**:
  - `MainActivity`: メイン画面
  - `SetupActivity`: 初回設定画面
  - `MainViewModel`: メイン画面の状態管理
  - `SetupViewModel`: 設定画面の状態管理
  - Custom Views: 時計、天気、ビジュアライザー等
- **禁止事項**: ビジネスロジック実装、直接的なデータアクセス

### Domain Layer
- **責務**: ビジネスロジック実装、ユースケース定義、ドメインモデル定義
- **主要コンポーネント**:
  - UseCases: 各機能のビジネスロジック
  - Entities: ドメインモデル
  - Repository Interfaces: データアクセスの抽象化
- **特徴**: Android SDKへの依存なし（Pure Kotlin）

### Data Layer
- **責務**: データアクセス実装、外部API通信、ローカルストレージ
- **主要コンポーネント**:
  - Repository Implementation
  - DataSource (API, Local)
  - DTO (Data Transfer Object)

## 依存性逆転の原則
- 依存性は常に外側から内側へ
- Domain層はフレームワーク非依存
- InterfaceによるData層の抽象化

## Product Flavors設計
- **共通実装**: `app/src/main/` に配置
- **Flavor固有**: `app/src/asachiru/` と `app/src/feedwatch/` に配置
- ビルド時に自動マージされる
