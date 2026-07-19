# LShiftFix

> **Minecraft 1.8.9 Forge Input Fix Mod** — Resolves "cannot jump while sneaking with IME active".

[![Modrinth](https://img.shields.io/badge/Modrinth-LShiftFix-1bd964?style=flat-square&logo=modrinth)](https://modrinth.com/project/lshiftfix)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.8.9-6c9f3f?style=flat-square)](https://modrinth.com/project/lshiftfix)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](./LICENSE)

## Problem

When a Chinese / Japanese / Korean IME is active in Minecraft 1.8.9, the player **cannot jump while holding Shift (sneak) and pressing Space**. The jump input is swallowed by the OS input method.

**Root cause**: Almost every IME registers `Shift+Space` as a full-width/half-width toggle hotkey. Windows routes `WM_KEYDOWN` through `ImmTranslateMessage` before dispatching to the app. The IME consumes the combo there, so Minecraft's `Keyboard.next()` event queue never sees the key press — `keyBindJump.isKeyDown()` stays `false` forever.

## Fix

LShiftFix injects at the tail of `EntityPlayerSP.updateMovementInput()`:

1. Reads the player's bound jump key (default Space, supports rebinding)
2. Calls `Keyboard.isKeyDown(keyCode)` to **directly poll the hardware state**
3. If physically pressed but `movementInput.jump` is still `false`, force it to `true`

LWJGL's `keyDownBuffer` is populated by `WindowProc` at a layer lower than `WM_KEYDOWN`, so IME interception of the event queue doesn't affect it. The fix is **IME-agnostic** — works for Sogou, Microsoft Pinyin, QQ Pinyin, Google Input, Japanese IME, Korean IME, etc.

## Install

| Dependency | Required |
|---|---|
| Minecraft 1.8.9 | Yes |
| Forge `11.15.1.x` (recommend `.2318`) | Yes |
| [MixinBooter](https://modrinth.com/mod/mixinbooter) (1.8.9 build) | Yes |

Drop both `MixinBooter-*.jar` and `LShiftFix-1.0.0.jar` into `.minecraft/mods/`.

## Config

`.minecraft/config/lshiftfix.cfg`:

| Option | Default | Description |
|---|---|---|
| `enableDebugLog` | `false` | Log each IME-swallowed key event |
| `enableAllKeyPolling` | `false` | Poll all movement keys (WASD + sneak) |
| `enableGuiImeGuard` | `true` | Suppress Shift leak in chat GUI |
| `debugLogCooldownTicks` | `20` | Min ticks between debug log entries |

## In-game Commands

| Command | Effect |
|---|---|
| `/lshiftfix` or `/lshiftfix status` | Show current state |
| `/lshiftfix reload` | Reload config from disk |
| `/lshiftfix debug on\|off` | Toggle debug logging |
| `/lshiftfix polling on\|off` | Toggle all-key polling |
| `/lshiftfix guard on\|off` | Toggle GUI IME guard |

Requires OP level 2.

## Config GUI

Mods menu → LShiftFix → Config. Toggle options without editing the cfg file.

## IME Hotkey Disable (Recommended)

Even with LShiftFix, we recommend disabling the IME's `Shift+Space` hotkey to avoid accidental toggles in other contexts:

- **Microsoft Pinyin (Win10/11)**: Settings → Time & Language → Language → Chinese → Options → Microsoft Pinyin → Options → Advanced → uncheck "Full/Half-width: Shift+Space"
- **Sogou**: Settings → Advanced → System shortcuts → remove "Full/Half-width (Shift+Space)"
- **QQ Pinyin**: Properties → Keys → uncheck "Full/Half-width Shift+Space"
- **Google Input**: Preferences → Keys → disable "Shift+Space"
- **MS-IME (Japanese)**: 設定 → 詳細設定 → キー設定 → remove `Shift+Space` full/half-width
- **Microsoft IME (Korean)**: 설정 → 고급 → 키 할당 → disable `Shift+Space`

## Compatibility

- **Client-side only** (but Forge will load it on both sides — required for the command to work)
- **GUI-safe**: automatically disabled while chat / book / sign GUI is open
- **Rebinding-respecting**: reads `gameSettings.keyBindJump` at runtime
- **Mouse-binding-safe**: skips if jump is bound to a mouse button
- **OptiFine**: no conflict, logged at startup
- **PlayerAPI**: detected, warns the user
- **Lunar Client / Badlion**: detected, main polling auto-disabled (they already fix this)

## Build

```bash
# Requires JDK 8 (ForgeGradle 2.1 limitation)
gradle wrapper --gradle-version 2.14.1
./gradlew build
# Output: build/libs/LShiftFix-1.0.0.jar
```

For CI, GitHub Actions is preconfigured — see [`.github/workflows/build.yml`](./.github/workflows/build.yml).

## Documentation

- [中文 README](./README.md)
- [FAQ & Troubleshooting (中文)](./FAQ.md)
- [Architecture (English)](./ARCHITECTURE.md)
- [日本語 README](./README_JA.md)
- [한국어 README](./README_KO.md)

## License

[MIT](./LICENSE) © 2026 ninefyu
