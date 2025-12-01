package dev.polv.taleapi.permission;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * The result of a permission query - not just a boolean!
 * <p>
 * PermissionResult contains the {@link Tristate} and an optional payload value.
 * This solves the "magic number" problem in traditional permission systems where
 * limits are encoded in permission strings (e.g., "plots.limit.5").
 * </p>
 *
 * <h2>Dynamic Values</h2>
 * <pre>{@code
 * // Old way (bad): "plots.limit.5" - requires string parsing
 * // New way (good): Permission key is "plots.limit", payload is 5
 *
 * PermissionResult result = permService.query(user, "plots.limit");
 * int maxPlots = result.asInt(1); // Returns 1 if not set
 * }</pre>
 *
 * @see PermissionService
 * @see Tristate
 */
public final class PermissionResult {

    /**
     * Result indicating the permission was allowed with no payload.
     */
    public static final PermissionResult ALLOWED = new PermissionResult(Tristate.ALLOW, null);

    /**
     * Result indicating the permission was denied with no payload.
     */
    public static final PermissionResult DENIED = new PermissionResult(Tristate.DENY, null);

    /**
     * Result indicating the permission was not defined (undefined).
     */
    public static final PermissionResult UNDEFINED = new PermissionResult(Tristate.UNDEFINED, null);

    private final Tristate state;
    private final Object payload;

    private PermissionResult(Tristate state, Object payload) {
        this.state = Objects.requireNonNull(state, "state");
        this.payload = payload;
    }

    /**
     * Creates a result with the given state and no payload.
     *
     * @param state the permission state
     * @return a PermissionResult with the given state
     */
    public static PermissionResult of(Tristate state) {
        return switch (state) {
            case ALLOW -> ALLOWED;
            case DENY -> DENIED;
            case UNDEFINED -> UNDEFINED;
        };
    }

    /**
     * Creates a result with the given state and payload.
     *
     * @param state   the permission state
     * @param payload the dynamic value payload
     * @return a new PermissionResult
     */
    public static PermissionResult of(Tristate state, Object payload) {
        if (payload == null) {
            return of(state);
        }
        return new PermissionResult(state, payload);
    }

    /**
     * Creates an ALLOW result with the given payload.
     *
     * @param payload the dynamic value
     * @return a PermissionResult with ALLOW state and the payload
     */
    public static PermissionResult allow(Object payload) {
        return new PermissionResult(Tristate.ALLOW, payload);
    }

    /**
     * Creates a DENY result with the given payload.
     *
     * @param payload the dynamic value
     * @return a PermissionResult with DENY state and the payload
     */
    public static PermissionResult deny(Object payload) {
        return new PermissionResult(Tristate.DENY, payload);
    }

    /**
     * Returns the permission state.
     *
     * @return the tristate (ALLOW, DENY, or UNDEFINED)
     */
    public Tristate getState() {
        return state;
    }

    /**
     * Checks if the permission is allowed.
     *
     * @return {@code true} if state is ALLOW
     */
    public boolean isAllowed() {
        return state == Tristate.ALLOW;
    }

    /**
     * Checks if the permission is denied.
     *
     * @return {@code true} if state is DENY
     */
    public boolean isDenied() {
        return state == Tristate.DENY;
    }

    /**
     * Checks if the permission is undefined.
     *
     * @return {@code true} if state is UNDEFINED
     */
    public boolean isUndefined() {
        return state == Tristate.UNDEFINED;
    }

    /**
     * Returns whether this result has a payload value.
     *
     * @return {@code true} if a payload is present
     */
    public boolean hasPayload() {
        return payload != null;
    }

    /**
     * Returns the raw payload value.
     *
     * @return an Optional containing the payload, or empty
     */
    public Optional<Object> getPayload() {
        return Optional.ofNullable(payload);
    }

    /**
     * Returns the payload as an integer.
     *
     * @param defaultValue the value to return if no payload or wrong type
     * @return the payload as int, or defaultValue
     */
    public int asInt(int defaultValue) {
        if (payload instanceof Number num) {
            return num.intValue();
        }
        if (payload instanceof String str) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    /**
     * Returns the payload as an optional integer.
     *
     * @return OptionalInt with the value, or empty
     */
    public OptionalInt asOptionalInt() {
        if (payload instanceof Number num) {
            return OptionalInt.of(num.intValue());
        }
        if (payload instanceof String str) {
            try {
                return OptionalInt.of(Integer.parseInt(str));
            } catch (NumberFormatException ignored) {
            }
        }
        return OptionalInt.empty();
    }

    /**
     * Returns the payload as a long.
     *
     * @param defaultValue the value to return if no payload or wrong type
     * @return the payload as long, or defaultValue
     */
    public long asLong(long defaultValue) {
        if (payload instanceof Number num) {
            return num.longValue();
        }
        if (payload instanceof String str) {
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    /**
     * Returns the payload as an optional long.
     *
     * @return OptionalLong with the value, or empty
     */
    public OptionalLong asOptionalLong() {
        if (payload instanceof Number num) {
            return OptionalLong.of(num.longValue());
        }
        if (payload instanceof String str) {
            try {
                return OptionalLong.of(Long.parseLong(str));
            } catch (NumberFormatException ignored) {
            }
        }
        return OptionalLong.empty();
    }

    /**
     * Returns the payload as a double.
     *
     * @param defaultValue the value to return if no payload or wrong type
     * @return the payload as double, or defaultValue
     */
    public double asDouble(double defaultValue) {
        if (payload instanceof Number num) {
            return num.doubleValue();
        }
        if (payload instanceof String str) {
            try {
                return Double.parseDouble(str);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    /**
     * Returns the payload as an optional double.
     *
     * @return OptionalDouble with the value, or empty
     */
    public OptionalDouble asOptionalDouble() {
        if (payload instanceof Number num) {
            return OptionalDouble.of(num.doubleValue());
        }
        if (payload instanceof String str) {
            try {
                return OptionalDouble.of(Double.parseDouble(str));
            } catch (NumberFormatException ignored) {
            }
        }
        return OptionalDouble.empty();
    }

    /**
     * Returns the payload as a string.
     *
     * @return the payload's string representation, or null if no payload
     */
    public String asString() {
        return payload != null ? payload.toString() : null;
    }

    /**
     * Returns the payload as a string with a default.
     *
     * @param defaultValue the value to return if no payload
     * @return the payload string, or defaultValue
     */
    public String asString(String defaultValue) {
        return payload != null ? payload.toString() : defaultValue;
    }

    /**
     * Returns the payload as a boolean.
     *
     * @param defaultValue the value to return if no payload or wrong type
     * @return the payload as boolean, or defaultValue
     */
    public boolean asBoolean(boolean defaultValue) {
        if (payload instanceof Boolean bool) {
            return bool;
        }
        if (payload instanceof String str) {
            return Boolean.parseBoolean(str);
        }
        return defaultValue;
    }

    /**
     * Returns the payload cast to the specified type.
     *
     * @param type the expected class type
     * @param <T>  the type parameter
     * @return an Optional containing the typed payload, or empty if wrong type
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> as(Class<T> type) {
        if (type.isInstance(payload)) {
            return Optional.of((T) payload);
        }
        return Optional.empty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PermissionResult that)) return false;
        return state == that.state && Objects.equals(payload, that.payload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(state, payload);
    }

    @Override
    public String toString() {
        if (payload == null) {
            return "PermissionResult{" + state + "}";
        }
        return "PermissionResult{" + state + ", payload=" + payload + "}";
    }
}

