package dev.polv.taleapi.command;

import dev.polv.taleapi.entity.TalePlayer;

/**
 * Represents an entity that can execute commands.
 * <p>
 * This can be a player, the server console, or any other command source.
 * </p>
 */
public interface CommandSender {

  /**
   * Sends a message to this command sender.
   *
   * @param message the message to send
   */
  void sendMessage(String message);

  /**
   * Checks if this sender has the specified permission.
   * <p>
   * Console senders typically have all permissions.
   * </p>
   *
   * @param permission the permission node to check
   * @return {@code true} if the sender has the permission
   */
  boolean hasPermission(String permission);

  /**
   * Returns the name of this command sender.
   * <p>
   * For players, this returns their display name.
   * For console, this typically returns "Console" or "Server".
   * </p>
   *
   * @return the sender's name
   */
  String getName();

  /**
   * Checks if this sender is a player.
   *
   * @return {@code true} if this sender is a player
   */
  default boolean isPlayer() {
    return this instanceof TalePlayer;
  }

  /**
   * Returns this sender as a player, if applicable.
   *
   * @return the player, or {@code null} if this sender is not a player
   */
  default TalePlayer asPlayer() {
    return isPlayer() ? (TalePlayer) this : null;
  }
}

