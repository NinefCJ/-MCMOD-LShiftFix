# Architecture

> LShiftFix 内部架构与技术决策记录。面向开发者 / 贡献者 / 二次修改者。

## 1. 顶层设计

```
┌──────────────────────────────────────────────────────────────┐
│                  Minecraft Main Thread (1.8.9)               │
│                                                              │
│   ┌──────────────────────────────────────────────┐           │
│   │  EntityPlayerSP.updateMovementInput()  ←──── Mixin @RETURN│
│   │     ↓ (original vanilla logic)               │           │
│   │     movementInput.jump = keyBindJump.isKeyDown│          │
│   │     ↓ (our injected callback)                │           │
│   │  ┌────────────────────────────────────────┐  │           │
│   │  │ MixinEntityPlayerSP                    │  │           │
│   │  │  1. Guard checks (init, GUI, Keyboard) │  │           │
│   │  │  2. Snapshot MovementInput → State     │  │           │
│   │  │  3. Read physical keys via LWJGL       │  │           │
│   │  │  4. KeyPollingLogic.apply(state, keys) │  │           │
│   │  │  5. Write back changed fields          │  │           │
│   │  └────────────────────────────────────────┘  │           │
│   └──────────────────────────────────────────────┘           │
│                                                              │
│   ┌──────────────────────────────────────────────┐           │
│   │  GuiScreen.updateScreen() / onGuiClosed()    │           │
│   │     ↓ Mixin @RETURN                          │           │
│   │  ┌────────────────────────────────────────┐  │           │
│   │  │ MixinGuiScreen                         │  │           │
│   │  │  - If instanceof GuiChat               │  │           │
│   │  │  - If sneak KeyBinding physically held │  │           │
│   │  │  - Force setKeyBindState(sneakCode, F) │  │           │
│   │  └────────────────────────────────────────┘  │           │
│   └──────────────────────────────────────────────┘           │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│                Pure Logic (unit-tested)                      │
│                                                              │
│  MovementInputState ←——→ KeyPollingLogic.apply()            │
│  (plain POJO, no MC)    (state machine, no MC)               │
│         ↑                          ↑                         │
│         └── snapshot/copy from MovementInput (in Mixin) ────┘│
└──────────────────────────────────────────────────────────────┘
```

## 2. 关键架构决策

### 2.1 为什么用 Mixin @RETURN 注入，而不是 KeyInputEvent？

**KeyInputEvent 的局限**：Forge 的 `KeyInputEvent` 在 `Minecraft.runTickKeyboard()` 中触发，**早于** `EntityPlayerSP.updateMovementInput()`。我们在 KeyInputEvent 中强制 `keyBindJump.pressed = true`，但 vanilla 的 `updateMovementInput` 会用 `keyBindJump.isKeyDown()` 重新读取 —— 而 `isKeyDown()` 读的是事件队列状态，IME 已经吞掉了事件，依然为 false。

**@RETURN 注入的优势**：
- 在 vanilla 计算 `movementInput` 之后执行
- 直接修改 `movementInput.jump = true`
- 此时距离玩家物理动作只过了几毫秒，物理按键状态依然有效
- 不需要拦截事件流，不破坏其他模组的事件订阅

### 2.2 为什么用 Keyboard.isKeyDown() 而不是 KeyBinding.isKeyDown()？

**KeyBinding.isKeyDown()** 读取的是事件队列派发后的状态，会被 IME 影响：
- IME 消费了 `WM_KEYDOWN(VK_SPACE)`，`Keyboard.next()` 永远拿不到该事件
- KeyBinding 的 `pressed` 字段永远为 false

**Keyboard.isKeyDown(int)** 直接读取 LWJGL 维护的 `keyDownBuffer`，这个 buffer 由窗口过程 `WindowProc` 在更底层填充，**IME 拦截不到**。

### 2.3 为什么不通过反射修改 KeyBinding？

**反射开销**：每 tick 反射调用 `Field.setBoolean()` 比 `Keyboard.isKeyDown()` 慢 10~100 倍。
**风险**：Forge / Mixin 字节码改写后，反射可能找不到字段。
**决策**：本模组**禁止在 tick 循环中反射**，全部使用直接字段写入 + Mixin @Shadow。

### 2.4 为什么抽取 KeyPollingLogic 到独立类？

**可测试性**：Minecraft 类不可在普通 JUnit 环境实例化（依赖 LWJGL Context、GLFW 窗口），导致纯 Mixin 代码无法单元测试。

**解决方案**：
- `MovementInputState`：纯 POJO，镜像 1.8.9 MovementInput 的四个公共字段
- `KeyPollingLogic.apply()`：纯静态方法，接收 state + PhysicalKeyState 接口
- `KeyPollingLogicTest`：14 个 JUnit 测试，覆盖所有状态机分支

Mixin 只负责：守卫 → 快照 → 读取物理键 → 调用 logic → 写回。所有状态决策都在可测试的逻辑层。

### 2.5 为什么 MixinGuiScreen 而不是 MixinGuiChat？

**GuiChat 不重写 updateScreen/onGuiClosed**：1.8.9 的 `GuiChat` 没有这两个方法，直接 Mixin 进 GuiChat 会报 `Target method not found`。

**解决方案**：Mixin 进 `GuiScreen`（基类），运行时用 `instanceof GuiChat` 过滤。`updateScreen` 每 tick 调用一次（仅当 GUI 打开），instanceof 检查开销可忽略。

### 2.6 为什么检测 Lunar/Badlion 后只禁用 polling，不直接禁用整个模组？

**理由**：
- 第三方客户端通常已修复跳跃吞键，polling 是冗余
- 但 GUI IME 守卫（聊天框 Shift 切换中英文不泄漏到潜行）他们通常**没修**
- 保留 GUI 守卫，禁用 polling，既避免重复处理又保留增量价值

## 3. 数据流

### 3.1 单 tick 完整流程（玩家按住 Shift+Space）

```
T+0ms   玩家物理按下 Shift
        → WindowProc → LWJGL keyDownBuffer[KEY_LSHIFT] = true
        → WM_KEYDOWN → ImmTranslateMessage → IME 切换中英状态
        → Keyboard.next() 返回事件 → KeyBinding.setKeyBindState(LSHIFT, true)
        → keyBindSneak.pressed = true

T+10ms  玩家物理按下 Space
        → WindowProc → LWJGL keyDownBuffer[KEY_SPACE] = true
        → WM_KEYDOWN → ImmTranslateMessage → IME 消费 (Shift+Space = 全角切换)
        → Keyboard.next() 不返回任何事件 ← 关键：IME 拦截！
        → keyBindJump.pressed 保持 false

T+50ms  Minecraft runTick → EntityPlayerSP.updateMovementInput()
        → vanilla: movementInput.sneak = keyBindSneak.isKeyDown() = true
        → vanilla: movementInput.jump  = keyBindJump.isKeyDown()  = false  ← BUG!
        → Mixin @RETURN 触发：
            - 检查 Keyboard.isKeyDown(KEY_SPACE) = true ← 物理状态保留！
            - movementInput.jump 已经是 false
            - KeyPollingLogic.pollJump → 设 jump = true
        → movementInput.jump = true ← 修复成功！

T+50ms  EntityPlayerSP.onLivingUpdate()
        → 读取 movementInput.jump → 触发跳跃
        → 读取 movementInput.sneak → 触发潜行
        → 同时蹲跳成功
```

### 3.2 GUI 守卫数据流（玩家在聊天框按 Shift 切换中英文）

```
T+0     玩家在 GuiChat 中按 Shift
        → Keyboard.next() → KeyBinding.setKeyBindState(LSHIFT, true)
        → keyBindSneak.pressed = true ← 泄漏！

T+50ms  MixinGuiScreen.updateScreen @RETURN
        → 检查 instanceof GuiChat = true
        → 检查 Keyboard.isKeyDown(keyBindSneak) = true
        → KeyBinding.setKeyBindState(sneakCode, false) ← 强制清除
        → keyBindSneak.pressed = false

T+100ms 玩家关闭 GuiChat
        → MixinGuiScreen.onGuiClosed @RETURN
        → 再次检查 Shift 仍按住 → 再次清除
        → 下一 tick 的 updateMovementInput 不会触发潜行

        ↑ 没有这个守卫，玩家关闭聊天后会突然潜行一拍
```

## 4. 模块职责

| 模块 | 职责 | 不做的事 |
|---|---|---|
| [LShiftFix](file:///e:/Code/LShiftFix/src/main/java/com/ninefyu/lshiftfix/LShiftFix.java) | Mod 入口、生命周期、配置加载、热键检测 | 不在 tick 中做任何工作 |
| [LShiftFixConfig](file:///e:/Code/LShiftFix/src/main/java/com/ninefyu/lshiftfix/config/LShiftFixConfig.java) | 配置读写、reload/save | 不缓存任何 volatile 之外的值 |
| [MixinEntityPlayerSP](file:///e:/Code/LShiftFix/src/main/java/com/ninefyu/lshiftfix/mixin/MixinEntityPlayerSP.java) | 注入点、守卫、快照/写回 | 不做任何状态决策 |
| [KeyPollingLogic](file:///e:/Code/LShiftFix/src/main/java/com/ninefyu/lshiftfix/logic/KeyPollingLogic.java) | 状态机决策 | 不读 LWJGL、不读 Minecraft |
| [MovementInputState](file:///e:/Code/LShiftFix/src/main/java/com/ninefyu/lshiftfix/logic/MovementInputState.java) | 纯 POJO | 不依赖任何 Minecraft 类 |
| [MixinGuiScreen](file:///e:/Code/LShiftFix/src/main/java/com/ninefyu/lshiftfix/mixin/MixinGuiScreen.java) | GUI IME 守卫 | 不修改 movementInput |
| [CompatHandler](file:///e:/Code/LShiftFix/src/main/java/com/ninefyu/lshiftfix/compat/CompatHandler.java) | 第三方客户端检测 | 不主动修改配置（只读 + warn） |
| [CommandLShiftFix](file:///e:/Code/LShiftFix/src/main/java/com/ninefyu/lshiftfix/command/CommandLShiftFix.java) | 命令注册、配置热重载 | 不读 LWJGL |
| [GuiConfigLShiftFix](file:///e:/Code/LShiftFix/src/main/java/com/ninefyu/lshiftfix/gui/GuiConfigLShiftFix.java) | 图形化配置 | 不依赖 Mixin |

## 5. 性能预算

每 tick 在 `updateMovementInput` 注入路径上的开销：

| 步骤 | 纳秒 | 备注 |
|---|---|---|
| 守卫检查（5个 null/状态判断） | ~50 ns | 短路返回 |
| `refreshKeyCache` (6 次 getKeyCode) | ~200 ns | 字段读取 |
| `new MovementInputState()` + 4 次赋值 | ~30 ns | 对象分配 |
| `new PhysicalKeyState()` (匿名类) | ~50 ns | 对象分配 |
| 6 次 `Keyboard.isKeyDown` | ~600 ns | LWJGL 数组访问 |
| `KeyPollingLogic.apply` 决策 | ~100 ns | 纯逻辑 |
| `movementInput` 字段写回 | ~30 ns | 直接字段写入 |
| **总计** | **~1.1 μs / tick** | 60 fps 下占 0.0066% CPU |

## 6. 字节码层面

### 6.1 Mixin 注入点

```java
// EntityPlayerSP (1.8.9 SRG names, post-reobf)
public void func_175161_p() {       // updateMovementInput
    // ... vanilla code ...
    this.field_71158_b.field_78899_e = this.field_71158_b.field_78899_e; // jump
    // ... more vanilla ...
    // ← LShiftFix mixin injects HERE (RETURN)
    return;
}
```

### 6.2 refmap 机制

构建时 MixinGradle 生成 `mixins.lshiftfix.refmap.json`，记录每个 `@At` 在不同映射下的目标名：

```json
{
  "mappings": {
    "updateMovementInput": "func_175161_p"
  }
}
```

运行时 Mixin 用 refmap 把 `updateMovementInput` 翻译为 SRG 名 `func_175161_p`，再用 SRG 名定位字节码方法。**这是为什么生产 jar 必须 reobf** —— 没有reobf，类名是 MCP 名（`updateMovementInput`），Mixin 找不到。

## 7. 扩展点

### 7.1 添加新的兼容性检测

在 [CompatHandler.detect()](file:///e:/Code/LShiftFix/src/main/java/com/ninefyu/lshiftfix/compat/CompatHandler.java#L41) 中追加：

```java
if (Loader.isModLoaded("somemod")) {
    LShiftFix.LOGGER.info("[LShiftFix] SomeMod detected — adjusting...");
    // your logic
}
```

### 7.2 添加新的轮询规则

在 [KeyPollingLogic.apply()](file:///e:/Code/LShiftFix/src/main/java/com/ninefyu/lshiftfix/logic/KeyPollingLogic.java#L51) 中追加新的 `pollXxx` 方法，并写对应的 JUnit 测试。

### 7.3 添加新的配置项

1. 在 [LShiftFixConfig](file:///e:/Code/LShiftFix/src/main/java/com/ninefyu/lshiftfix/config/LShiftFixConfig.java) 中添加 `public static volatile` 字段
2. 在 `loadInternal()` 中读取
3. 在 `save()` 中写入
4. 在 [CommandLShiftFix](file:///e:/Code/LShiftFix/src/main/java/com/ninefyu/lshiftfix/command/CommandLShiftFix.java) 中添加 `case "xxx"`
5. 在 [GuiConfigLShiftFix](file:///e:/Code/LShiftFix/src/main/java/com/ninefyu/lshiftfix/gui/GuiConfigLShiftFix.java) 中添加按钮

## 8. 已知限制

| 限制 | 原因 | 缓解 |
|---|---|---|
| 仅 1.8.9 | Mixin refmap 与字节码定位针对 1.8.9 SRG 名 | 用户可 fork 后改 mappings |
| 仅 Forge | 依赖 Forge 的 Mod/EventHandler 体系 | Fabric 需重写主类 |
| 不支持鼠标绑定的跳跃键 | `Keyboard.isKeyDown` 不读鼠标 | 可加 `Mouse.isKeyDown` |
| 不支持手柄 | 1.8.9 没有手柄 API | 需要专门的 mod |
| GUI 守卫依赖 KeyBinding API | 玩家若完全自定义输入系统则失效 | 当前方案覆盖 99% 用户 |

## 9. 贡献指南

1. Fork → branch → commit → PR
2. 代码风格：4 空格缩进、UTF-8、LF 换行
3. 注释英文，文档中文
4. 新增逻辑必须有对应单元测试（`src/test/java/`）
5. 不要在 tick 路径中引入反射
6. PR 描述请包含：动机、变更点、测试结果、兼容性影响
