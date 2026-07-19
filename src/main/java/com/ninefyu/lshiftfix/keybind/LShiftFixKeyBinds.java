package com.ninefyu.lshiftfix.keybind;

import com.ninefyu.lshiftfix.i18n.L10n;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.input.Keyboard;

/**
 * All custom key bindings for LShiftFix.
 *
 * <p>Registered in {@link LShiftFix#init} via {@link ClientRegistry#registerKeyBinding}.
 * Each binding shows up under the "LShiftFix" category in the Controls menu.</p>
 */
public final class LShiftFixKeyBinds {

    public static KeyBinding toggleDebug;
    public static KeyBinding togglePolling;
    public static KeyBinding toggleGuard;

    private static boolean registered = false;

    private LShiftFixKeyBinds() {}

    public static void init() {
        if (registered) return;

        toggleDebug    = reg("key.lshiftfix.toggleDebug",   Keyboard.KEY_NONE);
        togglePolling = reg("key.lshiftfix.togglePolling", Keyboard.KEY_NONE);
        toggleGuard   = reg("key.lshiftfix.toggleGuard", Keyboard.KEY_NONE);

        registered = true;
    }

    private static KeyBinding reg(String key, int defaultKey) {
        KeyBinding kb = new KeyBinding(key, defaultKey, L10n.KEY_CATEGORY);
        ClientRegistry.registerKeyBinding(kb);
        return kb;
    }

    /**
     * Show a short status message in the action bar when a toggle key is pressed.
     * Uses the translation key of the feature name + on/off state.
     */
    public static void showToggleMessage(String featureKey, boolean value) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;

        String nameKey;
        switch (featureKey) {
            case "debug":   nameKey = L10n.GUI_DEBUG;   break;
            case "polling": nameKey = L10n.GUI_POLLING; break;
            case "guard":   nameKey = L10n.GUI_GUARD;   break;
            default:        nameKey = featureKey;       break;
        }
        String state = value
            ? EnumChatFormatting.GREEN + L10n.format(L10n.GUI_ON)
            : EnumChatFormatting.RED   + L10n.format(L10n.GUI_OFF);
        String msg = EnumChatFormatting.AQUA + "[LShiftFix] "
            + EnumChatFormatting.WHITE + L10n.format(nameKey) + ": " + state;
        mc.thePlayer.addChatMessage(new ChatComponentText(msg));
    }
}
