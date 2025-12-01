package dev.polv.taleapi.event.player;

import dev.polv.taleapi.entity.TalePlayer;
import dev.polv.taleapi.event.Event;
import dev.polv.taleapi.permission.ContextSet;
import dev.polv.taleapi.permission.PermissionResult;

import java.util.Objects;

/**
 * Called after a permission provider calculates a result but before the game receives it.
 * <p>
 * This event allows plugins to temporarily override permissions without modifying
 * the underlying permission data. Common use cases include:
 * <ul>
 *   <li>Denying all commands during a minigame match</li>
 *   <li>Granting temporary permissions during an event</li>
 *   <li>Implementing custom permission logic based on game state</li>
 * </ul>
 * </p>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Deny all teleport commands during a match
 * PermissionCheckCallback.EVENT.register((player, key, context, result) -> {
 *     if (matchManager.isInMatch(player) && key.startsWith("cmd.teleport")) {
 *         return CheckResult.deny(); // Override to DENY
 *     }
 *     return CheckResult.unmodified(); // Keep original result
 * });
 *
 * // Grant temporary fly permission during an event
 * PermissionCheckCallback.EVENT.register((player, key, context, result) -> {
 *     if (eventManager.hasTemporaryFlight(player) && key.equals("ability.fly")) {
 *         return CheckResult.allow();
 *     }
 *     return CheckResult.unmodified();
 * });
 * }</pre>
 *
 * <h2>Priority</h2>
 * <p>
 * Listeners with higher {@link dev.polv.taleapi.event.EventPriority} run first
 * and can override the result for subsequent listeners. Use HIGHEST priority
 * for security-critical overrides.
 * </p>
 */
@FunctionalInterface
public interface PermissionCheckCallback {

    /**
     * The event instance. Use this to register listeners.
     */
    Event<PermissionCheckCallback> EVENT = Event.create(
        callbacks -> (player, key, context, result) -> {
            CheckResult current = CheckResult.of(result);
            for (PermissionCheckCallback callback : callbacks) {
                CheckResult callbackResult = callback.onPermissionCheck(player, key, context, current.getResult());
                if (callbackResult.isModified()) {
                    current = callbackResult;
                }
            }
            return current;
        },
        (player, key, context, result) -> CheckResult.of(result) // Empty invoker
    );

    /**
     * Called when a permission is being checked.
     *
     * @param player  the player whose permission is being checked
     * @param key     the permission key being queried
     * @param context the context of the check
     * @param result  the current permission result (from provider or previous listeners)
     * @return a CheckResult indicating whether to modify the result
     */
    CheckResult onPermissionCheck(TalePlayer player, String key, ContextSet context, PermissionResult result);

    /**
     * Represents the result of a permission check callback.
     * <p>
     * Use the static factory methods to create instances:
     * <ul>
     *   <li>{@link #unmodified()} - Keep the original result</li>
     *   <li>{@link #allow()} - Override to ALLOW</li>
     *   <li>{@link #deny()} - Override to DENY</li>
     *   <li>{@link #override(PermissionResult)} - Override with a custom result</li>
     * </ul>
     * </p>
     */
    final class CheckResult {
        private static final CheckResult UNMODIFIED = new CheckResult(null, false);

        private final PermissionResult result;
        private final boolean modified;

        private CheckResult(PermissionResult result, boolean modified) {
            this.result = result;
            this.modified = modified;
        }

        /**
         * Creates a result that keeps the original permission result.
         *
         * @return an unmodified check result
         */
        public static CheckResult unmodified() {
            return UNMODIFIED;
        }

        /**
         * Creates a result that wraps an existing permission result without modification.
         *
         * @param result the permission result to wrap
         * @return a check result containing the given permission result
         */
        public static CheckResult of(PermissionResult result) {
            return new CheckResult(result, false);
        }

        /**
         * Creates a result that overrides with ALLOW.
         *
         * @return an allow check result
         */
        public static CheckResult allow() {
            return new CheckResult(PermissionResult.ALLOWED, true);
        }

        /**
         * Creates a result that overrides with ALLOW and a payload.
         *
         * @param payload the dynamic value
         * @return an allow check result with payload
         */
        public static CheckResult allow(Object payload) {
            return new CheckResult(PermissionResult.allow(payload), true);
        }

        /**
         * Creates a result that overrides with DENY.
         *
         * @return a deny check result
         */
        public static CheckResult deny() {
            return new CheckResult(PermissionResult.DENIED, true);
        }

        /**
         * Creates a result that overrides with DENY and a payload.
         *
         * @param payload the dynamic value
         * @return a deny check result with payload
         */
        public static CheckResult deny(Object payload) {
            return new CheckResult(PermissionResult.deny(payload), true);
        }

        /**
         * Creates a result that overrides with a custom permission result.
         *
         * @param result the permission result to use
         * @return a modified check result
         */
        public static CheckResult override(PermissionResult result) {
            return new CheckResult(Objects.requireNonNull(result, "result"), true);
        }

        /**
         * Returns whether this result modifies the permission.
         *
         * @return {@code true} if the result was modified
         */
        public boolean isModified() {
            return modified;
        }

        /**
         * Returns the permission result.
         *
         * @return the permission result (may be null if unmodified and no prior result)
         */
        public PermissionResult getResult() {
            return result;
        }
    }
}

