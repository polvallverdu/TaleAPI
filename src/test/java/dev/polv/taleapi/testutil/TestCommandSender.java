package dev.polv.taleapi.testutil;

import dev.polv.taleapi.command.CommandSender;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Test implementation of CommandSender for testing console-like command sources.
 * <p>
 * This can be used to test commands that should work for non-player senders.
 * By default, it has all permissions (like a console).
 * </p>
 */
public class TestCommandSender implements CommandSender {

  private final String name;
  private final Set<String> permissions;
  private final List<String> messages;
  private boolean hasAllPermissions;

  /**
   * Creates a new test command sender with default name "Console".
   */
  public TestCommandSender() {
    this("Console");
  }

  /**
   * Creates a new test command sender with the given name.
   *
   * @param name the sender name
   */
  public TestCommandSender(String name) {
    this.name = name;
    this.permissions = new HashSet<>();
    this.messages = new ArrayList<>();
    this.hasAllPermissions = true; // Console has all permissions by default
  }

  @Override
  public void sendMessage(String message) {
    messages.add(message);
  }

  @Override
  public boolean hasPermission(String permission) {
    if (hasAllPermissions) {
      return true;
    }
    return permissions.contains(permission) || permissions.contains("*");
  }

  @Override
  public String getName() {
    return name;
  }

  /**
   * Adds a permission to this sender.
   *
   * @param permission the permission to add
   * @return this sender for chaining
   */
  public TestCommandSender addPermission(String permission) {
    permissions.add(permission);
    return this;
  }

  /**
   * Sets whether this sender has all permissions.
   *
   * @param hasAll whether to have all permissions
   * @return this sender for chaining
   */
  public TestCommandSender setHasAllPermissions(boolean hasAll) {
    this.hasAllPermissions = hasAll;
    return this;
  }

  /**
   * Returns all messages sent to this sender.
   *
   * @return a copy of the messages list
   */
  public List<String> getMessages() {
    return new ArrayList<>(messages);
  }

  /**
   * Returns the last message sent to this sender.
   *
   * @return the last message, or null if no messages
   */
  public String getLastMessage() {
    return messages.isEmpty() ? null : messages.get(messages.size() - 1);
  }

  /**
   * Clears all received messages.
   *
   * @return this sender for chaining
   */
  public TestCommandSender clearMessages() {
    messages.clear();
    return this;
  }

  @Override
  public String toString() {
    return "TestCommandSender{name='" + name + "'}";
  }
}

