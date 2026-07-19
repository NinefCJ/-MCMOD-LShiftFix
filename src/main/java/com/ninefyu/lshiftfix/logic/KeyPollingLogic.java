package com.ninefyu.lshiftfix.logic;

import com.ninefyu.lshiftfix.logic.polling.PollingRuleSet;

/**
 * Polling logic entry point.
 *
 * <p>Delegates to {@link PollingRuleSet} — a strategy-pattern collection of
 * {@link com.ninefyu.lshiftfix.logic.polling.IPollingRule} implementations.
 * This class is kept as a thin facade for backward compatibility with the
 * Mixin caller and the existing tests.</p>
 *
 * <p>Rule sets are pre-built (not created every tick) so there's zero
 * allocation on the hot path.</p>
 */
public final class KeyPollingLogic {

    private static final PollingRuleSet JUMP_ONLY = PollingRuleSet.jumpOnly();
    private static final PollingRuleSet ALL_KEYS  = PollingRuleSet.allKeys();

    private KeyPollingLogic() {}

    /**
     * Apply polling rules.
     *
     * @param state             movement state to mutate
     * @param keys              physical key state
     * @param enableAllKeyPolling if false, only the jump key is re-polled
     * @return true if any field was changed
     */
    public static boolean apply(MovementInputState state, PhysicalKeyState keys, boolean enableAllKeyPolling) {
        if (state == null || keys == null) {
            throw new NullPointerException("state and keys must not be null");
        }
        PollingRuleSet rules = enableAllKeyPolling ? ALL_KEYS : JUMP_ONLY;
        return rules.applyAll(state, keys);
    }

    /**
     * Callback interface for physical key state.
     * The Mixin adapter reads these from LWJGL's Keyboard.isKeyDown().
     */
    public interface PhysicalKeyState {
        boolean isJumpPressed();
        boolean isSneakPressed();
        boolean isForwardPressed();
        boolean isBackPressed();
        boolean isLeftPressed();
        boolean isRightPressed();
    }
}
