package com.ninefyu.lshiftfix.i18n;

import net.minecraft.client.resources.I18n;

/**
 * Thin wrapper around Minecraft's {@link I18n} for LShiftFix keys.
 *
 * <p>All keys are prefixed with {@code "lshiftfix."} to avoid collisions
 * with vanilla and other mods. The actual translation files live at:</p>
 * <pre>
 *   assets/lshiftfix/lang/en_US.lang
 *   assets/lshiftfix/lang/zh_CN.lang
 *   assets/lshiftfix/lang/ja_JP.lang
 *   assets/lshiftfix/lang/ko_KR.lang
 * </pre>
 *
 * <p>This class exists mainly so the mixin / config code doesn't scatter
 * raw key strings everywhere — it's a single source of truth for what
 * keys we use.</p>
 */
public final class L10n {

    private L10n() {}

    /**
     * Format a translation key with arguments. Falls back to the key itself
     * if no translation is found (Minecraft's I18n already does this).
     */
    public static String format(String key, Object... args) {
        try {
            return I18n.format(key, args);
        } catch (Throwable t) {
            // I18n may not be available in very early init.
            return key;
        }
    }

    // ---- Config GUI ----
    public static final String GUI_TITLE    = "gui.lshiftfix.title";
    public static final String GUI_VERSION  = "gui.lshiftfix.version";
    public static final String GUI_DEBUG    = "gui.lshiftfix.debug";
    public static final String GUI_POLLING  = "gui.lshiftfix.polling";
    public static final String GUI_GUARD    = "gui.lshiftfix.guard";
    public static final String GUI_UPDATE_CHECK = "gui.lshiftfix.update_check";
    public static final String GUI_RELOAD   = "gui.lshiftfix.reload";
    public static final String GUI_SAVE     = "gui.lshiftfix.save";
    public static final String GUI_DONE     = "gui.lshiftfix.done";
    public static final String GUI_ON       = "gui.lshiftfix.on";
    public static final String GUI_OFF      = "gui.lshiftfix.off";

    // ---- Key bindings ----
    public static final String KEY_CATEGORY       = "key.lshiftfix.category";
    public static final String KEY_TOGGLE_DEBUG   = "key.lshiftfix.toggleDebug";
    public static final String KEY_TOGGLE_POLLING = "key.lshiftfix.togglePolling";
    public static final String KEY_TOGGLE_GUARD   = "key.lshiftfix.toggleGuard";

    // ---- Command messages ----
    public static final String CMD_STATUS_HEADER    = "command.lshiftfix.status.header";
    public static final String CMD_STATUS_INIT      = "command.lshiftfix.status.initialized";
    public static final String CMD_STATUS_DEBUG     = "command.lshiftfix.status.debug";
    public static final String CMD_STATUS_POLLING   = "command.lshiftfix.status.polling";
    public static final String CMD_STATUS_GUARD     = "command.lshiftfix.status.guard";
    public static final String CMD_STATUS_UPDATE    = "command.lshiftfix.status.update";
    public static final String CMD_STATUS_COOLDOWN  = "command.lshiftfix.status.cooldown";
    public static final String CMD_RELOAD_SUCCESS   = "command.lshiftfix.reload.success";
    public static final String CMD_TOGGLE_ON        = "command.lshiftfix.toggle.on";
    public static final String CMD_TOGGLE_OFF       = "command.lshiftfix.toggle.off";
    public static final String CMD_TOGGLE_CHANGED   = "command.lshiftfix.toggle.changed";
    public static final String CMD_TOGGLE_USAGE     = "command.lshiftfix.toggle.usage";

    // ---- Compatibility ----
    public static final String COMPAT_LUNAR     = "compat.lunar";
    public static final String COMPAT_BADLION   = "compat.badlion";
    public static final String COMPAT_OPTIFINE  = "compat.optifine";
    public static final String COMPAT_PLAYERAPI = "compat.playerapi";

    // ---- Update checker ----
    public static final String UPDATE_AVAILABLE  = "update.available";
    public static final String UPDATE_CHECK_FAIL = "update.check_failed";

    // ---- Hotkey conflict detection ----
    public static final String HOTKEY_WARNING  = "hotkey.conflict.warning";
    public static final String HOTKEY_SUMMARY  = "hotkey.conflict.summary";
    public static final String HOTKEY_NONE     = "hotkey.conflict.none";

    // ---- Misc ----
    public static final String MISC_INIT         = "misc.initialized";
    public static final String MISC_INIT_SUMMARY = "misc.init_summary";
}
