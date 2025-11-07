# Issue #24: Custom View実装（VisualizerView）

**Labels:** implementation, presentation

## 概要
スペクトラムアナライザーを表示するカスタムビューを実装する。

## タスク
- [ ] VisualizerView実装
- [ ] Visualizer APIを使った波形取得
- [ ] 50本のバー描画
- [ ] パステルレインボーカラー実装
- [ ] パフォーマンス最適化（フレームレート制限）
- [ ] UIテスト作成

## 受け入れ条件
- 音楽に反応してバーが動くこと
- 50本のバーがパステルカラーで表示されること
- フレームレートが30fps以下に制限されていること
- リソースリークがないこと
- UIテストが全てパスすること

## 参考
- design/Module_MusicAndVisualizer.md
- design/DESIGN_REVIEW.md（パフォーマンス最適化）
