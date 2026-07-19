package com.ninefyu.lshiftfix.compat;

import com.ninefyu.lshiftfix.LShiftFix;
import com.ninefyu.lshiftfix.config.LShiftFixConfig;
import com.ninefyu.lshiftfix.i18n.L10n;
import net.minecraftforge.fml.common.Loader;

/**
 * Runtime compatibility detector.
 *
 * <p>Checks for known 1.8.9 clients/mods that may conflict with LShiftFix
 * or already implement the same fix, and adjusts behavior accordingly.</p>
 *
 * <ul>
 *   <li><b>Lunar Client / Badlion Client</b>: third-party clients that ship
 *       their own input pipeline. They detect LShiftFix as redundant and
 *       disable the polling fix to avoid double-processing.</li>
 *   <li><b>OptiFine</b>: pure rendering mod, no conflict. Logged for info.</li>
 *   <li><b>PlayerAPI</b>: replaces EntityPlayerSP — we log a warning because
 *       their hook may run BEFORE our RETURN-injected fix, causing state
 *       inconsistency. We do NOT disable; user can decide.</li>
 * </ul>
 */
public final class CompatHandler {

    private static boolean detectedLunar      = false;
    private static boolean detectedBadlion    = false;
    private static boolean detectedOptifine   = false;
    private static boolean detectedPlayerAPI  = false;

    private CompatHandler() {}

    public static void detect() {
        detectedLunar     = detectClass("com.lunarclient.lunar.ClientLauncher")
                         || detectClass("com.lunarclient.bootstrap.Bootstrap")
                         || "true".equalsIgnoreCase(System.getProperty("lunarclient"));
        detectedBadlion   = detectClass("net.badlion.client.BadlionClient")
                         || detectClass("net.badlion.base.BaseClient");
        detectedOptifine  = detectClass("optifine.OptiFineForgeTweaker")
                         || detectClass("net.optifine.Config")
                         || Loader.isModLoaded("optifine");
        detectedPlayerAPI = Loader.isModLoaded("playerapi");

        if (detectedLunar) {
            LShiftFix.LOGGER.warn("[LShiftFix] " + L10n.format(L10n.COMPAT_LUNAR));
            LShiftFixConfig.enableAllKeyPolling = false;
        }

        if (detectedBadlion) {
            LShiftFix.LOGGER.warn("[LShiftFix] " + L10n.format(L10n.COMPAT_BADLION));
            LShiftFixConfig.enableAllKeyPolling = false;
        }

        if (detectedOptifine) {
            LShiftFix.LOGGER.info("[LShiftFix] " + L10n.format(L10n.COMPAT_OPTIFINE));
        }

        if (detectedPlayerAPI) {
            LShiftFix.LOGGER.warn("[LShiftFix] " + L10n.format(L10n.COMPAT_PLAYERAPI));
        }
    }

    public static boolean isThirdPartyClient() {
        return detectedLunar || detectedBadlion;
    }

    public static boolean shouldSkipPolling() {
        return isThirdPartyClient();
    }

    public static boolean isOptifinePresent() {
        return detectedOptifine;
    }

    public static boolean isPlayerApiPresent() {
        return detectedPlayerAPI;
    }

    private static boolean detectClass(String className) {
        try {
            Class.forName(className, false, CompatHandler.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        } catch (Throwable t) {
            return false;
        }
    }
}
