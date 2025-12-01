package dev.polv.taleapi.permission;

import dev.polv.taleapi.entity.TalePlayer;
import dev.polv.taleapi.event.player.PermissionCheckCallback;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The main entry point for the permission system.
 * <p>
 * PermissionService acts as a facade over the active
 * {@link PermissionProvider}.
 * It handles provider registration, query routing, and event firing.
 * </p>
 *
 * <h2>SPI Architecture</h2>
 * 
 * <pre>
 *                ┌──────────────────────┐
 *                │  PermissionService   │  ← Public API
 *                │    (Facade)          │
 *                └──────────┬───────────┘
 *                           │
 *            ┌──────────────┴──────────────┐
 *            ▼                             ▼
 *    ┌───────────────┐             ┌───────────────┐
 *    │DefaultProvider│             │ Custom Plugin │
 *    │   (JSON)      │             │   Provider    │
 *    └───────────────┘             └───────────────┘
 * </pre>
 *
 * <h2>The Hook</h2>
 * <p>
 * After the provider calculates the result, a {@link PermissionCheckCallback}
 * event fires. This allows plugins to temporarily override permissions
 * (e.g., "Deny all commands during a minigame match").
 * </p>
 *
 * <h2>Example Usage</h2>
 * 
 * <pre>{@code
 * // Query a simple permission
 * if (permService.query(player, "plots.claim").isAllowed()) {
 *     // Player can claim plots
 * }
 *
 * // Query a permission with payload (dynamic limit)
 * int maxPlots = permService.query(player, "plots.limit").asInt(1);
 *
 * // Context-aware query
 * ContextSet context = ContextSet.of(ContextKey.WORLD, "creative");
 * if (permService.query(player, "build.creative", context).isAllowed()) {
 *     // Player can build in creative world
 * }
 * }</pre>
 *
 * @see PermissionProvider
 * @see PermissionResult
 */
public final class PermissionService {

    private static final PermissionService INSTANCE = new PermissionService();

    private final AtomicReference<PermissionProvider> provider;

    private PermissionService() {
        this.provider = new AtomicReference<>();
    }

    /**
     * Returns the singleton instance of the permission service.
     *
     * @return the permission service
     */
    public static PermissionService getInstance() {
        return INSTANCE;
    }

    /**
     * Registers a permission provider.
     * <p>
     * If a provider is already registered, it will be disabled and replaced.
     * </p>
     *
     * @param newProvider the provider to register
     * @throws NullPointerException if provider is null
     */
    public void setProvider(PermissionProvider newProvider) {
        Objects.requireNonNull(newProvider, "provider");

        PermissionProvider oldProvider = provider.getAndSet(newProvider);
        if (oldProvider != null) {
            oldProvider.onDisable();
        }
        newProvider.onEnable();
    }

    /**
     * Returns the currently active provider.
     *
     * @return the active provider, or null if none registered
     */
    public PermissionProvider getProvider() {
        return provider.get();
    }

    /**
     * Checks if a provider is registered.
     *
     * @return {@code true} if a provider is active
     */
    public boolean hasProvider() {
        return provider.get() != null;
    }

    /**
     * Queries a permission for a player.
     * <p>
     * This method:
     * <ol>
     * <li>Delegates to the active provider</li>
     * <li>Fires a {@link PermissionCheckCallback} event</li>
     * <li>Returns the (possibly modified) result</li>
     * </ol>
     * </p>
     *
     * @param player the player to check
     * @param key    the permission key
     * @return the permission result
     * @throws IllegalStateException if no provider is registered
     */
    public PermissionResult query(TalePlayer player, String key) {
        return query(player, key, ContextSet.EMPTY);
    }

    /**
     * Queries a permission for a player with context.
     *
     * @param player  the player to check
     * @param key     the permission key
     * @param context the context to check against
     * @return the permission result
     * @throws IllegalStateException if no provider is registered
     */
    public PermissionResult query(TalePlayer player, String key, ContextSet context) {
        PermissionProvider activeProvider = requireProvider();

        // Get result from provider
        PermissionResult result = activeProvider.query(player, key, context);

        // Fire the hook event - allows plugins to override
        PermissionCheckCallback.CheckResult hookResult = PermissionCheckCallback.EVENT.invoker()
                .onPermissionCheck(player, key, context, result);

        // If a listener modified the result, use the new one
        if (hookResult.isModified()) {
            return hookResult.getResult();
        }

        return result;
    }

    /**
     * Convenience method to check if a permission is allowed.
     *
     * @param player the player to check
     * @param key    the permission key
     * @return {@code true} if the permission is ALLOW
     */
    public boolean has(TalePlayer player, String key) {
        return query(player, key).isAllowed();
    }

    /**
     * Convenience method to check if a permission is allowed with context.
     *
     * @param player  the player to check
     * @param key     the permission key
     * @param context the context
     * @return {@code true} if the permission is ALLOW
     */
    public boolean has(TalePlayer player, String key, ContextSet context) {
        return query(player, key, context).isAllowed();
    }

    /**
     * Gets the cached permission tree for a player.
     *
     * @param player the player
     * @return the player's permission tree
     * @throws IllegalStateException if no provider is registered
     */
    public PermissionTree getPlayerTree(TalePlayer player) {
        return requireProvider().getPlayerTree(player);
    }

    /**
     * Sets a permission for a player.
     *
     * @param player the player
     * @param node   the permission node
     * @return a future that completes when saved
     */
    public CompletableFuture<Void> setPermission(TalePlayer player, PermissionNode node) {
        return requireProvider().setPermission(player, node);
    }

    /**
     * Removes a permission from a player.
     *
     * @param player the player
     * @param key    the permission key
     * @return a future that completes when removed
     */
    public CompletableFuture<Void> removePermission(TalePlayer player, String key) {
        return requireProvider().removePermission(player, key);
    }

    /**
     * Returns permission keys to sync to the client.
     *
     * @param player the player
     * @return set of client-synced permission keys
     */
    public Set<String> getClientSyncedNodes(TalePlayer player) {
        return requireProvider().getClientSyncedNodes(player);
    }

    /**
     * Called when a player joins. Loads their permissions.
     *
     * @param player the player
     * @return future completing when loaded
     */
    public CompletableFuture<Void> loadPlayer(TalePlayer player) {
        PermissionProvider p = provider.get();
        if (p != null) {
            return p.loadPlayer(player);
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Called when a player leaves. Unloads their permissions.
     *
     * @param player the player
     */
    public void unloadPlayer(TalePlayer player) {
        PermissionProvider p = provider.get();
        if (p != null) {
            p.unloadPlayer(player);
        }
    }

    /**
     * Invalidates and refreshes a player's permission cache.
     *
     * @param player the player
     * @return future completing when refreshed
     */
    public CompletableFuture<Void> invalidateCache(TalePlayer player) {
        return requireProvider().invalidateCache(player);
    }

    /**
     * Shuts down the permission service.
     * <p>
     * Called on server shutdown. Disables the active provider.
     * </p>
     */
    public void shutdown() {
        PermissionProvider p = provider.getAndSet(null);
        if (p != null) {
            p.onDisable();
        }
    }

    private PermissionProvider requireProvider() {
        PermissionProvider p = provider.get();
        if (p == null) {
            throw new IllegalStateException("No permission provider registered");
        }
        return p;
    }
}
