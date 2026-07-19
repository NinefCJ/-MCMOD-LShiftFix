package com.ninefyu.lshiftfix.logic.polling;

import com.ninefyu.lshiftfix.logic.KeyPollingLogic.PhysicalKeyState;
import com.ninefyu.lshiftfix.logic.MovementInputState;

/**
 * Re-poll the jump key only.
 *
 * <p>This is the default (minimum) fix. If the physical jump key is pressed
 * but {@code movementInput.jump} is false, force it to true.</p>
 */
public final class JumpKeyRule implements IPollingRule {

    @Override
    public boolean apply(MovementInputState state, PhysicalKeyState keys) {
        if (!state.jump && keys.isJumpPressed()) {
            state.jump = true;
            return true;
        }
        return false;
    }
}
