package dev.polv.taleapi.permission;

import dev.polv.taleapi.entity.TalePlayer;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Service Provider Interface (SPI) for permission backends.
 * <p>
 * This interface defines the contract for permission providers. The game engine
 * provides a default JSON-based implementation, but third-party plugins (like
 * a custom plugin) can register their own high-performance SQL/Redis-backed provider.
 * </p>
 *
 * <h2>SPI Pattern</h2>
 * <p>
 * The Game Engine defines the Contract (Interface), but does not force a Strategy
 * (Implementation). This allows hot-swapping the permission backend without
 * changing any game code.
 * </p>
 *
 * <h2>Implementation Requirements</h2>
 * <ul>
 *   <li>Providers must be thread-safe</li>
 *   <li>Query operations should be fast (preferably cached)</li>
 *   <li>Load/save operations may be async</li>
 * </ul>
 *
 * @see PermissionService
 * @see PermissionResult
 */
public interface PermissionProvider {

    /**
     * Returns the unique identifier for this provider.
     * <p>
     * Examples: "default", "mysql", "redis"
     * </p>
     *
     * @return the provider identifier
     */
    String getId();

    /**
     * Returns a human-readable name for this provider.
     *
     * @return the provider name
     */
    String getName();

    /**
     * Queries a permission for a player.
     * <p>
     * This is the primary query method. It should return quickly,
     * preferably from a cached permission tree.
     * </p>
     *
     * @param player the player to check
     * @param key    the permission key
     * @return the permission result
     */
    PermissionResult query(TalePlayer player, String key);

    /**
     * Queries a permission for a player with context.
     * <p>
     * Context-aware permissions allow different results based on
     * world, server, gamemode, or custom conditions.
     * </p>
     *
     * @param player  the player to check
     * @param key     the permission key
     * @param context the context to check against
     * @return the permission result
     */
    PermissionResult query(TalePlayer player, String key, ContextSet context);

    /**
     * Gets the cached permission tree for a player.
     * <p>
     * This returns the player's flattened permission tree, which can
     * be queried directly for maximum performance.
     * </p>
     *
     * @param player the player
     * @return the player's permission tree, or null if not loaded
     */
    PermissionTree getPlayerTree(TalePlayer player);

    /**
     * Sets a permission for a player.
     *
     * @param player the player
     * @param node   the permission node to set
     * @return a future that completes when the permission is saved
     */
    CompletableFuture<Void> setPermission(TalePlayer player, PermissionNode node);

    /**
     * Removes a permission from a player.
     *
     * @param player the player
     * @param key    the permission key to remove
     * @return a future that completes when the permission is removed
     */
    CompletableFuture<Void> removePermission(TalePlayer player, String key);

    /**
     * Returns the permission nodes that should be synced to the client.
     * <p>
     * This is used for client-side UI prediction. Only permissions that
     * affect UI should be included (e.g., "ui.button.admin").
     * </p>
     *
     * @param player the player
     * @return a set of permission keys to sync
     */
    Set<String> getClientSyncedNodes(TalePlayer player);

    /**
     * Called when a player joins the server.
     * <p>
     * Implementations should load and cache the player's permissions here.
     * </p>
     *
     * @param player the player who joined
     * @return a future that completes when loading is done
     */
    CompletableFuture<Void> loadPlayer(TalePlayer player);

    /**
     * Called when a player leaves the server.
     * <p>
     * Implementations should clean up any cached data here.
     * </p>
     *
     * @param player the player who left
     */
    void unloadPlayer(TalePlayer player);

    /**
     * Invalidates the cached permissions for a player.
     * <p>
     * Called when permissions are externally modified and need to be reloaded.
     * </p>
     *
     * @param player the player to invalidate
     * @return a future that completes when the cache is refreshed
     */
    CompletableFuture<Void> invalidateCache(TalePlayer player);

    /**
     * Called when this provider is registered with the permission service.
     * <p>
     * Use this for initialization (database connections, etc.).
     * </p>
     */
    default void onEnable() {}

    /**
     * Called when this provider is unregistered or the server shuts down.
     * <p>
     * Use this for cleanup (closing connections, saving data).
     * </p>
     */
    default void onDisable() {}
}

