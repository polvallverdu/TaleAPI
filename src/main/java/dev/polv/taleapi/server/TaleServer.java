package dev.polv.taleapi.server;

import dev.polv.taleapi.entity.TalePlayer;
import dev.polv.taleapi.permission.PermissionService;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents the game server and provides access to server-wide functionality.
 * <p>
 * TaleServer is the central access point for:
 * <ul>
 *   <li>Online player management and tracking</li>
 *   <li>Permission service access</li>
 *   <li>Server configuration</li>
 * </ul>
 * </p>
 *
 * <h2>Player Tracking</h2>
 * <p>
 * The server automatically tracks players joining and leaving. The permission
 * system is hooked into these events to load/unload player permissions.
 * </p>
 * <pre>{@code
 * // Get all online players
 * Collection<TalePlayer> players = TaleServer.getInstance().getOnlinePlayers();
 *
 * // Find a specific player
 * Optional<TalePlayer> player = TaleServer.getInstance().getPlayer("Steve");
 * }</pre>
 *
 * <h2>Permissions</h2>
 * <p>
 * Access the permission service for advanced permission operations:
 * </p>
 * <pre>{@code
 * PermissionService perms = TaleServer.getInstance().getPermissions();
 * perms.setPermission(player, PermissionNode.allow("cmd.fly"));
 * }</pre>
 */
public interface TaleServer {

  /**
   * Returns the singleton instance of the server.
   * <p>
   * This method must be implemented by the actual server implementation.
   * </p>
   *
   * @return the server instance
   * @throws UnsupportedOperationException if no implementation is available
   */
  static TaleServer getInstance() {
    throw new UnsupportedOperationException("Not implemented - awaiting Hytale server implementation");
  }

  /**
   * Returns the permission service.
   * <p>
   * This provides access to the permission system for advanced operations
   * like setting permissions programmatically or registering custom providers.
   * </p>
   *
   * @return the permission service
   */
  default PermissionService getPermissions() {
    return PermissionService.getInstance();
  }

  /**
   * Returns all currently online players.
   *
   * @return an unmodifiable collection of online players
   */
  Collection<TalePlayer> getOnlinePlayers();

  /**
   * Finds an online player by their unique ID.
   *
   * @param uniqueId the player's unique identifier
   * @return an Optional containing the player, or empty if not online
   */
  Optional<TalePlayer> getPlayer(String uniqueId);

  /**
   * Broadcasts a message to all online players.
   *
   * @param message the message to broadcast
   */
  void broadcastMessage(String message);

}
