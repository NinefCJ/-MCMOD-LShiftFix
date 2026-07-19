package com.ninefyu.lshiftfix.logic.polling;

import com.ninefyu.lshiftfix.logic.KeyPollingLogic.PhysicalKeyState;
import com.ninefyu.lshiftfix.logic.MovementInputState;

/**
 * Re-poll the forward key (W by default).
 *
 * <p>In 1.8.9 MovementInput, forward is represented by
 * {@code moveForward > 0} (typically +1.0f). We only force-forward when
 * the current value is not already positive.</p>
 */
public final class ForwardKeyRule implements IPollingRule {

    @Override
    public boolean apply(MovementInputState state, PhysicalKeyState keys) {
        if (state.moveForward <= 0.0f && keys.isForwardPressed()) {
            state.moveForward = 1.0f;
            return true;
        }
        return false;
    }
}
