package dev.polv.taleapi.permission;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * An immutable set of context key-value pairs.
 * <p>
 * ContextSet defines the conditions under which a permission applies.
 * For example, a permission might only apply in the "nether" world or
 * when the player is in "creative" gamemode.
 * </p>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * ContextSet context = ContextSet.builder()
 *     .add(ContextKey.WORLD, "nether")
 *     .add(ContextKey.GAMEMODE, "creative")
 *     .build();
 *
 * // Check if context matches
 * if (context.matches(playerContext)) {
 *     // Permission applies
 * }
 * }</pre>
 *
 * @see ContextKey
 */
public final class ContextSet {

    /**
     * An empty context set that matches everything.
     */
    public static final ContextSet EMPTY = new ContextSet(Map.of());

    private final Map<String, Object> contexts;

    private ContextSet(Map<String, Object> contexts) {
        this.contexts = Map.copyOf(contexts);
    }

    /**
     * Creates a new builder for constructing a ContextSet.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a ContextSet with a single context value.
     *
     * @param key   the context key
     * @param value the context value
     * @param <T>   the value type
     * @return a new ContextSet with the single context
     */
    public static <T> ContextSet of(ContextKey<T> key, T value) {
        return builder().add(key, value).build();
    }

    /**
     * Creates a ContextSet with two context values.
     *
     * @param key1   the first context key
     * @param value1 the first context value
     * @param key2   the second context key
     * @param value2 the second context value
     * @param <T1>   the first value type
     * @param <T2>   the second value type
     * @return a new ContextSet with both contexts
     */
    public static <T1, T2> ContextSet of(ContextKey<T1> key1, T1 value1,
                                          ContextKey<T2> key2, T2 value2) {
        return builder().add(key1, value1).add(key2, value2).build();
    }

    /**
     * Gets a context value by its key.
     *
     * @param key the context key
     * @param <T> the value type
     * @return an Optional containing the value, or empty if not present
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(ContextKey<T> key) {
        Object value = contexts.get(key.key());
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of((T) value);
    }

    /**
     * Checks if this context set contains the specified key.
     *
     * @param key the context key to check
     * @return {@code true} if the key is present
     */
    public boolean contains(ContextKey<?> key) {
        return contexts.containsKey(key.key());
    }

    /**
     * Checks if this context set contains a specific key-value pair.
     *
     * @param key   the context key
     * @param value the expected value
     * @param <T>   the value type
     * @return {@code true} if the key exists with the specified value
     */
    public <T> boolean contains(ContextKey<T> key, T value) {
        return Objects.equals(contexts.get(key.key()), value);
    }

    /**
     * Checks if this context set matches another context set.
     * <p>
     * A context set "matches" another if all keys in this set are present
     * in the other set with matching values. An empty context set matches
     * everything.
     * </p>
     *
     * @param other the context set to match against (e.g., player's current context)
     * @return {@code true} if all contexts in this set match
     */
    public boolean matches(ContextSet other) {
        if (this.isEmpty()) {
            return true; // Empty context matches everything
        }
        for (Map.Entry<String, Object> entry : contexts.entrySet()) {
            Object otherValue = other.contexts.get(entry.getKey());
            if (!Objects.equals(entry.getValue(), otherValue)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns whether this context set is empty.
     *
     * @return {@code true} if no contexts are defined
     */
    public boolean isEmpty() {
        return contexts.isEmpty();
    }

    /**
     * Returns the number of context entries.
     *
     * @return the size of this context set
     */
    public int size() {
        return contexts.size();
    }

    /**
     * Returns all context keys in this set.
     *
     * @return an unmodifiable set of key names
     */
    public Set<String> keys() {
        return contexts.keySet();
    }

    /**
     * Returns an unmodifiable view of all contexts.
     *
     * @return the context map
     */
    public Map<String, Object> asMap() {
        return contexts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContextSet that)) return false;
        return contexts.equals(that.contexts);
    }

    @Override
    public int hashCode() {
        return contexts.hashCode();
    }

    @Override
    public String toString() {
        return "ContextSet" + contexts;
    }

    /**
     * Builder for creating ContextSet instances.
     */
    public static final class Builder {
        private final Map<String, Object> contexts = new HashMap<>();

        private Builder() {}

        /**
         * Adds a context key-value pair.
         *
         * @param key   the context key
         * @param value the context value
         * @param <T>   the value type
         * @return this builder for chaining
         */
        public <T> Builder add(ContextKey<T> key, T value) {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(value, "value");
            contexts.put(key.key(), value);
            return this;
        }

        /**
         * Adds a raw string key-value pair.
         * <p>
         * Prefer using typed {@link ContextKey} when possible.
         * </p>
         *
         * @param key   the context key name
         * @param value the context value
         * @return this builder for chaining
         */
        public Builder add(String key, String value) {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(value, "value");
            contexts.put(key, value);
            return this;
        }

        /**
         * Adds all contexts from another ContextSet.
         *
         * @param other the context set to copy from
         * @return this builder for chaining
         */
        public Builder addAll(ContextSet other) {
            contexts.putAll(other.contexts);
            return this;
        }

        /**
         * Builds the immutable ContextSet.
         *
         * @return a new ContextSet
         */
        public ContextSet build() {
            if (contexts.isEmpty()) {
                return EMPTY;
            }
            return new ContextSet(contexts);
        }
    }
}

