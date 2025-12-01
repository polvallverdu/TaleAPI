/**
 * Extensible Permission System for TaleAPI.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides a modern, high-performance permission system built on
 * the Service Provider Interface (SPI) pattern. It supports dynamic values,
 * contextual permissions, and efficient O(k) lookups using a Radix Tree.
 * </p>
 *
 * <h2>Architecture</h2>
 * 
 * <pre>
 *                ┌──────────────────────┐
 *                │  PermissionService   │  ← Public API
 *                │    (Singleton)       │
 *                └──────────┬───────────┘
 *                           │
 *                ┌──────────▼───────────┐
 *                │  PermissionProvider  │  ← SPI Interface
 *                │    (Pluggable)       │
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
 * <h2>Key Features</h2>
 * <ul>
 * <li><b>Tristate:</b> ALLOW, DENY, or UNDEFINED (not just boolean)</li>
 * <li><b>Dynamic Values:</b> Permissions can carry payloads (integers, strings,
 * etc.)</li>
 * <li><b>Context:</b> Permissions can be conditional (world, gamemode,
 * server)</li>
 * <li><b>Wildcards:</b> Instant O(1) wildcard matching via Radix Tree</li>
 * <li><b>Events:</b> Hook into permission checks to temporarily override
 * results</li>
 * </ul>
 *
 * <h2>Quick Start</h2>
 * 
 * <pre>{@code
 * // Get the permission service
 * PermissionService perms = PermissionService.getInstance();
 *
 * // Query a boolean permission
 * if (perms.query(player, "plots.claim").isAllowed()) {
 *   // Player can claim plots
 * }
 *
 * // Query a permission with a dynamic value
 * int maxPlots = perms.query(player, "plots.limit").asInt(1);
 *
 * // Context-aware query
 * ContextSet context = ContextSet.of(ContextKey.WORLD, "nether");
 * if (perms.query(player, "ability.fly", context).isAllowed()) {
 *   // Player can fly in the nether
 * }
 * }</pre>
 *
 * <h2>Hooking Permission Checks</h2>
 * 
 * <pre>{@code
 * // Temporarily deny teleport during a minigame
 * PermissionCheckCallback.EVENT.register((player, key, ctx, result) -> {
 *   if (matchManager.isInMatch(player) && key.startsWith("cmd.teleport")) {
 *     return CheckResult.deny();
 *   }
 *   return CheckResult.unmodified();
 * });
 * }</pre>
 *
 * @see dev.polv.taleapi.permission.PermissionService
 * @see dev.polv.taleapi.permission.PermissionProvider
 * @see dev.polv.taleapi.permission.PermissionResult
 * @see dev.polv.taleapi.event.player.PermissionCheckCallback
 */
package dev.polv.taleapi.permission;
