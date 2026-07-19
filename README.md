# LShiftFix

> **Minecraft 1.8.9 Forge 输入修复模组** — 解决"按住 Shift（潜行）时无法跳跃"的顽疾。

[![Modrinth](https://img.shields.io/badge/Modrinth-LShiftFix-1bd964?style=flat-square&logo=modrinth)](https://modrinth.com/project/lshiftfix)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.8.9-6c9f3f?style=flat-square)](https://modrinth.com/project/lshiftfix)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](./LICENSE)

## 问题描述

在中文/日文/韩文输入法激活的 Minecraft 1.8.9 客户端中，玩家**按住 Shift（潜行）的同时按下 Space（跳跃）** 时，跳跃指令会被系统输入法吞掉，导致玩家只能蹲不能跳。

**根本原因**：几乎所有主流 IME 都把 `Shift+Space` 注册为全角/半角切换热键。Windows 在派发 `WM_KEYDOWN` 给应用之前，会先经过 `ImmTranslateMessage`，IME 在此环节消费掉该组合键，于是 Minecraft 的 `Keyboard.next()` 事件队列里根本不会出现这次按键 —— `keyBindJump.isKeyDown()` 永远为 `false`，`movementInput.jump` 永远不会被设为 `true`。

## 修复原理

LShiftFix 在 `EntityPlayerSP.updateMovementInput()`（SRG `func_175161_p`）的尾部注入一段逻辑：

1. 读取玩家绑定的跳跃键（默认 Space，支持重绑定）
2. 调用 `Keyboard.isKeyDown(keyCode)` **直接读取硬件按键状态**
3. 若物理状态为按下但 `movementInput.jump` 仍为 `false`，强制设为 `true`

LWJGL 的 `keyDownBuffer` 由比 `WM_KEYDOWN` 更低层的窗口钩子维护，IME 拦截事件队列时它依然如实记录物理按下 —— 这就是绕过 IME 的关键。该方案**与具体输入法无关**，对搜狗、微软拼音、QQ 拼音、Google 输入法、日文 IME、韩文 IME 等均生效。

## 安装

### 前置依赖

| 依赖 | 说明 |
|---|---|
| **Minecraft 1.8.9** | 仅此版本 |
| **Forge 1.8.9** (`11.15.1.x`) | 推荐 `11.15.1.2318` |
| **[MixinBooter](https://modrinth.com/mod/mixinbooter)** | 提供 Mixin 运行环境，必须先于本模组加载 |

### 安装步骤

1. 下载并安装 Forge 1.8.9（如已安装可跳过）
2. 下载 [MixinBooter](https://modrinth.com/mod/mixinbooter/versions?g=1.8.9)（选择 1.8.9 兼容版本）
3. 下载 LShiftFix 最新版
4. 将两个 `.jar` 放入 `.minecraft/mods/`
5. 启动游戏

## 配置

首次启动后会在 `.minecraft/config/lshiftfix.cfg` 生成：

```cfg
general {
    # 设为 true 以记录每次"被 IME 吞掉的按键"事件，用于验证修复是否生效。默认: false
    B:enableDebugLog=false

    # 设为 true 以轮询所有移动按键（前进/后退/左/右/潜行）。当 IME 拦截其他移动键时启用。默认: false
    B:enableAllKeyPolling=false

    # 设为 true 以启用 GUI IME 守卫（聊天框中按 Shift 切换中英文时不会泄漏到潜行状态）。默认: true
    B:enableGuiImeGuard=true

    # 调试日志输出的冷却时间（tick），防止刷屏。默认: 20（约1秒）
    I:debugLogCooldownTicks=20
}
```

## 游戏内命令（热重载）

无需重启游戏即可动态切换配置，OP 权限等级 2 即可使用：

| 命令 | 作用 |
|---|---|
| `/lshiftfix` 或 `/lshiftfix status` | 查看当前所有配置状态 |
| `/lshiftfix reload` | 从磁盘重新读取配置文件 |
| `/lshiftfix debug on\|off` | 开关调试日志 |
| `/lshiftfix polling on\|off` | 开关全按键轮询模式 |
| `/lshiftfix guard on\|off` | 开关 GUI IME 守卫 |

## 高级功能

### 全按键轮询模式 (`enableAllKeyPolling`)

部分 IME 可能会拦截其他移动按键（如 `W`/`A`/`S`/`D`），导致玩家无法正常移动。启用此选项后，模组会轮询所有移动按键：

- `W` / 前进
- `S` / 后退
- `A` / 左移
- `D` / 右移
- `Space` / 跳跃
- `Shift` / 潜行

**多键组合保护**：当检测到 Shift + Space 同时物理按下时，模组会同时保证 sneak 和 jump 都为 true，避免 IME 吞掉其中一个。

### GUI IME 守卫 (`enableGuiImeGuard`)

很多 IME 使用 Shift 作为中英文切换键。在聊天框中按 Shift 切换语言时，vanilla 的 KeyBinding 会记录 Shift 按下，导致关闭聊天框后玩家会突然潜行一拍。本守卫会在 GuiChat 打开期间持续清除 Shift 的绑定状态，并在关闭时再次清除，确保不会有残留。

### 热键冲突检测

模组在 postInit 阶段会扫描所有已注册的 KeyBinding，如果发现任何绑定到 SPACE 的按键，会在日志中打印警告（因为 SPACE 经常与 IME 的 `Shift+Space` / `Ctrl+Space` 冲突）。检查 `.minecraft/logs/latest.log` 中的 `[LShiftFix] Hotkey conflict potential` 行即可。

### 性能优化

- **GameSettings 缓存**：在 init 阶段缓存 GameSettings 引用，避免每 tick 调用 `Minecraft.getMinecraft().gameSettings`
- **KeyBinding keycode 缓存**：每 tick 刷新一次 keycode，避免重复调用 `getKeyCode()`
- **早退机制**：当 movementInput / Minecraft / Keyboard 任意一个未就绪时立即返回，避免无谓的 `isKeyDown()` 调用

## 调试日志

开启 `enableDebugLog=true` 后，每次修复触发会在日志中输出：

```
[LShiftFix] IME swallowed 'jump' key event — physical key 57 polled as pressed, forcing state=true
```

> 57 = `Keyboard.KEY_SPACE`。若你重绑定了跳跃键，此数字会变化。

## 输入法侧修复指南（推荐配合使用）

虽然 LShiftFix 已能修复跳跃，但建议同时关闭 IME 的 `Shift+Space` 热键，避免在其他场景触发意外切换：

### 微软拼音（Windows 10/11 自带）

1. 设置 → 时间和语言 → 语言 → 中文（简体）→ 选项 → 微软拼音 → 选项
2. 高级 → 取消勾选 "全角/半角切换：Shift+Space"

### 搜狗拼音

1. 搜狗输入法设置 → 高级 → 系统功能快捷键
2. 找到 "全角/半角切换（Shift+Space）" → 删除或修改

### QQ 拼音

1. 属性设置 → 按键 → 快捷键设置
2. 取消 "全角/半角切换" 的 `Shift+Space` 绑定

### Google 输入法

1. 偏好设置 → 按键
2. 关闭 "全角/半角切换" 的 `Shift+Space` 项

### 日文 IME（MS-IME）

1. 设置 → 全般 → 詳細設定 → キー設定
2. 編集 → 找到 "全角/半角" 对应的 `Shift+Space` → 删除

### 韩文 IME（Microsoft IME）

1. 설정 → 고급 → 키 할당
2. `Shift+Space` 한영 전환 → 해제

## 兼容性

- **客户端专用**：仅作用于 `EntityPlayerSP` 和 `GuiChat`，无需服务端安装
- **GUI 安全**：在聊天框、书与笔、告示牌等界面会自动屏蔽修复逻辑，打字时不会误触跳跃
- **重绑定兼容**：实时读取 `gameSettings.keyBindJump`，尊重用户的按键设置
- **鼠标绑定保护**：若跳跃被绑定到鼠标按键，本模组会自动跳过（避免越界读取）
- **不修改原版逻辑**：仅在 `RETURN` 注入，不替换原方法，与其他 Mixin 模组可共存
- **安全防护**：包含多项健壮性检查（Minecraft 实例非空、Keyboard 已初始化、配置已加载）

## 构建源码

```bash
# 1. 生成 Gradle wrapper（如未包含）
gradle wrapper --gradle-version 2.14.1

# 2. 构建生产 jar（输出到 build/libs/LShiftFix-1.0.0.jar）
./gradlew build
```

> 构建需要 JDK 8（推荐 Adoptium Temurin 8）。

## 许可证

[MIT License](./LICENSE) © 2026 ninefyu
