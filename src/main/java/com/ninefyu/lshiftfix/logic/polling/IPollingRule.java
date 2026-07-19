package com.ninefyu.lshiftfix.logic.polling;

import com.ninefyu.lshiftfix.logic.KeyPollingLogic.PhysicalKeyState;
import com.ninefyu.lshiftfix.logic.MovementInputState;

/**
 * A single polling rule. Each rule inspects the physical key state and may
 * modify the movement input state.
 *
 * <p>Rules are applied in order; later rules see the state after earlier
 * rules have already run. This allows the combo rule to run first and
 * single-key rules to fill in the rest.</p>
 */
public interface IPollingRule {

    /**
     * Apply this rule.
     *
     * @param state current movement state (mutated in-place)
     * @param keys  physical key state
     * @return {@code true} if the rule changed any field
     */
    boolean apply(MovementInputState state, PhysicalKeyState keys);
}
