package dev.polv.taleapi.entity;

/**
 * Represents a player in Hytale.
 * <p>
 * Players are a specialized type of entity with additional player-specific
 * functionality. They inherit all entity behavior from {@link TaleEntity}.
 * </p>
 * 
 * <h2>Event Behavior</h2>
 * <p>
 * Players fire both player-specific events (like {@code PlayerJoinCallback})
 * and general entity events (like {@code EntityMoveCallback}). When listening
 * to entity events, you can check if the entity is a player using
 * {@code instanceof TalePlayer}.
 * </p>
 */
public interface TalePlayer extends TaleEntity {

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
   * Permissions are dot-separated strings following the LuckPerms convention
   * (e.g., "server.admin.kick", "world.edit", "chat.color").
   * </p>
   * <p>
   * Permission checking supports wildcards:
   * <ul>
   *   <li>{@code server.admin.*} - grants all permissions under server.admin</li>
   *   <li>{@code *} - grants all permissions</li>
   * </ul>
   * </p>
   *
   * @param permission the permission node to check
   * @return {@code true} if the player has the permission, {@code false} otherwise
   */
  boolean hasPermission(String permission);

  /**
   * Sends a message to the player.
   *
   * @param message the message to send
   */
  void sendMessage(String message);

}
