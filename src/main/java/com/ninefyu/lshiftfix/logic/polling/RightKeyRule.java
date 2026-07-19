package com.ninefyu.lshiftfix.logic.polling;

import com.ninefyu.lshiftfix.logic.KeyPollingLogic.PhysicalKeyState;
import com.ninefyu.lshiftfix.logic.MovementInputState;

/**
 * Re-poll the right strafe key (D by default).
 *
 * <p>In 1.8.9 MovementInput, right is represented by
 * {@code moveStrafe > 0} (typically +1.0f).</p>
 */
public final class RightKeyRule implements IPollingRule {

    @Override
    public boolean apply(MovementInputState state, PhysicalKeyState keys) {
        if (state.moveStrafe <= 0.0f && keys.isRightPressed()) {
            state.moveStrafe = 1.0f;
            return true;
        }
        return false;
    }
}
