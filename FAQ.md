# FAQ 与故障排查

常见问题与解决方案。如按以下步骤仍无法解决，请到 [Modrinth Issue](https://modrinth.com/project/lshiftfix) 反馈。

---

## 安装相关

### Q1：游戏启动后 LShiftFix 没有加载， Mods 菜单看不到

**A**：依次检查：

1. `.minecraft/mods/` 中是否存在 `LShiftFix-1.0.0.jar` 和 `MixinBooter-*.jar`
2. Minecraft 版本必须是 **1.8.9**（不是 1.8 或 1.8.8）
3. Forge 版本必须是 **11.15.1.x**（推荐 `11.15.1.2318`）
4. 查看 `logs/latest.log`，搜索 `[lshiftfix]` 关键字
   - 若完全没有日志 → Forge 未识别到 jar
   - 若有 `Skipping LShiftFix` → mcmod.info 校验失败
5. 若日志报 `java.lang.NoClassDefFoundError: org/spongepowered/asm/mixin/Mixin` → 缺 MixinBooter

### Q2：启动崩溃，报 `MixinApplyError` 或 `Failed to apply mixin`

**A**：常见原因：

| 报错信息 | 原因 | 解决 |
|---|---|---|
| `Target class not found: net.minecraft.client.entity.EntityPlayerSP` | MixinBooter 版本太旧 | 升级到 `1.2-beta-1` 或更新 |
| `Target method not found: updateMovementInput` | MC 版本不是 1.8.9 | 确认客户端为 1.8.9 |
| `Incompatible Mixin version` | MixinBooter 与本模组 Mixin 版本不兼容 | 升级 MixinBooter |
| `Cannot find refmap` | 构建时 refmap 未生成 | 重新下载官方发布的 jar，勿自行构建 |

### Q3：报 `required-after:mixinbooter` 缺失

**A**：MixinBooter 未安装或版本过旧。

1. 从 [Modrinth 下载](https://modrinth.com/mod/mixinbooter/versions?g=1.8.9)
2. 选择 **1.8.9 兼容版本**
3. 放入 `.minecraft/mods/`，与 LShiftFix 同目录

---

## 功能相关

### Q4：装了模组后还是不能蹲跳同时进行

**A**：按顺序排查：

1. **检查 Mixin 是否注入**：日志搜索 `Physical jump-key polling is now active`
   - 没有 → Mixin 未加载，回到 Q1/Q2
2. **检查第三方客户端**：日志搜索 `Lunar Client detected` 或 `Badlion Client detected`
   - 若有，这些客户端已自带输入修复，本模组会自动禁用 polling
3. **检查 GUI 守卫**：聊天框打开时跳跃失效是正常的（避免打字误触），关闭聊天框再试
4. **检查 keyBindJump 绑定**：确认 Controls 设置里跳跃键是 `Space` 或你重绑定的键
5. **开启 debug 日志**：`/lshiftfix debug on`，蹲跳一次，日志应出现 `IME swallowed a movement key event`
   - 没有该日志 → 物理 Space 没被检测到，可能 IME 完全拦截了硬件层（罕见）
   - 有该日志但游戏没跳 → MovementInput 写入被覆盖，提交 issue

### Q5：聊天框打字时，玩家会突然跳跃

**A**：这是 GUI 守卫未生效。检查：

1. 配置项 `enableGuiImeGuard` 必须为 `true`（默认）
2. MixinGuiScreen 是否成功注入：日志搜索 `MixinGuiScreen`
3. 用 `/lshiftfix guard on` 强制开启
4. 若使用 PlayerAPI，可能与本模组的 GuiScreen 注入冲突，临时关闭 PlayerAPI 测试

### Q6：开启 all-key polling 后，WASD 移动反而失效

**A**：可能你的 IME 完全接管了字母键（很罕见）。关闭 polling：

```
/lshiftfix polling off
```

仅保留跳跃修复（这是最常见场景）。

### Q7：`/lshiftfix` 命令显示"无权限"

**A**：命令需要 OP 等级 2。

- **单人游戏**：你天然是 OP 4，不会遇到此问题
- **局域网联机**：房主在控制台执行 `op <你的名字>` 给自己 OP
- **服务器**：让管理员执行 `op <你的名字>`

如希望所有玩家都能用，自行修改 [CommandLShiftFix.java](file:///e:/Code/LShiftFix/src/main/java/com/ninefyu/lshiftfix/command/CommandLShiftFix.java#L38-L40) 中的 `getRequiredPermissionLevel()` 返回 `0`。

---

## 兼容性相关

### Q8：和 OptiFine 一起用会冲突吗？

**A**：不会。OptiFine 只修改渲染管线，不碰输入系统。LShiftFix 启动时会检测并打印日志 `OptiFine detected — no conflict expected`。

### Q9：和 PlayerAPI 一起用会冲突吗？

**A**：理论不会，但 PlayerAPI 会替换 `EntityPlayerSP`，可能导致 Mixin 注入的目标类不同。

- 启动时若打印 `PlayerAPI detected`，表示已检测到，请测试蹲跳是否正常
- 若失效，请在 issue 中附 `latest.log` 全文

### Q10：和 Lunar Client / Badlion 一起用会冲突吗？

**A**：本模组会自动检测这些第三方客户端，**自动禁用** 主 polling 逻辑（避免双重处理），仅保留 GUI IME 守卫。如日志看到 `Lunar Client detected` 或 `Badlion Client detected` 即为正常。

实际上这些客户端已自带 IME 修复，你**不需要**安装本模组。

### Q11：和 LabyMod / OAM / SkyblockAddons 等其他 1.8.9 模组会冲突吗？

**A**：理论上不会。本模组仅在 `EntityPlayerSP.updateMovementInput` 的 `RETURN` 注入、`GuiScreen.updateScreen` / `onGuiClosed` 的 `RETURN` 注入，不替换原方法、不修改字节码顺序，与其他 Mixin 模组可共存。

如遇到具体冲突，请附 `latest.log` 与冲突模组名称提交 issue。

---

## 构建相关

### Q12：本地构建报 `LShiftFix build requires JDK 8`

**A**：ForgeGradle 2.1 只支持 JDK 8。解决：

1. **安装 JDK 8**：[Adoptium Temurin 8 LTS](https://adoptium.net/temurin/releases/?version=8)
2. **指定 JAVA_HOME**：
   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Java\jdk1.8.0_xxx"
   .\gradlew.bat build
   ```
3. **或在 gradle.properties 中指定**：
   ```properties
   org.gradle.java.home=C:/Program Files/Java/jdk1.8.0_xxx/jre
   ```
4. **或直接用 GitHub Actions**：仓库已配置 CI 自动构建，提交后即可在 Actions 页面下载

### Q13：本地没有 gradlew，怎么构建？

**A**：仓库不包含 gradlew 二进制（无法由 AI 生成）。两种方式：

```powershell
# 方式 A：系统已装任意版本 Gradle
gradle wrapper --gradle-version 2.14.1
.\gradlew.bat build

# 方式 B：直接下载 Gradle 2.14.1 standalone
curl -sSL -o gradle.zip https://services.gradle.org/distributions/gradle-2.14.1-bin.zip
Expand-Archive gradle.zip -DestinationPath C:\
C:\gradle-2.14.1\bin\gradle.bat build
```

### Q14：CI 上构建失败，报 `gradlew: No such file or directory`

**A**：已修复。CI 配置已改为直接下载 Gradle 2.14.1，不依赖 gradlew。

### Q15：构建成功但 jar 无法在游戏里加载

**A**：常见原因：

1. **没用 reobf 后的 jar**：生产 jar 必须是 SRG 名的，路径为 `build/libs/LShiftFix-1.0.0.jar`（不是 `-dev.jar`）
2. **MixinBooter 未装**：本模组强依赖 MixinBooter
3. **MC 版本不匹配**：jar 内的 Mixin refmap 是 1.8.9 的，1.8.8/1.8 都不行

---

## 输入法侧修复（推荐配合）

即使本模组修复了跳跃，**仍建议关闭 IME 的 Shift+Space 热键**，避免在 Ctrl+Space（中英切换）、Shift+Space（全角切换）等场景被 IME 干扰。详见 [README.md → 输入法侧修复指南](./README.md#输入法侧修复指南推荐配合使用)。

---

## 仍未解决？

提交 issue 时请附：

1. `logs/latest.log` 完整日志（可粘贴到 pastebin/gist）
2. `.minecraft/mods/` 目录截图
3. 客户端类型（原版 Forge / Lunar / Badlion / LabyMod）
4. 输入法名称与版本
5. 复现步骤（具体按键序列）
6. `/lshiftfix status` 输出截图
