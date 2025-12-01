package dev.polv.taleapi.permission;

/**
 * Represents a tri-valued permission state.
 * <p>
 * Unlike a simple boolean, Tristate provides three distinct states:
 * <ul>
 *   <li>{@link #ALLOW} - Permission is explicitly granted</li>
 *   <li>{@link #DENY} - Permission is explicitly denied</li>
 *   <li>{@link #UNDEFINED} - No explicit permission set (inherit from parent/default)</li>
 * </ul>
 * </p>
 *
 * <h2>Why Tristate?</h2>
 * <p>
 * A binary true/false system cannot distinguish between "denied" and "not set".
 * This distinction is crucial for permission inheritance:
 * <ul>
 *   <li>UNDEFINED permissions inherit from parent groups</li>
 *   <li>DENY permissions block inheritance even if a parent allows it</li>
 * </ul>
 * </p>
 */
public enum Tristate {
    /**
     * Permission is explicitly granted.
     */
    ALLOW,

    /**
     * Permission is explicitly denied.
     */
    DENY,

    /**
     * No explicit permission set - inherit from parent or use default.
     */
    UNDEFINED;

    /**
     * Converts this tristate to a boolean value.
     *
     * @return {@code true} if ALLOW, {@code false} otherwise
     */
    public boolean asBoolean() {
        return this == ALLOW;
    }

    /**
     * Converts this tristate to a boolean with a default for UNDEFINED.
     *
     * @param defaultValue the value to return if this is UNDEFINED
     * @return {@code true} if ALLOW, {@code false} if DENY, or defaultValue if UNDEFINED
     */
    public boolean asBoolean(boolean defaultValue) {
        return switch (this) {
            case ALLOW -> true;
            case DENY -> false;
            case UNDEFINED -> defaultValue;
        };
    }

    /**
     * Creates a Tristate from a boolean value.
     *
     * @param value the boolean value
     * @return {@link #ALLOW} if true, {@link #DENY} if false
     */
    public static Tristate fromBoolean(boolean value) {
        return value ? ALLOW : DENY;
    }

    /**
     * Creates a Tristate from a nullable Boolean.
     *
     * @param value the Boolean value, or null
     * @return {@link #ALLOW} if true, {@link #DENY} if false, {@link #UNDEFINED} if null
     */
    public static Tristate fromNullableBoolean(Boolean value) {
        if (value == null) {
            return UNDEFINED;
        }
        return value ? ALLOW : DENY;
    }

    /**
     * Returns whether this state is defined (not UNDEFINED).
     *
     * @return {@code true} if this is ALLOW or DENY
     */
    public boolean isDefined() {
        return this != UNDEFINED;
    }
}

