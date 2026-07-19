package com.ninefyu.lshiftfix.mixin;

import com.ninefyu.lshiftfix.LShiftFix;
import com.ninefyu.lshiftfix.compat.CompatHandler;
import com.ninefyu.lshiftfix.config.LShiftFixConfig;
import com.ninefyu.lshiftfix.logic.KeyPollingLogic;
import com.ninefyu.lshiftfix.logic.MovementInputState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.MovementInput;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Core fix. Injected at the tail of EntityPlayerSP.updateMovementInput().
 *
 * <p>Delegates the actual IME-bypass state machine to
 * {@link KeyPollingLogic}, which is unit-tested in
 * {@code src/test/java/.../KeyPollingLogicTest}. This Mixin only:</p>
 * <ol>
 *   <li>Guards against uninitialized Minecraft / Keyboard / GUI screens.</li>
 *   <li>Reads physical key state via LWJGL {@link Keyboard#isKeyDown(int)}.</li>
 *   <li>Copies {@link MovementInput} fields into a plain
 *       {@link MovementInputState}, runs the logic, and copies back.</li>
 * </ol>
 */
@Mixin(EntityPlayerSP.class)
public abstract class MixinEntityPlayerSP {

    @Shadow
    public MovementInput movementInput;

    private int debugLogCooldown = 0;

    // Cached key codes (refreshed every tick).
    private int cachedJumpKey    = -1;
    private int cachedForwardKey = -1;
    private int cachedBackKey    = -1;
    private int cachedLeftKey    = -1;
    private int cachedRightKey   = -1;
    private int cachedSneakKey   = -1;

    @Inject(method = "updateMovementInput", at = @At("RETURN"))
    private void lshiftfix$forceJumpOnPhysicalPress(CallbackInfo ci) {
        if (!LShiftFix.isModInitialized()) return;
        if (this.movementInput == null) return;

        // Third-party clients (Lunar/Badlion) already handle IME — skip.
        if (CompatHandler.shouldSkipPolling()) return;

        Minecraft mc = LShiftFix.getMinecraft();
        if (mc == null) return;

        if (!isKeyboardInitialized()) return;
        if (mc.currentScreen != null) return;

        GameSettings settings = LShiftFix.getCachedSettings();
        if (settings == null) return;

        refreshKeyCache(settings);

        // Snapshot the current MovementInput into a plain state object.
        MovementInputState state = new MovementInputState();
        state.moveForward = this.movementInput.moveForward;
        state.moveStrafe  = this.movementInput.moveStrafe;
        state.jump        = this.movementInput.jump;
        state.sneak       = this.movementInput.sneak;

        // Build the physical-key adapter.
        KeyPollingLogic.PhysicalKeyState keys = new KeyPollingLogic.PhysicalKeyState() {
            public boolean isJumpPressed()    { return cachedJumpKey    >= 0 && Keyboard.isKeyDown(cachedJumpKey); }
            public boolean isSneakPressed()   { return cachedSneakKey   >= 0 && Keyboard.isKeyDown(cachedSneakKey); }
            public boolean isForwardPressed() { return cachedForwardKey >= 0 && Keyboard.isKeyDown(cachedForwardKey); }
            public boolean isBackPressed()    { return cachedBackKey    >= 0 && Keyboard.isKeyDown(cachedBackKey); }
            public boolean isLeftPressed()    { return cachedLeftKey    >= 0 && Keyboard.isKeyDown(cachedLeftKey); }
            public boolean isRightPressed()   { return cachedRightKey   >= 0 && Keyboard.isKeyDown(cachedRightKey); }
        };

        boolean changed = KeyPollingLogic.apply(state, keys, LShiftFixConfig.enableAllKeyPolling);

        if (changed) {
            // Write back only the fields that may have changed.
            this.movementInput.moveForward = state.moveForward;
            this.movementInput.moveStrafe  = state.moveStrafe;
            this.movementInput.jump        = state.jump;
            this.movementInput.sneak       = state.sneak;

            if (LShiftFixConfig.enableDebugLog) {
                logFix();
            }
        }
    }

    private void refreshKeyCache(GameSettings settings) {
        cachedJumpKey    = settings.keyBindJump.getKeyCode();
        cachedForwardKey = settings.keyBindForward.getKeyCode();
        cachedBackKey    = settings.keyBindBack.getKeyCode();
        cachedLeftKey    = settings.keyBindLeft.getKeyCode();
        cachedRightKey   = settings.keyBindRight.getKeyCode();
        cachedSneakKey   = settings.keyBindSneak.getKeyCode();
    }

    private boolean isKeyboardInitialized() {
        try {
            return Keyboard.isCreated();
        } catch (Exception e) {
            return false;
        }
    }

    private void logFix() {
        if (debugLogCooldown > 0) {
            debugLogCooldown--;
            return;
        }
        LShiftFix.LOGGER.info(
            "[LShiftFix] IME swallowed a movement key event — re-polled physical state and forced movement fields."
        );
        debugLogCooldown = LShiftFixConfig.debugLogCooldownTicks;
    }
}
