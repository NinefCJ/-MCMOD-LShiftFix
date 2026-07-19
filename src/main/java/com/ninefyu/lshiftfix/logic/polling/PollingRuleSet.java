package com.ninefyu.lshiftfix.logic.polling;

import com.ninefyu.lshiftfix.logic.KeyPollingLogic.PhysicalKeyState;
import com.ninefyu.lshiftfix.logic.MovementInputState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builds and executes an ordered list of {@link IPollingRule} strategies.
 *
 * <p>Two predefined rule sets:</p>
 * <ul>
 *   <li><b>jumpOnly</b>: just the {@link JumpKeyRule}. Lightweight, fixes the
 *       most common IME problem (Shift+Space swallowing jump).</li>
 *   <li><b>allKeys</b>: combo rule + all six single-key rules. For users whose
 *       IME intercepts more than just the jump key.</li>
 * </ul>
 *
 * <p>Rule order matters — combo rules run first so that single-key rules can
 * pick up any remaining un-fixed states.</p>
 */
public final class PollingRuleSet {

    private final List<IPollingRule> rules;

    private PollingRuleSet(List<IPollingRule> rules) {
        this.rules = Collections.unmodifiableList(new ArrayList<IPollingRule>(rules));
    }

    /**
     * Apply every rule in order. Returns true if ANY rule changed the state.
     */
    public boolean applyAll(MovementInputState state, PhysicalKeyState keys) {
        boolean changed = false;
        for (IPollingRule rule : rules) {
            if (rule.apply(state, keys)) {
                changed = true;
            }
        }
        return changed;
    }

    public List<IPollingRule> getRules() {
        return rules;
    }

    /** The minimal rule set — just fix the jump key. */
    public static PollingRuleSet jumpOnly() {
        List<IPollingRule> list = new ArrayList<IPollingRule>();
        list.add(new JumpKeyRule());
        return new PollingRuleSet(list);
    }

    /** The full rule set — combo + all movement keys. */
    public static PollingRuleSet allKeys() {
        List<IPollingRule> list = new ArrayList<IPollingRule>();
        // Combo first (handles the most common IME case)
        list.add(new ComboShiftSpaceRule());
        // Then individual keys (fill in anything the combo didn't cover)
        list.add(new ForwardKeyRule());
        list.add(new BackKeyRule());
        list.add(new LeftKeyRule());
        list.add(new RightKeyRule());
        list.add(new SneakKeyRule());
        list.add(new JumpKeyRule());
        return new PollingRuleSet(list);
    }
}
