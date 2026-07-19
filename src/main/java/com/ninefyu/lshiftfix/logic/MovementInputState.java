package com.ninefyu.lshiftfix.logic;

/**
 * Pure movement-input state holder, decoupled from Minecraft classes.
 *
 * <p>Mirrors the four public fields of {@code net.minecraft.util.MovementInput}
 * from 1.8.9:</p>
 * <ul>
 *   <li>{@code float moveForward} — +1 = forward, -1 = backward</li>
 *   <li>{@code float moveStrafe} — +1 = right, -1 = left</li>
 *   <li>{@code boolean jump}</li>
 *   <li>{@code boolean sneak}</li>
 * </ul>
 *
 * <p>Decoupled so it can be unit-tested without Forge/Minecraft on the
 * classpath.</p>
 */
public final class MovementInputState {

    public float moveForward = 0.0f;
    public float moveStrafe  = 0.0f;
    public boolean jump      = false;
    public boolean sneak     = false;

    public MovementInputState copy() {
        MovementInputState s = new MovementInputState();
        s.moveForward = moveForward;
        s.moveStrafe  = moveStrafe;
        s.jump        = jump;
        s.sneak       = sneak;
        return s;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MovementInputState)) return false;
        MovementInputState s = (MovementInputState) o;
        return Float.compare(s.moveForward, moveForward) == 0
            && Float.compare(s.moveStrafe,  moveStrafe)  == 0
            && jump == s.jump
            && sneak == s.sneak;
    }

    @Override
    public int hashCode() {
        int r = Float.floatToIntBits(moveForward);
        r = 31 * r + Float.floatToIntBits(moveStrafe);
        r = 31 * r + (jump ? 1 : 0);
        r = 31 * r + (sneak ? 1 : 0);
        return r;
    }

    @Override
    public String toString() {
        return "MovementInputState{forward=" + moveForward
            + ", strafe=" + moveStrafe
            + ", jump=" + jump
            + ", sneak=" + sneak + "}";
    }
}
