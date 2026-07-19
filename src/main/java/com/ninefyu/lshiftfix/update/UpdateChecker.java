package com.ninefyu.lshiftfix.update;

import com.ninefyu.lshiftfix.LShiftFix;
import com.ninefyu.lshiftfix.i18n.L10n;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Asynchronous Modrinth version checker.
 *
 * <p>Queries the Modrinth API endpoint {@code /v2/project/{id}/version} on a
 * background thread so that startup is never blocked. The result is queued and
 * displayed in chat the first time the player joins a world.</p>
 *
 * <p>Endpoint reference: https://docs.modrinth.com/api/</p>
 */
public final class UpdateChecker {

    private static final String MODRINTH_ID = "vxJbizDU";
    private static final String API_URL = "https://api.modrinth.com/v2/project/" + MODRINTH_ID + "/version";

    private static volatile String latestVersion = null;
    private static volatile boolean checked = false;
    private static volatile boolean notified = false;

    private UpdateChecker() {}

    /**
     * Start an asynchronous update check. Returns immediately; the actual HTTP
     * request runs on a daemon thread.
     */
    public static void checkAsync() {
        if (checked) return;
        checked = true;

        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    String latest = fetchLatestVersion();
                    if (latest != null && !latest.equals(LShiftFix.VERSION)) {
                        latestVersion = latest;
                        LShiftFix.LOGGER.info("[LShiftFix] New version available: {} (current: {})",
                            latest, LShiftFix.VERSION);
                    }
                } catch (Throwable t) {
                    LShiftFix.LOGGER.warn("[LShiftFix] "
                        + L10n.format(L10n.UPDATE_CHECK_FAIL, t.getMessage()));
                }
            }
        }, "LShiftFix-UpdateChecker");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * If a newer version was found, print it to the player's chat.
     * Called on the first world join after check completes.
     */
    public static void maybeNotifyPlayer() {
        if (notified) return;
        if (latestVersion == null) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;

        notified = true;
        String msg = EnumChatFormatting.YELLOW + "[LShiftFix] "
            + EnumChatFormatting.WHITE + L10n.format(L10n.UPDATE_AVAILABLE,
                latestVersion, LShiftFix.VERSION,
                EnumChatFormatting.AQUA + "https://modrinth.com/project/" + MODRINTH_ID);
        mc.thePlayer.addChatMessage(new ChatComponentText(msg));
    }

    public static String getLatestVersion() {
        return latestVersion;
    }

    public static boolean hasChecked() {
        return checked;
    }

    // ---- internal ----

    /**
     * Hit the Modrinth API and return the latest version number string.
     *
     * <p>The response is a JSON array of version objects. We take the first
     * one (most recent) and extract its {@code version_number} field.</p>
     */
    private static String fetchLatestVersion() throws Exception {
        URL url = new URL(API_URL + "?game_versions=%5B%221.8.9%22%5D&loaders=%5B%22forge%22%5D");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "LShiftFix/" + LShiftFix.VERSION + " (ninefyu)");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        int code = conn.getResponseCode();
        if (code != 200) {
            throw new RuntimeException("HTTP " + code);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        conn.disconnect();

        return VersionJsonParser.parseFirstVersionNumber(sb.toString());
    }
}
