package com.ninefyu.lshiftfix;

import com.ninefyu.lshiftfix.command.CommandLShiftFix;
import com.ninefyu.lshiftfix.compat.CompatHandler;
import com.ninefyu.lshiftfix.config.LShiftFixConfig;
import com.ninefyu.lshiftfix.i18n.L10n;
import com.ninefyu.lshiftfix.keybind.LShiftFixKeyBinds;
import com.ninefyu.lshiftfix.update.UpdateChecker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

/**
 * LShiftFix main mod class.
 *
 * <p>Bypasses IME event interception by re-polling physical keyboard state
 * every tick inside EntityPlayerSP.updateMovementInput().</p>
 */
@Mod(
    modid       = LShiftFix.MODID,
    name        = LShiftFix.NAME,
    version     = LShiftFix.VERSION,
    dependencies = "required-after:mixinbooter;",
    guiFactory  = "com.ninefyu.lshiftfix.gui.GuiFactoryLShiftFix"
)
public final class LShiftFix {

    public static final String MODID   = "lshiftfix";
    public static final String NAME    = "LShiftFix";
    public static final String VERSION = "@VERSION@";

    public static final Logger LOGGER = LogManager.getLogger(MODID);

    private static volatile boolean isInitialized = false;
    private static volatile GameSettings cachedSettings = null;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LShiftFixConfig.load(event);
        LOGGER.info("[LShiftFix] " + L10n.format(L10n.MISC_INIT_SUMMARY,
            VERSION,
            LShiftFixConfig.enableDebugLog ? "ENABLED" : "disabled",
            LShiftFixConfig.enableAllKeyPolling ? "ENABLED" : "disabled"));
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // Cache GameSettings reference as soon as Minecraft is available.
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.gameSettings != null) {
            cachedSettings = mc.gameSettings;
        }

        // Register key bindings and input event handler.
        LShiftFixKeyBinds.init();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        CompatHandler.detect();
        isInitialized = true;
        LOGGER.info("[LShiftFix] " + L10n.format(L10n.MISC_INIT));
        detectImeHotkeyConflicts();

        // Kick off async update check (won't block startup).
        if (LShiftFixConfig.enableUpdateCheck) {
            UpdateChecker.checkAsync();
        }
    }

    /**
     * Handle our custom keybind presses.
     * Each toggle key flips the corresponding config option and shows a
     * one-line status message in the action bar.
     */
    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (LShiftFixKeyBinds.toggleDebug.isPressed()) {
            LShiftFixConfig.enableDebugLog = !LShiftFixConfig.enableDebugLog;
            LShiftFixKeyBinds.showToggleMessage("debug", LShiftFixConfig.enableDebugLog);
        }
        if (LShiftFixKeyBinds.togglePolling.isPressed()) {
            LShiftFixConfig.enableAllKeyPolling = !LShiftFixConfig.enableAllKeyPolling;
            LShiftFixKeyBinds.showToggleMessage("polling", LShiftFixConfig.enableAllKeyPolling);
        }
        if (LShiftFixKeyBinds.toggleGuard.isPressed()) {
            LShiftFixConfig.enableGuiImeGuard = !LShiftFixConfig.enableGuiImeGuard;
            LShiftFixKeyBinds.showToggleMessage("guard", LShiftFixConfig.enableGuiImeGuard);
        }
    }

    /**
     * Notify the player about available updates once they are in-game.
     *
     * <p>{@code PlayerLoggedInEvent} is a server-side event and never reaches
     * the client event bus, so we piggy-back on the client player tick instead.
     * {@link UpdateChecker#maybeNotifyPlayer()} is guarded by an internal
     * "already notified" flag, so the repeated tick calls are cheap.</p>
     */
    @SubscribeEvent
    public void onClientPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.side != Side.CLIENT) return;
        UpdateChecker.maybeNotifyPlayer();
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandLShiftFix());
    }

    public static boolean isModInitialized() {
        return isInitialized;
    }

    public static GameSettings getCachedSettings() {
        GameSettings s = cachedSettings;
        if (s == null) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null) {
                s = mc.gameSettings;
                cachedSettings = s;
            }
        }
        return s;
    }

    public static Minecraft getMinecraft() {
        return Minecraft.getMinecraft();
    }

    /**
     * Scan all registered KeyBindings on startup and warn about combinations
     * that are likely to conflict with IME hotkeys (Shift+Space, Ctrl+Space,
     * Shift+letter, etc.).
     */
    private void detectImeHotkeyConflicts() {
        // In 1.8.9, KeyBinding.getKeybinds() returns Set<String> (descriptions),
        // not KeyBinding instances. Use GameSettings.keyBindings array instead.
        KeyBinding[] bindings = Minecraft.getMinecraft().gameSettings.keyBindings;
        if (bindings == null) return;

        int conflicts = 0;
        for (KeyBinding kb : bindings) {
            if (kb == null) continue;
            int code = kb.getKeyCode();
            if (code < 0) continue;

            // Space key combined with Shift modifier at the OS level is the
            // classic IME full/half-width toggle. Flag any binding that uses
            // Space as potentially conflicting.
            if (code == Keyboard.KEY_SPACE) {
                LOGGER.warn("[LShiftFix] " + L10n.format(L10n.HOTKEY_WARNING,
                    kb.getKeyDescription(), code));
                conflicts++;
            }
        }

        if (conflicts > 0) {
            LOGGER.warn("[LShiftFix] " + L10n.format(L10n.HOTKEY_SUMMARY, conflicts));
        } else {
            LOGGER.info("[LShiftFix] " + L10n.format(L10n.HOTKEY_NONE));
        }
    }
}
