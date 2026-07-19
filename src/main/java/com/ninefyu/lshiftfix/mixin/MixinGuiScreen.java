package com.ninefyu.lshiftfix.mixin;

import com.ninefyu.lshiftfix.LShiftFix;
import com.ninefyu.lshiftfix.config.LShiftFixConfig;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * GUI IME guard for {@link GuiScreen}.
 *
 * <p>Problem: many IMEs (Sogou, Microsoft Pinyin, QQ, ...) use Shift as a
 * Chinese/English toggle and Shift+Space as full/half-width toggle. When
 * the player types in chat and presses Shift to switch languages, vanilla's
 * KeyBinding pipeline can briefly see the Shift press and leak it into
 * sneak state, which is then read by EntityPlayerSP on the next tick.</p>
 *
 * <p>We target {@link GuiScreen} (not GuiChat) because GuiChat does not
 * override {@code updateScreen} or {@code onGuiClosed} — both are defined
 * on GuiScreen. We only activate the guard when the current screen is a
 * GuiChat (or its subclass).</p>
 *
 * <p>The suppressed keycode is read from {@code gameSettings.keyBindSneak}
 * at runtime, so the guard respects custom sneak rebindings.</p>
 */
@Mixin(GuiScreen.class)
public abstract class MixinGuiScreen {

    @Inject(method = "updateScreen", at = @At("RETURN"))
    private void lshiftfix$suppressShiftInChat(CallbackInfo ci) {
        if (!LShiftFixConfig.enableGuiImeGuard) return;
        if (!((Object) this instanceof GuiChat)) return;

        suppressSneakBindingIfShiftHeld();
    }

    @Inject(method = "onGuiClosed", at = @At("RETURN"))
    private void lshiftfix$clearShiftResidueOnClose(CallbackInfo ci) {
        if (!LShiftFixConfig.enableGuiImeGuard) return;
        if (!((Object) this instanceof GuiChat)) return;

        suppressSneakBindingIfShiftHeld();
        if (LShiftFixConfig.enableDebugLog) {
            LShiftFix.LOGGER.debug("[LShiftFix] Cleared sneak KeyBinding residue on GuiChat close.");
        }
    }

    /**
     * If the user's bound sneak key is physically held right now, mark that
     * KeyBinding as un-pressed. This is what stops the IME language-toggle
     * Shift press from leaking into sneak state while chat is open.
     *
     * <p>We read the sneak keycode from GameSettings rather than hard-coding
     * LSHIFT/RSHIFT so that users who rebound sneak to another key are still
     * protected.</p>
     */
    private void suppressSneakBindingIfShiftHeld() {
        if (!Keyboard.isCreated()) return;

        GameSettings settings = LShiftFix.getCachedSettings();
        if (settings == null) return;

        int sneakCode = settings.keyBindSneak.getKeyCode();
        if (sneakCode < 0) return;

        try {
            if (Keyboard.isKeyDown(sneakCode)) {
                KeyBinding.setKeyBindState(sneakCode, false);
            }
        } catch (Throwable t) {
            LShiftFix.LOGGER.warn("[LShiftFix] Failed to suppress sneak KeyBinding: {}", t.getMessage());
        }
    }
}
