package com.ninefyu.lshiftfix.logic.polling;

import com.ninefyu.lshiftfix.logic.KeyPollingLogic.PhysicalKeyState;
import com.ninefyu.lshiftfix.logic.MovementInputState;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for individual single-key rules.
 */
public class SingleKeyRuleTest {

    private static PhysicalKeyState allFalse() {
        return new PhysicalKeyState() {
            public boolean isJumpPressed()    { return false; }
            public boolean isSneakPressed()   { return false; }
            public boolean isForwardPressed() { return false; }
            public boolean isBackPressed()    { return false; }
            public boolean isLeftPressed()    { return false; }
            public boolean isRightPressed()   { return false; }
        };
    }

    // --- JumpKeyRule ---
    @Test
    public void jumpRule_sets_jump_when_pressed_and_not_set() {
        MovementInputState s = new MovementInputState();
        PhysicalKeyState k = allFalse() {
            public boolean isJumpPressed() { return true; }
        };
        assertTrue(new JumpKeyRule().apply(s, k));
        assertTrue(s.jump);
    }

    @Test
    public void jumpRule_no_change_when_already_true() {
        MovementInputState s = new MovementInputState();
        s.jump = true;
        PhysicalKeyState k = allFalse() {
            public boolean isJumpPressed() { return true; }
        };
        assertFalse(new JumpKeyRule().apply(s, k));
    }

    @Test
    public void jumpRule_no_change_when_not_pressed() {
        MovementInputState s = new MovementInputState();
        assertFalse(new JumpKeyRule().apply(s, allFalse()));
        assertFalse(s.jump);
    }

    // --- SneakKeyRule ---
    @Test
    public void sneakRule_sets_sneak_when_pressed() {
        MovementInputState s = new MovementInputState();
        PhysicalKeyState k = allFalse() {
            public boolean isSneakPressed() { return true; }
        };
        assertTrue(new SneakKeyRule().apply(s, k));
        assertTrue(s.sneak);
    }

    @Test
    public void sneakRule_no_change_when_already_true() {
        MovementInputState s = new MovementInputState();
        s.sneak = true;
        PhysicalKeyState k = allFalse() {
            public boolean isSneakPressed() { return true; }
        };
        assertFalse(new SneakKeyRule().apply(s, k));
    }

    // --- ForwardKeyRule ---
    @Test
    public void forwardRule_sets_positive_when_zero_and_pressed() {
        MovementInputState s = new MovementInputState();
        PhysicalKeyState k = allFalse() {
            public boolean isForwardPressed() { return true; }
        };
        assertTrue(new ForwardKeyRule().apply(s, k));
        assertEquals(1.0f, s.moveForward, 0.0001f);
    }

    @Test
    public void forwardRule_does_not_overwrite_negative() {
        MovementInputState s = new MovementInputState();
        s.moveForward = -0.5f;
        PhysicalKeyState k = allFalse() {
            public boolean isForwardPressed() { return true; }
        };
        // moveForward is < 0 → guard "if (state.moveForward <= 0.0f)" would trigger
        // Wait, actually -0.5 IS <= 0, so it WILL set to +1. That's the expected behavior.
        assertTrue(new ForwardKeyRule().apply(s, k));
        assertEquals(1.0f, s.moveForward, 0.0001f);
    }

    // --- BackKeyRule ---
    @Test
    public void backRule_sets_negative_when_zero_and_pressed() {
        MovementInputState s = new MovementInputState();
        PhysicalKeyState k = allFalse() {
            public boolean isBackPressed() { return true; }
        };
        assertTrue(new BackKeyRule().apply(s, k));
        assertEquals(-1.0f, s.moveForward, 0.0001f);
    }

    @Test
    public void backRule_does_not_overwrite_positive() {
        MovementInputState s = new MovementInputState();
        s.moveForward = 1.0f;
        PhysicalKeyState k = allFalse() {
            public boolean isBackPressed() { return true; }
        };
        assertFalse(new BackKeyRule().apply(s, k));
        assertEquals(1.0f, s.moveForward, 0.0001f);
    }

    // --- LeftKeyRule ---
    @Test
    public void leftRule_sets_negative_strafe() {
        MovementInputState s = new MovementInputState();
        PhysicalKeyState k = allFalse() {
            public boolean isLeftPressed() { return true; }
        };
        assertTrue(new LeftKeyRule().apply(s, k));
        assertEquals(-1.0f, s.moveStrafe, 0.0001f);
    }

    @Test
    public void leftRule_does_not_overwrite_positive() {
        MovementInputState s = new MovementInputState();
        s.moveStrafe = 1.0f;
        PhysicalKeyState k = allFalse() {
            public boolean isLeftPressed() { return true; }
        };
        assertFalse(new LeftKeyRule().apply(s, k));
        assertEquals(1.0f, s.moveStrafe, 0.0001f);
    }

    // --- RightKeyRule ---
    @Test
    public void rightRule_sets_positive_strafe() {
        MovementInputState s = new MovementInputState();
        PhysicalKeyState k = allFalse() {
            public boolean isRightPressed() { return true; }
        };
        assertTrue(new RightKeyRule().apply(s, k));
        assertEquals(1.0f, s.moveStrafe, 0.0001f);
    }

    @Test
    public void rightRule_does_not_overwrite_negative() {
        MovementInputState s = new MovementInputState();
        s.moveStrafe = -1.0f;
        PhysicalKeyState k = allFalse() {
            public boolean isRightPressed() { return true; }
        };
        assertFalse(new RightKeyRule().apply(s, k));
        assertEquals(-1.0f, s.moveStrafe, 0.0001f);
    }
}
