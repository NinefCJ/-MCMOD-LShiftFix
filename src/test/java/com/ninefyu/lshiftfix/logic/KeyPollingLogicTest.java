package com.ninefyu.lshiftfix.logic;

import com.ninefyu.lshiftfix.logic.KeyPollingLogic.PhysicalKeyState;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Pure-logic unit tests for {@link KeyPollingLogic}.
 *
 * <p>These tests do NOT touch Minecraft / Forge / LWJGL — they verify only
 * the IME-bypass state machine in isolation.</p>
 *
 * <p>Run with {@code gradle test} (JUnit 4 on the classpath).</p>
 */
public class KeyPollingLogicTest {

    private static PhysicalKeyState keys(final boolean jump, final boolean sneak,
                                          final boolean forward, final boolean back,
                                          final boolean left, final boolean right) {
        return new PhysicalKeyState() {
            public boolean isJumpPressed()    { return jump; }
            public boolean isSneakPressed()   { return sneak; }
            public boolean isForwardPressed() { return forward; }
            public boolean isBackPressed()    { return back; }
            public boolean isLeftPressed()    { return left; }
            public boolean isRightPressed()   { return right; }
        };
    }

    // -----------------------------------------------------------------
    // Jump-only mode (default: enableAllKeyPolling = false)
    // -----------------------------------------------------------------

    @Test
    public void jumpOnly_mode_sets_jump_when_physical_pressed_and_not_already_true() {
        MovementInputState s = new MovementInputState();
        boolean changed = KeyPollingLogic.apply(s, keys(true, false, false, false, false, false), false);
        assertTrue("should report a change", changed);
        assertTrue("jump should be forced", s.jump);
    }

    @Test
    public void jumpOnly_mode_does_not_touch_other_fields() {
        MovementInputState s = new MovementInputState();
        KeyPollingLogic.apply(s, keys(true, false, true, true, true, true), false);
        assertEquals("forward should be untouched", 0.0f, s.moveForward, 0.0001f);
        assertEquals("strafe should be untouched",  0.0f, s.moveStrafe,  0.0001f);
        assertFalse("sneak should be untouched", s.sneak);
    }

    @Test
    public void jumpOnly_mode_no_change_when_jump_already_true() {
        MovementInputState s = new MovementInputState();
        s.jump = true;
        boolean changed = KeyPollingLogic.apply(s, keys(true, false, false, false, false, false), false);
        assertFalse("should not report a change", changed);
        assertTrue(s.jump);
    }

    @Test
    public void jumpOnly_mode_no_change_when_physical_not_pressed() {
        MovementInputState s = new MovementInputState();
        boolean changed = KeyPollingLogic.apply(s, keys(false, false, false, false, false, false), false);
        assertFalse(changed);
        assertFalse(s.jump);
    }

    // -----------------------------------------------------------------
    // All-key polling mode
    // -----------------------------------------------------------------

    @Test
    public void allKey_mode_sets_forward_to_positive_one_when_pressed() {
        MovementInputState s = new MovementInputState();
        KeyPollingLogic.apply(s, keys(false, false, true, false, false, false), true);
        assertEquals(1.0f, s.moveForward, 0.0001f);
    }

    @Test
    public void allKey_mode_sets_forward_to_negative_one_when_back_pressed() {
        MovementInputState s = new MovementInputState();
        KeyPollingLogic.apply(s, keys(false, false, false, true, false, false), true);
        assertEquals(-1.0f, s.moveForward, 0.0001f);
    }

    @Test
    public void allKey_mode_back_does_not_overwrite_forward() {
        MovementInputState s = new MovementInputState();
        s.moveForward = 1.0f; // already moving forward
        KeyPollingLogic.apply(s, keys(false, false, false, true, false, false), true);
        // back-poll guard: only set to -1 if moveForward >= 0; here it's +1
        assertEquals("forward should be preserved", 1.0f, s.moveForward, 0.0001f);
    }

    @Test
    public void allKey_mode_sets_strafe_negative_for_left() {
        MovementInputState s = new MovementInputState();
        KeyPollingLogic.apply(s, keys(false, false, false, false, true, false), true);
        assertEquals(-1.0f, s.moveStrafe, 0.0001f);
    }

    @Test
    public void allKey_mode_sets_strafe_positive_for_right() {
        MovementInputState s = new MovementInputState();
        KeyPollingLogic.apply(s, keys(false, false, false, false, false, true), true);
        assertEquals(1.0f, s.moveStrafe, 0.0001f);
    }

    @Test
    public void allKey_mode_right_does_not_overwrite_left() {
        MovementInputState s = new MovementInputState();
        s.moveStrafe = -1.0f; // already strafing left
        KeyPollingLogic.apply(s, keys(false, false, false, false, false, true), true);
        // right-poll guard: only set to +1 if moveStrafe <= 0; here it's -1
        assertEquals("left strafe should be preserved", -1.0f, s.moveStrafe, 0.0001f);
    }

    // -----------------------------------------------------------------
    // Multi-key combination (Shift + Space — the classic IME case)
    // -----------------------------------------------------------------

    @Test
    public void multiKey_combo_forces_both_sneak_and_jump() {
        MovementInputState s = new MovementInputState();
        KeyPollingLogic.apply(s, keys(true, true, false, false, false, false), true);
        assertTrue(s.jump);
        assertTrue(s.sneak);
    }

    @Test
    public void multiKey_combo_does_not_fire_when_only_one_pressed() {
        MovementInputState s = new MovementInputState();
        KeyPollingLogic.apply(s, keys(true, false, false, false, false, false), true);
        assertTrue(s.jump);
        assertFalse("sneak should NOT be forced by combo (only jump was pressed)", s.sneak);
    }

    @Test
    public void multiKey_combo_no_duplicate_change_when_already_set() {
        MovementInputState s = new MovementInputState();
        s.jump = true;
        s.sneak = true;
        boolean changed = KeyPollingLogic.apply(s, keys(true, true, false, false, false, false), true);
        // Combo guard sets sneak/jump, but they're already true, so combo path
        // doesn't report a change. However, pollJump/pollSneak also don't
        // change anything (already true). Overall: no change.
        assertFalse("already in target state, no change expected", changed);
    }

    // -----------------------------------------------------------------
    // Edge cases
    // -----------------------------------------------------------------

    @Test(expected = NullPointerException.class)
    public void apply_throws_NPE_on_null_state() {
        KeyPollingLogic.apply(null, keys(false, false, false, false, false, false), false);
    }

    @Test(expected = NullPointerException.class)
    public void apply_throws_NPE_on_null_keys() {
        KeyPollingLogic.apply(new MovementInputState(), null, false);
    }

    @Test
    public void apply_returns_false_when_nothing_pressed() {
        MovementInputState s = new MovementInputState();
        boolean changed = KeyPollingLogic.apply(s, keys(false, false, false, false, false, false), true);
        assertFalse(changed);
    }

    @Test
    public void apply_is_idempotent_on_target_state() {
        MovementInputState s = new MovementInputState();
        s.jump = true;
        s.sneak = true;
        s.moveForward = 1.0f;
        s.moveStrafe = -1.0f;
        PhysicalKeyState k = keys(true, true, true, false, true, false);
        boolean changed1 = KeyPollingLogic.apply(s, k, true);
        MovementInputState snapshot = s.copy();
        boolean changed2 = KeyPollingLogic.apply(s, k, true);
        // Second application should not change anything.
        assertEquals(snapshot, s);
        // First application: combo guard sees sneak+jump already true → no change
        //   pollForward sees moveForward=1 (>0) → no change
        //   pollLeft sees moveStrafe=-1 (<0) → no change
        //   pollSneak/pollJump already true → no change
        assertFalse("first apply should be no-op on target state", changed1);
        assertFalse(changed2);
    }

    @Test
    public void apply_handles_random_input_without_crash() {
        Random rnd = new Random(42);
        for (int i = 0; i < 1000; i++) {
            MovementInputState s = new MovementInputState();
            s.moveForward = rnd.nextFloat() * 2 - 1;
            s.moveStrafe  = rnd.nextFloat() * 2 - 1;
            s.jump  = rnd.nextBoolean();
            s.sneak = rnd.nextBoolean();
            PhysicalKeyState k = keys(rnd.nextBoolean(), rnd.nextBoolean(), rnd.nextBoolean(),
                                       rnd.nextBoolean(), rnd.nextBoolean(), rnd.nextBoolean());
            KeyPollingLogic.apply(s, k, true);
        }
    }
}
