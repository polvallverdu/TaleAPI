package dev.polv.taleapi.permission;

import java.util.Objects;

/**
 * Represents a single permission node with state, payload, and context.
 * <p>
 * Unlike traditional permission systems where a permission is just a string
 * mapped to a boolean, a PermissionNode is a rich state object containing:
 * <ul>
 *   <li><b>Key:</b> The permission path (e.g., "plots.limit")</li>
 *   <li><b>State:</b> A {@link Tristate} (ALLOW, DENY, UNDEFINED)</li>
 *   <li><b>Payload:</b> An arbitrary value (Integer, Double, String, etc.)</li>
 *   <li><b>Context:</b> Conditions for when this permission applies</li>
 * </ul>
 * </p>
 *
 * <h2>Solving Dynamic Values</h2>
 * <pre>{@code
 * // Old way: "plots.limit.5" - requires string parsing at runtime
 * // New way: key="plots.limit", payload=5
 *
 * PermissionNode node = PermissionNode.builder("plots.limit")
 *     .allow()
 *     .payload(5)
 *     .context(ContextSet.of(ContextKey.WORLD, "survival"))
 *     .build();
 * }</pre>
 *
 * @see PermissionResult
 * @see ContextSet
 */
public final class PermissionNode {

    private final String key;
    private final Tristate state;
    private final Object payload;
    private final ContextSet context;

    private PermissionNode(String key, Tristate state, Object payload, ContextSet context) {
        this.key = Objects.requireNonNull(key, "key");
        this.state = Objects.requireNonNull(state, "state");
        this.payload = payload;
        this.context = Objects.requireNonNull(context, "context");
    }

    /**
     * Creates a builder for a permission node with the given key.
     *
     * @param key the permission key (e.g., "plots.create")
     * @return a new Builder instance
     */
    public static Builder builder(String key) {
        return new Builder(key);
    }

    /**
     * Creates a simple ALLOW permission node with no payload.
     *
     * @param key the permission key
     * @return an ALLOW permission node
     */
    public static PermissionNode allow(String key) {
        return new PermissionNode(key, Tristate.ALLOW, null, ContextSet.EMPTY);
    }

    /**
     * Creates a simple DENY permission node with no payload.
     *
     * @param key the permission key
     * @return a DENY permission node
     */
    public static PermissionNode deny(String key) {
        return new PermissionNode(key, Tristate.DENY, null, ContextSet.EMPTY);
    }

    /**
     * Creates an ALLOW permission node with a payload.
     *
     * @param key     the permission key
     * @param payload the dynamic value
     * @return an ALLOW permission node with payload
     */
    public static PermissionNode allow(String key, Object payload) {
        return new PermissionNode(key, Tristate.ALLOW, payload, ContextSet.EMPTY);
    }

    /**
     * Creates a DENY permission node with a payload.
     *
     * @param key     the permission key
     * @param payload the dynamic value
     * @return a DENY permission node with payload
     */
    public static PermissionNode deny(String key, Object payload) {
        return new PermissionNode(key, Tristate.DENY, payload, ContextSet.EMPTY);
    }

    /**
     * Returns the permission key (path).
     *
     * @return the permission key
     */
    public String getKey() {
        return key;
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
     * Returns the payload value.
     *
     * @return the payload, or null if none
     */
    public Object getPayload() {
        return payload;
    }

    /**
     * Checks if this node has a payload.
     *
     * @return {@code true} if a payload is present
     */
    public boolean hasPayload() {
        return payload != null;
    }

    /**
     * Returns the context for this permission.
     *
     * @return the context set (never null, may be empty)
     */
    public ContextSet getContext() {
        return context;
    }

    /**
     * Checks if this node applies in the given context.
     *
     * @param currentContext the current context to check against
     * @return {@code true} if this node's context matches
     */
    public boolean appliesInContext(ContextSet currentContext) {
        return context.matches(currentContext);
    }

    /**
     * Converts this node to a PermissionResult.
     *
     * @return a PermissionResult with this node's state and payload
     */
    public PermissionResult toResult() {
        return PermissionResult.of(state, payload);
    }

    /**
     * Creates a copy of this node with a different state.
     *
     * @param newState the new state
     * @return a new PermissionNode with the updated state
     */
    public PermissionNode withState(Tristate newState) {
        return new PermissionNode(key, newState, payload, context);
    }

    /**
     * Creates a copy of this node with a different payload.
     *
     * @param newPayload the new payload
     * @return a new PermissionNode with the updated payload
     */
    public PermissionNode withPayload(Object newPayload) {
        return new PermissionNode(key, state, newPayload, context);
    }

    /**
     * Creates a copy of this node with a different context.
     *
     * @param newContext the new context
     * @return a new PermissionNode with the updated context
     */
    public PermissionNode withContext(ContextSet newContext) {
        return new PermissionNode(key, state, payload, newContext);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PermissionNode that)) return false;
        return key.equals(that.key) &&
               state == that.state &&
               Objects.equals(payload, that.payload) &&
               context.equals(that.context);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, state, payload, context);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("PermissionNode{");
        sb.append("key='").append(key).append("'");
        sb.append(", state=").append(state);
        if (payload != null) {
            sb.append(", payload=").append(payload);
        }
        if (!context.isEmpty()) {
            sb.append(", context=").append(context);
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Builder for creating PermissionNode instances.
     */
    public static final class Builder {
        private final String key;
        private Tristate state = Tristate.UNDEFINED;
        private Object payload = null;
        private ContextSet context = ContextSet.EMPTY;

        private Builder(String key) {
            this.key = Objects.requireNonNull(key, "key");
        }

        /**
         * Sets the permission state.
         *
         * @param state the tristate
         * @return this builder
         */
        public Builder state(Tristate state) {
            this.state = Objects.requireNonNull(state, "state");
            return this;
        }

        /**
         * Sets the state to ALLOW.
         *
         * @return this builder
         */
        public Builder allow() {
            this.state = Tristate.ALLOW;
            return this;
        }

        /**
         * Sets the state to DENY.
         *
         * @return this builder
         */
        public Builder deny() {
            this.state = Tristate.DENY;
            return this;
        }

        /**
         * Sets the payload value.
         *
         * @param payload the dynamic value
         * @return this builder
         */
        public Builder payload(Object payload) {
            this.payload = payload;
            return this;
        }

        /**
         * Sets the context for this permission.
         *
         * @param context the context set
         * @return this builder
         */
        public Builder context(ContextSet context) {
            this.context = Objects.requireNonNull(context, "context");
            return this;
        }

        /**
         * Builds the immutable PermissionNode.
         *
         * @return a new PermissionNode
         */
        public PermissionNode build() {
            return new PermissionNode(key, state, payload, context);
        }
    }
}

