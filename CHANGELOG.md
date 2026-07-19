# Changelog

All notable changes to this project will be documented in this file.

## [1.0.0] - 2026-07-19

### Added
- Initial release.
- Mixin into `EntityPlayerSP.updateMovementInput` to re-poll the physical jump
  key via `Keyboard.isKeyDown`, bypassing IME event interception.
- GUI guard: skip the fix when any `GuiScreen` is open (chat, book, sign).
- Mouse-binding guard: skip when jump is rebound to a mouse button.
- Configurable debug logging (`config/lshiftfix.cfg`), off by default.
- **全按键轮询模式**：新增 `enableAllKeyPolling` 配置项，可轮询所有移动按键（前进/后退/左/右/潜行），提供完整的 IME 防护
- **多键组合保护**：当 Shift + Space 同时物理按下时，强制保证 sneak 和 jump 状态一致，避免 IME 吞键
- **GUI IME 守卫**：新增 `MixinGuiChat`，在聊天框中按 Shift 切换中英文时不会泄漏到潜行状态
- **日志冷却机制**：新增 `debugLogCooldownTicks` 配置项，防止调试日志刷屏
- **命令热重载**：新增 `/lshiftfix` 命令，支持 `status` / `reload` / `debug` / `polling` / `guard` 子命令，OP 权限等级 2
- **热键冲突检测**：postInit 阶段扫描所有 KeyBinding，警告可能与 IME 冲突的 SPACE 绑定
- **性能优化**：
  - 缓存 `GameSettings` 引用（init 阶段一次性获取）
  - 缓存 KeyBinding 的 keyCode（每 tick 刷新一次，避免重复 `getKeyCode()` 调用）
  - 早退机制：movementInput / Minecraft / Keyboard 任意未就绪时立即返回
- **健壮性增强**：
  - 新增 `isModInitialized()` 检查，防止在初始化完成前运行修复逻辑
  - 新增 `Keyboard.isCreated()` 检查，防止 Keyboard 未初始化时调用 `isKeyDown()`
  - 新增 `movementInput` 非空检查
  - 配置字段全部使用 `volatile` 修饰，确保命令热重载的可见性

### Changed
- **代码重构**：将 Mixin 逻辑拆分为多个独立方法，提高可读性和可维护性
- **Lambda 移除**：移除 Java 8 lambda 表达式，使用匿名内部类，确保与 Mixin 0.6 的完全兼容性
