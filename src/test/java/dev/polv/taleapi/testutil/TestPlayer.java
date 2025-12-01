package dev.polv.taleapi.testutil;

import dev.polv.taleapi.command.CommandSender;
import dev.polv.taleapi.entity.TalePlayer;
import dev.polv.taleapi.permission.ContextSet;
import dev.polv.taleapi.permission.PermissionResult;
import dev.polv.taleapi.permission.PermissionService;
import dev.polv.taleapi.world.Location;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Test implementation of TalePlayer for testing purposes.
 * <p>
 * This class provides a simple in-memory implementation of the TalePlayer
 * interface that can be used across all tests. Also implements
 * {@link CommandSender} for command testing.
 * </p>
 * <p>
 * This test player supports two modes:
 * <ul>
 *   <li><b>Local mode:</b> Uses internal permission set (default)</li>
 *   <li><b>Service mode:</b> Delegates to PermissionService when available</li>
 * </ul>
 * </p>
 */
public class TestPlayer implements TalePlayer, CommandSender {
  private final String uuid;
  private final String name;
  private Location location;
  private final Set<String> permissions;
  private final List<String> messages;
  private boolean isOp;

  /**
   * Creates a new test player with the given name and a randomly generated UUID.
   *
   * @param name the display name for the player
   */
  public TestPlayer(String name) {
    this.uuid = UUID.randomUUID().toString();
    this.name = name;
    this.location = new Location(0, 0, 0);
    this.permissions = new HashSet<>();
    this.messages = new ArrayList<>();
    this.isOp = false;
  }

  /**
   * Creates a new test player with the specified UUID and name.
   *
   * @param uuid the unique identifier
   * @param name the display name
   */
  public TestPlayer(String uuid, String name) {
    this.uuid = uuid;
    this.name = name;
    this.location = new Location(0, 0, 0);
    this.permissions = new HashSet<>();
    this.messages = new ArrayList<>();
    this.isOp = false;
  }

  @Override
  public String getUniqueId() {
    return uuid;
  }

  @Override
  public String getDisplayName() {
    return name;
  }

  @Override
  public Location getLocation() {
    return location;
  }

  @Override
  public void teleport(Location location) {
    this.location = location;
  }

  @Override
  public boolean hasPermission(String permission) {
    // First, try to use the PermissionService if available
    PermissionService service = PermissionService.getInstance();
    if (service.hasProvider()) {
      return service.has(this, permission);
    }
    
    // Fall back to local permission checking
    return hasLocalPermission(permission);
  }
  
  /**
   * Checks if this player has a permission using only local storage.
   * <p>
   * This bypasses the PermissionService and uses the internal permission set.
   * Useful for tests that don't want to set up a full permission provider.
   * </p>
   *
   * @param permission the permission to check
   * @return {@code true} if the player has the permission locally
   */
  public boolean hasLocalPermission(String permission) {
    if (isOp) {
      return true;
    }
    // Check exact permission
    if (permissions.contains(permission)) {
      return true;
    }
    // Check wildcard permissions
    if (permissions.contains("*")) {
      return true;
    }
    // Check hierarchical wildcards (e.g., "server.*" matches "server.admin.kick")
    String[] parts = permission.split("\\.");
    StringBuilder current = new StringBuilder();
    for (int i = 0; i < parts.length; i++) {
      if (i > 0) {
        current.append(".");
      }
      current.append(parts[i]);
      if (permissions.contains(current + ".*")) {
        return true;
      }
    }
    return false;
  }
  
  @Override
  public PermissionResult getPermissionValue(String permission) {
    PermissionService service = PermissionService.getInstance();
    if (service.hasProvider()) {
      return service.query(this, permission);
    }
    // Fall back to simple boolean result
    return hasLocalPermission(permission) ? PermissionResult.ALLOWED : PermissionResult.UNDEFINED;
  }
  
  @Override
  public PermissionResult getPermissionValue(String permission, ContextSet context) {
    PermissionService service = PermissionService.getInstance();
    if (service.hasProvider()) {
      return service.query(this, permission, context);
    }
    // Local permissions don't support context
    return getPermissionValue(permission);
  }

  @Override
  public void sendMessage(String message) {
    messages.add(message);
  }

  @Override
  public String getName() {
    return name;
  }

  /**
   * Adds a permission to this player.
   *
   * @param permission the permission to add
   * @return this player for chaining
   */
  public TestPlayer addPermission(String permission) {
    permissions.add(permission);
    return this;
  }

  /**
   * Removes a permission from this player.
   *
   * @param permission the permission to remove
   * @return this player for chaining
   */
  public TestPlayer removePermission(String permission) {
    permissions.remove(permission);
    return this;
  }

  /**
   * Clears all permissions from this player.
   *
   * @return this player for chaining
   */
  public TestPlayer clearPermissions() {
    permissions.clear();
    return this;
  }

  /**
   * Returns all permissions this player has.
   *
   * @return a copy of the permissions set
   */
  public Set<String> getPermissions() {
    return new HashSet<>(permissions);
  }

  /**
   * Sets whether this player is an operator (has all permissions).
   *
   * @param isOp whether the player is an operator
   * @return this player for chaining
   */
  public TestPlayer setOp(boolean isOp) {
    this.isOp = isOp;
    return this;
  }

  /**
   * Checks if this player is an operator.
   *
   * @return {@code true} if the player is an operator
   */
  public boolean isOp() {
    return isOp;
  }

  /**
   * Returns all messages sent to this player.
   *
   * @return a copy of the messages list
   */
  public List<String> getMessages() {
    return new ArrayList<>(messages);
  }

  /**
   * Returns the last message sent to this player.
   *
   * @return the last message, or null if no messages
   */
  public String getLastMessage() {
    return messages.isEmpty() ? null : messages.get(messages.size() - 1);
  }

  /**
   * Clears all received messages.
   *
   * @return this player for chaining
   */
  public TestPlayer clearMessages() {
    messages.clear();
    return this;
  }

  @Override
  public String toString() {
    return "TestPlayer{" +
        "uuid='" + uuid + '\'' +
        ", name='" + name + '\'' +
        '}';
  }
}
