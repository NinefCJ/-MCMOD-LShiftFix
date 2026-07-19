package com.ninefyu.lshiftfix.logic.polling;

import com.ninefyu.lshiftfix.logic.KeyPollingLogic.PhysicalKeyState;
import com.ninefyu.lshiftfix.logic.MovementInputState;

/**
 * Forces both sneak and jump to true when BOTH are physically pressed.
 *
 * <p>This is the classic IME Shift+Space case: the IME swallows one of the
 * two events, so we enforce consistency when we can confirm both keys are
 * physically held.</p>
 */
public final class ComboShiftSpaceRule implements IPollingRule {

    @Override
    public boolean apply(MovementInputState state, PhysicalKeyState keys) {
        if (!keys.isSneakPressed() || !keys.isJumpPressed()) return false;

        boolean changed = false;
        if (!state.sneak) { state.sneak = true; changed = true; }
        if (!state.jump)  { state.jump  = true; changed = true; }
        return changed;
    }
}
