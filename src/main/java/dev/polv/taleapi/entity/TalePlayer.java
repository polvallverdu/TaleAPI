package dev.polv.taleapi.entity;

import dev.polv.taleapi.permission.PermissionHolder;
import dev.polv.taleapi.permission.PermissionResult;

/**
 * Represents a player in Hytale.
 * <p>
 * Players are a specialized type of entity with additional player-specific
 * functionality. They inherit all entity behavior from {@link TaleEntity}
 * and permission capabilities from {@link PermissionHolder}.
 * </p>
 * 
 * <h2>Event Behavior</h2>
 * <p>
 * Players fire both player-specific events (like {@code PlayerJoinCallback})
 * and general entity events (like {@code EntityMoveCallback}). When listening
 * to entity events, you can check if the entity is a player using
 * {@code instanceof TalePlayer}.
 * </p>
 *
 * <h2>Permissions</h2>
 * <p>
 * Players implement {@link PermissionHolder}, providing access to the
 * extensible permission system. Use {@link #hasPermission(String)} for
 * simple checks, or {@link #getPermissionValue(String)} for dynamic values.
 * </p>
 * 
 * <pre>{@code
 * // Simple boolean check
 * if (player.hasPermission("cmd.teleport")) { ... }
 *
 * // Dynamic value (e.g., max homes limit)
 * int maxHomes = player.getPermissionValue("homes.limit").asInt(3);
 * }</pre>
 *
 * @see PermissionHolder
 * @see PermissionResult
 */
public interface TalePlayer extends TaleEntity, PermissionHolder {

  /**
   * Returns the player's unique identifier.
   * <p>
   * This overrides {@link TaleEntity#getUniqueId()} to clarify that for players,
   * this represents their account UUID. If the Hytale team ends up using UUIDs,
   * we will use that directly.
   * </p>
   *
   * @return the player's UUID, guaranteed to be unique
   */
  @Override
  String getUniqueId();

  /**
   * Returns the player's display name.
   * <p>
   * Note: Display names are NOT guaranteed to be unique across players.
   * </p>
   *
   * @return the player's display name
   */
  String getDisplayName();

  /**
   * Checks if the player has the specified permission.
   * <p>
   * Permissions are dot-separated strings following a hierarchical convention
   * (e.g., "server.admin.kick", "world.edit", "chat.color").
   * </p>
   * <p>
   * Permission checking supports wildcards:
   * </p>
   * <ul>
   * <li>{@code server.admin.*} - grants all permissions under server.admin</li>
   * <li>{@code *} - grants all permissions</li>
   * </ul>
   * <p>
   * This method is inherited from {@link PermissionHolder} and delegates to
   * the active {@link dev.polv.taleapi.permission.PermissionService}.
   * </p>
   *
   * @param permission the permission node to check
   * @return {@code true} if the player has the permission, {@code false}
   *         otherwise
   */
  @Override
  boolean hasPermission(String permission);

  /**
   * Sends a message to the player.
   *
   * @param message the message to send
   */
  void sendMessage(String message);

}
