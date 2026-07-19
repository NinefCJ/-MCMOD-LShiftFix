package com.ninefyu.lshiftfix.logic;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link MovementInputState} — value-object semantics
 * (copy / equals / hashCode / toString).
 */
public class MovementInputStateTest {

    @Test
    public void default_state_is_all_zero_false() {
        MovementInputState s = new MovementInputState();
        assertEquals(0.0f, s.moveForward, 0.0001f);
        assertEquals(0.0f, s.moveStrafe, 0.0001f);
        assertFalse(s.jump);
        assertFalse(s.sneak);
    }

    // ---- copy() ----

    @Test
    public void copy_produces_equal_but_distinct_instance() {
        MovementInputState original = new MovementInputState();
        original.moveForward = 1.0f;
        original.moveStrafe  = -1.0f;
        original.jump  = true;
        original.sneak = false;

        MovementInputState copy = original.copy();

        assertNotSame(original, copy);
        assertEquals(original, copy);
        assertEquals(original.hashCode(), copy.hashCode());
    }

    @Test
    public void copy_is_independent_of_original() {
        MovementInputState original = new MovementInputState();
        original.jump = true;

        MovementInputState copy = original.copy();
        copy.jump = false;

        assertTrue("mutating copy should not affect original", original.jump);
        assertFalse(copy.jump);
    }

    // ---- equals() ----

    @Test
    public void equals_reflexive() {
        MovementInputState s = new MovementInputState();
        assertEquals(s, s);
    }

    @Test
    public void equals_symmetric() {
        MovementInputState a = newState(1.0f, -1.0f, true, false);
        MovementInputState b = newState(1.0f, -1.0f, true, false);
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
    }

    @Test
    public void not_equal_when_forward_differs() {
        MovementInputState a = newState(1.0f, 0.0f, false, false);
        MovementInputState b = newState(0.0f, 0.0f, false, false);
        assertFalse(a.equals(b));
    }

    @Test
    public void not_equal_when_strafe_differs() {
        MovementInputState a = newState(0.0f, 1.0f, false, false);
        MovementInputState b = newState(0.0f, -1.0f, false, false);
        assertFalse(a.equals(b));
    }

    @Test
    public void not_equal_when_jump_differs() {
        MovementInputState a = newState(0.0f, 0.0f, true, false);
        MovementInputState b = newState(0.0f, 0.0f, false, false);
        assertFalse(a.equals(b));
    }

    @Test
    public void not_equal_when_sneak_differs() {
        MovementInputState a = newState(0.0f, 0.0f, false, true);
        MovementInputState b = newState(0.0f, 0.0f, false, false);
        assertFalse(a.equals(b));
    }

    @Test
    public void not_equal_to_null() {
        assertFalse(new MovementInputState().equals(null));
    }

    @Test
    public void not_equal_to_other_type() {
        assertFalse(new MovementInputState().equals("not a state"));
    }

    // ---- hashCode() ----

    @Test
    public void equal_objects_have_same_hashCode() {
        MovementInputState a = newState(1.0f, -1.0f, true, true);
        MovementInputState b = newState(1.0f, -1.0f, true, true);
        assertEquals(a.hashCode(), b.hashCode());
    }

    // ---- toString() ----

    @Test
    public void toString_contains_all_fields() {
        MovementInputState s = newState(1.0f, -1.0f, true, false);
        String str = s.toString();
        assertTrue("should contain forward", str.contains("forward=1.0"));
        assertTrue("should contain strafe",  str.contains("strafe=-1.0"));
        assertTrue("should contain jump",    str.contains("jump=true"));
        assertTrue("should contain sneak",   str.contains("sneak=false"));
    }

    // ---- helpers ----

    private static MovementInputState newState(float fwd, float strafe, boolean jump, boolean sneak) {
        MovementInputState s = new MovementInputState();
        s.moveForward = fwd;
        s.moveStrafe  = strafe;
        s.jump        = jump;
        s.sneak       = sneak;
        return s;
    }

    private static void assertNotSame(Object a, Object b) {
        assertFalse("expected distinct instances", a == b);
    }
}
