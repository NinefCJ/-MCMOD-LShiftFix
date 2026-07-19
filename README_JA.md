# LShiftFix

> **Minecraft 1.8.9 Forge 入力修正 Mod** — IME 有効時の「スニーク中にジャンプできない」問題を解決。

[![Modrinth](https://img.shields.io/badge/Modrinth-LShiftFix-1bd964?style=flat-square&logo=modrinth)](https://modrinth.com/project/lshiftfix)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.8.9-6c9f3f?style=flat-square)](https://modrinth.com/project/lshiftfix)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](./LICENSE)

## 問題

日本語 IME を有効にした Minecraft 1.8.9 で、**Shift（スニーク）を押しながら Space（ジャンプ）を押してもジャンプできない**問題が発生します。ジャンプ入力が OS の IME に飲み込まれるためです。

**根本原因**: 日本語 IME は `Shift+Space` を全角/半角切り替えホットキーとして登録しています。Windows は `WM_KEYDOWN` をアプリに配信する前に `ImmTranslateMessage` を経由し、IME がここで組み合わせキーを消費します。その結果、Minecraft の `Keyboard.next()` イベントキューにキーイベントが届かず、`keyBindJump.isKeyDown()` が永遠に `false` のままになります。

## 修正原理

LShiftFix は `EntityPlayerSP.updateMovementInput()` の末尾に注入します：

1. プレイヤーのジャンプキー（デフォルト Space、再バインド対応）を読み取る
2. `Keyboard.isKeyDown(keyCode)` で**ハードウェア状態を直接ポーリング**
3. 物理的に押されているのに `movementInput.jump` が `false` なら `true` に強制設定

LWJGL の `keyDownBuffer` は `WM_KEYDOWN` より低いレイヤーの `WindowProc` で更新されるため、IME のイベントキュー傍受に影響されません。この修正は**IME 非依存**で、MS-IME、Google 日本語入力、ATOK、Sogou、Microsoft Pinyin など全てで動作します。

## インストール

| 依存 | 必須 |
|---|---|
| Minecraft 1.8.9 | はい |
| Forge `11.15.1.x`（`.2318` 推奨） | はい |
| [MixinBooter](https://modrinth.com/mod/mixinbooter)（1.8.9 版） | はい |

`MixinBooter-*.jar` と `LShiftFix-1.0.0.jar` を共に `.minecraft/mods/` に入れてください。

## 設定

`.minecraft/config/lshiftfix.cfg`:

| オプション | デフォルト | 説明 |
|---|---|---|
| `enableDebugLog` | `false` | IME 飲み込みイベントをログ出力 |
| `enableAllKeyPolling` | `false` | 全移動キー（WASD + スニーク）をポーリング |
| `enableGuiImeGuard` | `true` | チャット GUI 内の Shift 漏れを抑制 |
| `debugLogCooldownTicks` | `20` | デバッグログの最小間隔（tick） |

## ゲーム内コマンド

| コマンド | 効果 |
|---|---|
| `/lshiftfix` または `/lshiftfix status` | 現在の状態を表示 |
| `/lshiftfix reload` | 設定を再読み込み |
| `/lshiftfix debug on\|off` | デバッグログ切替 |
| `/lshiftfix polling on\|off` | 全キーポーリング切替 |
| `/lshiftfix guard on\|off` | GUI IME ガード切替 |

OP レベル 2 が必要です。

## 設定 GUI

Mods メニュー → LShiftFix → Config で、cfg ファイルを編集せずに切り替え可能。

## IME ホットキー無効化（推奨）

LShiftFix でジャンプは修正されますが、他の場面での誤作動を防ぐため IME の `Shift+Space` ホットキー無効化を推奨します：

- **MS-IME（Windows 標準）**: 設定 → プロパティ → 全般 → 詳細設定 → キー設定 → 「全角/半角」の `Shift+Space` を削除
- **Google 日本語入力**: プロパティ → キー設定 → 「全角/半角切り替え」の `Shift+Space` を無効化
- **ATOK**: 環境設定 → キー・ローマ字・色 → キーカスタマイズ → 「全角/半角」を変更

## 互換性

- **クライアント専用**（コマンド登録のため両サイドでロードされます）
- **GUI セーフ**: チャット / 本 / 看板 GUI 中は自動的に無効化
- **再バインド対応**: `gameSettings.keyBindJump` を実行時に読み取り
- **マウスバインド保護**: ジャンプがマウスボタンにバインドされている場合はスキップ
- **OptiFine**: コンフリクトなし、起動時にログ出力
- **PlayerAPI**: 検出して警告
- **Lunar Client / Badlion**: 検出してメインポーリングを自動無効化（これらは既に修正済みのため）

## ビルド

```bash
# JDK 8 必須（ForgeGradle 2.1 の制限）
gradle wrapper --gradle-version 2.14.1
./gradlew build
# 出力: build/libs/LShiftFix-1.0.0.jar
```

CI は GitHub Actions に設定済み — [`.github/workflows/build.yml`](./.github/workflows/build.yml) を参照。

## ドキュメント

- [中文 README](./README.md)
- [English README](./README_EN.md)
- [FAQ（中文）](./FAQ.md)
- [Architecture（English）](./ARCHITECTURE.md)
- [한국어 README](./README_KO.md)

## ライセンス

[MIT](./LICENSE) © 2026 ninefyu
