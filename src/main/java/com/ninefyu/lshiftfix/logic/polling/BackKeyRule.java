package com.ninefyu.lshiftfix.logic.polling;

import com.ninefyu.lshiftfix.logic.KeyPollingLogic.PhysicalKeyState;
import com.ninefyu.lshiftfix.logic.MovementInputState;

/**
 * Re-poll the back key (S by default).
 *
 * <p>In 1.8.9 MovementInput, back is represented by
 * {@code moveForward < 0} (typically -1.0f). We only force-back when
 * the current value is not already negative.</p>
 */
public final class BackKeyRule implements IPollingRule {

    @Override
    public boolean apply(MovementInputState state, PhysicalKeyState keys) {
        if (state.moveForward >= 0.0f && keys.isBackPressed()) {
            state.moveForward = -1.0f;
            return true;
        }
        return false;
    }
}
