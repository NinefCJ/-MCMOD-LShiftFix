package com.ninefyu.lshiftfix.logic.polling;

import com.ninefyu.lshiftfix.logic.KeyPollingLogic.PhysicalKeyState;
import com.ninefyu.lshiftfix.logic.MovementInputState;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link ComboShiftSpaceRule}.
 */
public class ComboShiftSpaceRuleTest {

    private static PhysicalKeyState keys(final boolean jump, final boolean sneak) {
        return new PhysicalKeyState() {
            public boolean isJumpPressed()    { return jump; }
            public boolean isSneakPressed()   { return sneak; }
            public boolean isForwardPressed() { return false; }
            public boolean isBackPressed()    { return false; }
            public boolean isLeftPressed()    { return false; }
            public boolean isRightPressed()   { return false; }
        };
    }

    @Test
    public void sets_both_sneak_and_jump_when_both_pressed() {
        MovementInputState s = new MovementInputState();
        boolean changed = new ComboShiftSpaceRule().apply(s, keys(true, true));
        assertTrue(changed);
        assertTrue(s.jump);
        assertTrue(s.sneak);
    }

    @Test
    public void no_change_when_only_jump_pressed() {
        MovementInputState s = new MovementInputState();
        boolean changed = new ComboShiftSpaceRule().apply(s, keys(true, false));
        assertFalse(changed);
        assertFalse(s.jump);
        assertFalse(s.sneak);
    }

    @Test
    public void no_change_when_only_sneak_pressed() {
        MovementInputState s = new MovementInputState();
        boolean changed = new ComboShiftSpaceRule().apply(s, keys(false, true));
        assertFalse(changed);
        assertFalse(s.jump);
        assertFalse(s.sneak);
    }

    @Test
    public void no_change_when_both_already_set() {
        MovementInputState s = new MovementInputState();
        s.jump = true;
        s.sneak = true;
        boolean changed = new ComboShiftSpaceRule().apply(s, keys(true, true));
        assertFalse(changed);
    }

    @Test
    public void fixes_partial_state_jump_true_sneak_false() {
        MovementInputState s = new MovementInputState();
        s.jump = true;
        boolean changed = new ComboShiftSpaceRule().apply(s, keys(true, true));
        assertTrue("sneak should be fixed", changed);
        assertTrue(s.jump);
        assertTrue(s.sneak);
    }

    @Test
    public void fixes_partial_state_sneak_true_jump_false() {
        MovementInputState s = new MovementInputState();
        s.sneak = true;
        boolean changed = new ComboShiftSpaceRule().apply(s, keys(true, true));
        assertTrue("jump should be fixed", changed);
        assertTrue(s.jump);
        assertTrue(s.sneak);
    }
}
