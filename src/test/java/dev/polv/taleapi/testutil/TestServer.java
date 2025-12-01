package dev.polv.taleapi.testutil;

import dev.polv.taleapi.entity.TalePlayer;
import dev.polv.taleapi.server.TaleServer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Test implementation of TaleServer for testing purposes.
 * <p>
 * This class provides a simple in-memory implementation of the TaleServer
 * interface that can be used for testing server-related functionality.
 * </p>
 * <p>
 * Supports player tracking via {@link #addPlayer(TalePlayer)} and
 * {@link #removePlayer(TalePlayer)} methods.
 * </p>
 */
public class TestServer implements TaleServer {
  private final String name;
  private final Map<String, TalePlayer> onlinePlayers;
  private final List<String> broadcastMessages;

  /**
   * Creates a new test server with a default name.
   */
  public TestServer() {
    this("TestServer");
  }

  /**
   * Creates a new test server with the specified name.
   *
   * @param name the server name
   */
  public TestServer(String name) {
    this.name = name;
    this.onlinePlayers = new ConcurrentHashMap<>();
    this.broadcastMessages = Collections.synchronizedList(new ArrayList<>());
  }

  /**
   * @return the server name
   */
  public String getName() {
    return name;
  }

  @Override
  public Collection<TalePlayer> getOnlinePlayers() {
    return Collections.unmodifiableCollection(onlinePlayers.values());
  }

  @Override
  public Optional<TalePlayer> getPlayer(String uniqueId) {
    return Optional.ofNullable(onlinePlayers.get(uniqueId));
  }

  @Override
  public void broadcastMessage(String message) {
    broadcastMessages.add(message);
    for (TalePlayer player : onlinePlayers.values()) {
      player.sendMessage(message);
    }
  }

  /**
   * Adds a player to this server's online player list.
   *
   * @param player the player to add
   * @return this server for chaining
   */
  public TestServer addPlayer(TalePlayer player) {
    onlinePlayers.put(player.getUniqueId(), player);
    return this;
  }

  /**
   * Removes a player from this server's online player list.
   *
   * @param player the player to remove
   * @return this server for chaining
   */
  public TestServer removePlayer(TalePlayer player) {
    onlinePlayers.remove(player.getUniqueId());
    return this;
  }

  /**
   * Removes a player by their unique ID.
   *
   * @param uniqueId the player's unique ID
   * @return this server for chaining
   */
  public TestServer removePlayer(String uniqueId) {
    onlinePlayers.remove(uniqueId);
    return this;
  }

  /**
   * Clears all online players.
   *
   * @return this server for chaining
   */
  public TestServer clearPlayers() {
    onlinePlayers.clear();
    return this;
  }

  /**
   * Returns the number of online players.
   *
   * @return the online player count
   */
  public int getOnlinePlayerCount() {
    return onlinePlayers.size();
  }

  /**
   * Returns all broadcast messages sent to this server.
   *
   * @return a copy of the broadcast messages list
   */
  public List<String> getBroadcastMessages() {
    return new ArrayList<>(broadcastMessages);
  }

  /**
   * Returns the last broadcast message sent.
   *
   * @return the last message, or null if no messages
   */
  public String getLastBroadcastMessage() {
    return broadcastMessages.isEmpty() ? null : broadcastMessages.get(broadcastMessages.size() - 1);
  }

  /**
   * Clears all recorded broadcast messages.
   *
   * @return this server for chaining
   */
  public TestServer clearBroadcastMessages() {
    broadcastMessages.clear();
    return this;
  }

  /**
   * Finds an online player by their display name.
   * <p>
   * Note: Display names are not unique. This returns the first match.
   * </p>
   *
   * @param name the player's display name
   * @return an Optional containing the player, or empty if not found
   */
  public Optional<TalePlayer> getPlayerByName(String name) {
    return onlinePlayers.values().stream()
        .filter(p -> p.getDisplayName().equals(name))
        .findFirst();
  }

  @Override
  public String toString() {
    return "TestServer{" +
        "name='" + name + '\'' +
        ", onlinePlayers=" + onlinePlayers.size() +
        '}';
  }
}
