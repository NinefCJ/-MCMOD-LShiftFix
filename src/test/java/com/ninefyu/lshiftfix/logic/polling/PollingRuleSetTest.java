package com.ninefyu.lshiftfix.logic.polling;

import com.ninefyu.lshiftfix.logic.KeyPollingLogic.PhysicalKeyState;
import com.ninefyu.lshiftfix.logic.MovementInputState;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link PollingRuleSet} — rule ordering, correct set composition,
 * and integration-level behavior (all rules applied together).
 */
public class PollingRuleSetTest {

    private static PhysicalKeyState keys(final boolean jump, final boolean sneak,
                                          final boolean fwd, final boolean back,
                                          final boolean left, final boolean right) {
        return new PhysicalKeyState() {
            public boolean isJumpPressed()    { return jump; }
            public boolean isSneakPressed()   { return sneak; }
            public boolean isForwardPressed() { return fwd; }
            public boolean isBackPressed()    { return back; }
            public boolean isLeftPressed()    { return left; }
            public boolean isRightPressed()   { return right; }
        };
    }

    @Test
    public void jumpOnly_set_has_exactly_one_rule() {
        PollingRuleSet set = PollingRuleSet.jumpOnly();
        List<IPollingRule> rules = set.getRules();
        assertEquals(1, rules.size());
        assertTrue(rules.get(0) instanceof JumpKeyRule);
    }

    @Test
    public void allKeys_set_has_seven_rules() {
        PollingRuleSet set = PollingRuleSet.allKeys();
        assertEquals(7, set.getRules().size());
    }

    @Test
    public void allKeys_first_rule_is_combo() {
        PollingRuleSet set = PollingRuleSet.allKeys();
        assertTrue(set.getRules().get(0) instanceof ComboShiftSpaceRule);
    }

    @Test
    public void jumpOnly_fixes_shift_space_case_partially() {
        // With only jump rule active, sneak doesn't get fixed if IME ate it.
        MovementInputState s = new MovementInputState();
        boolean changed = PollingRuleSet.jumpOnly().applyAll(s, keys(true, true, false, false, false, false));
        assertTrue(changed);
        assertTrue(s.jump);
        assertFalse("jumpOnly set should NOT fix sneak", s.sneak);
    }

    @Test
    public void allKeys_fixes_shift_space_case_fully() {
        MovementInputState s = new MovementInputState();
        boolean changed = PollingRuleSet.allKeys().applyAll(s, keys(true, true, false, false, false, false));
        assertTrue(changed);
        assertTrue(s.jump);
        assertTrue(s.sneak);
    }

    @Test
    public void allKeys_full_movement_press() {
        MovementInputState s = new MovementInputState();
        boolean changed = PollingRuleSet.allKeys().applyAll(s, keys(true, true, true, false, false, true));
        assertTrue(changed);
        assertTrue(s.jump);
        assertTrue(s.sneak);
        assertEquals(1.0f, s.moveForward, 0.0001f);
        assertEquals(1.0f, s.moveStrafe, 0.0001f);
    }

    @Test
    public void allKeys_does_nothing_when_no_keys_pressed() {
        MovementInputState s = new MovementInputState();
        boolean changed = PollingRuleSet.allKeys().applyAll(s, keys(false, false, false, false, false, false));
        assertFalse(changed);
    }

    @Test
    public void rules_are_unmodifiable() {
        PollingRuleSet set = PollingRuleSet.allKeys();
        boolean threw = false;
        try {
            set.getRules().clear();
        } catch (UnsupportedOperationException e) {
            threw = true;
        }
        assertTrue("rule list should be unmodifiable", threw);
    }
}
