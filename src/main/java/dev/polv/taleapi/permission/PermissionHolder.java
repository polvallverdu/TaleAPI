package dev.polv.taleapi.permission;

/**
 * Interface for entities that can have permissions.
 * <p>
 * This interface provides convenience methods for permission checking
 * on entities like players. It wraps calls to the {@link PermissionService}.
 * </p>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // On any PermissionHolder (like TalePlayer)
 * if (player.hasPermission("cmd.teleport")) {
 *     // Execute teleport
 * }
 *
 * // Get a dynamic limit
 * int maxHomes = player.getPermissionValue("homes.limit").asInt(3);
 * }</pre>
 *
 * @see PermissionService
 * @see PermissionResult
 */
public interface PermissionHolder {

    /**
     * Returns the unique identifier for this holder.
     * <p>
     * This is used to look up permissions in the permission system.
     * </p>
     *
     * @return the unique identifier
     */
    String getUniqueId();

    /**
     * Checks if this holder has the specified permission.
     * <p>
     * This is a convenience method equivalent to:
     * {@code PermissionService.getInstance().query(this, permission).isAllowed()}
     * </p>
     *
     * @param permission the permission key to check
     * @return {@code true} if the permission is ALLOW
     */
    default boolean hasPermission(String permission) {
        if (this instanceof dev.polv.taleapi.entity.TalePlayer player) {
            return PermissionService.getInstance().has(player, permission);
        }
        return false;
    }

    /**
     * Checks if this holder has the specified permission in a context.
     *
     * @param permission the permission key
     * @param context    the context to check in
     * @return {@code true} if the permission is ALLOW in the given context
     */
    default boolean hasPermission(String permission, ContextSet context) {
        if (this instanceof dev.polv.taleapi.entity.TalePlayer player) {
            return PermissionService.getInstance().has(player, permission, context);
        }
        return false;
    }

    /**
     * Queries a permission and returns the full result.
     * <p>
     * Use this when you need the payload value or tristate:
     * </p>
     * <pre>{@code
     * int maxPlots = holder.getPermissionValue("plots.limit").asInt(1);
     * }</pre>
     *
     * @param permission the permission key
     * @return the permission result
     */
    default PermissionResult getPermissionValue(String permission) {
        if (this instanceof dev.polv.taleapi.entity.TalePlayer player) {
            return PermissionService.getInstance().query(player, permission);
        }
        return PermissionResult.UNDEFINED;
    }

    /**
     * Queries a permission with context and returns the full result.
     *
     * @param permission the permission key
     * @param context    the context to check in
     * @return the permission result
     */
    default PermissionResult getPermissionValue(String permission, ContextSet context) {
        if (this instanceof dev.polv.taleapi.entity.TalePlayer player) {
            return PermissionService.getInstance().query(player, permission, context);
        }
        return PermissionResult.UNDEFINED;
    }
}

