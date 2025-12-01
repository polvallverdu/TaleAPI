package dev.polv.taleapi.permission;

import java.util.Objects;

/**
 * Represents a context key used in permission context matching.
 * <p>
 * Context keys are typed identifiers for context values. Common built-in keys include:
 * <ul>
 *   <li>{@code world} - The world name (e.g., "nether", "overworld")</li>
 *   <li>{@code server} - The server name in a network</li>
 *   <li>{@code gamemode} - The player's gamemode</li>
 * </ul>
 * </p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * ContextKey<String> WORLD = ContextKey.of("world", String.class);
 * ContextKey<String> GAMEMODE = ContextKey.of("gamemode", String.class);
 * }</pre>
 *
 * @param <T> the type of value this key holds
 */
public final class ContextKey<T> {

    // Built-in context keys
    public static final ContextKey<String> WORLD = of("world", String.class);
    public static final ContextKey<String> SERVER = of("server", String.class);
    public static final ContextKey<String> GAMEMODE = of("gamemode", String.class);

    private final String key;
    private final Class<T> type;

    private ContextKey(String key, Class<T> type) {
        this.key = Objects.requireNonNull(key, "key");
        this.type = Objects.requireNonNull(type, "type");
    }

    /**
     * Creates a new context key.
     *
     * @param key  the string identifier for this key
     * @param type the class type of values for this key
     * @param <T>  the value type
     * @return a new ContextKey instance
     */
    public static <T> ContextKey<T> of(String key, Class<T> type) {
        return new ContextKey<>(key, type);
    }

    /**
     * Returns the string identifier for this key.
     *
     * @return the key name
     */
    public String key() {
        return key;
    }

    /**
     * Returns the type of values this key holds.
     *
     * @return the value class type
     */
    public Class<T> type() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContextKey<?> that)) return false;
        return key.equals(that.key) && type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, type);
    }

    @Override
    public String toString() {
        return "ContextKey{" + key + ", type=" + type.getSimpleName() + "}";
    }
}

