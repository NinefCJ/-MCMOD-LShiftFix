package com.ninefyu.lshiftfix.config;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.io.File;

/**
 * Config wrapper for LShiftFix.
 *
 * <p>All fields are mutable so they can be hot-reloaded at runtime via
 * {@code /lshiftfix reload}.</p>
 */
public final class LShiftFixConfig {

    public static volatile boolean enableDebugLog = false;
    public static volatile boolean enableAllKeyPolling = false;
    public static volatile boolean enableGuiImeGuard = true;
    public static volatile boolean enableUpdateCheck = true;
    public static volatile int debugLogCooldownTicks = 20;

    private static volatile File configFile;
    private static volatile Configuration config;

    private LShiftFixConfig() {}

    public static void load(FMLPreInitializationEvent event) {
        configFile = new File(event.getModConfigurationDirectory(), "lshiftfix.cfg");
        loadInternal();
    }

    /** Reload config from disk. Called by {@code /lshiftfix reload}. */
    public static void reload() {
        if (configFile == null) return;
        loadInternal();
    }

    /** Persist current in-memory values back to disk. Called by config GUI. */
    public static void save() {
        if (config == null) {
            if (configFile == null) return;
            config = new Configuration(configFile);
        }
        config.load();
        config.get("general", "enableDebugLog",        false).set(enableDebugLog);
        config.get("general", "enableAllKeyPolling",   false).set(enableAllKeyPolling);
        config.get("general", "enableGuiImeGuard",     true ).set(enableGuiImeGuard);
        config.get("general", "enableUpdateCheck",     true ).set(enableUpdateCheck);
        config.get("general", "debugLogCooldownTicks", 20   ).set(debugLogCooldownTicks);
        if (config.hasChanged()) {
            config.save();
        }
    }

    private static void loadInternal() {
        config = new Configuration(configFile);
        config.load();

        enableDebugLog = config.getBoolean(
            "enableDebugLog",
            "general",
            false,
            "Set to true to log when the mod detects and fixes an IME-swallowed key press. Default: false."
        );

        enableAllKeyPolling = config.getBoolean(
            "enableAllKeyPolling",
            "general",
            false,
            "Set to true to poll ALL movement keys (forward/back/left/right/sneak) from physical state. "
            + "Useful if your IME intercepts other movement keys. Default: false."
        );

        enableGuiImeGuard = config.getBoolean(
            "enableGuiImeGuard",
            "general",
            true,
            "When true, the mod tracks Shift press/release inside GuiChat to prevent IME full/half-width "
            + "toggle (Shift+Space) from leaking into movement state. Default: true."
        );

        enableUpdateCheck = config.getBoolean(
            "enableUpdateCheck",
            "general",
            true,
            "When true, check Modrinth on startup for newer versions and notify in chat. Default: true."
        );

        debugLogCooldownTicks = config.getInt(
            "debugLogCooldownTicks",
            "general",
            20,
            1,
            100,
            "Minimum ticks between debug log entries to prevent spam. Default: 20 (1 second)."
        );

        if (config.hasChanged()) {
            config.save();
        }
    }
}
