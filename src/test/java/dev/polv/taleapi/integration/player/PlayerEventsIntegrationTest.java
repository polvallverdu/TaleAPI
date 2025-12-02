package dev.polv.taleapi.integration.player;

import dev.polv.taleapi.event.player.PlayerJoinCallback;
import dev.polv.taleapi.event.player.PlayerJoinResult;
import dev.polv.taleapi.event.player.PlayerQuitCallback;
import dev.polv.taleapi.testutil.TestPlayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for player events.
 * <p>
 * These tests verify that different player events work together correctly
 * and handle complex scenarios involving multiple events and players.
 * </p>
 */
@DisplayName("Player Events Integration")
class PlayerEventsIntegrationTest {

  @AfterEach
  void cleanup() {
    // Clear listeners after each test to avoid interference
    PlayerJoinCallback.EVENT.clearListeners();
    PlayerQuitCallback.EVENT.clearListeners();
  }

  @Test
  @DisplayName("should handle full player lifecycle")
  void shouldHandleFullLifecycle() {
    List<String> events = new ArrayList<>();

    PlayerJoinCallback.EVENT.register(player -> {
      events.add("join:" + player.getDisplayName());
      return PlayerJoinResult.pass();
    });

    PlayerQuitCallback.EVENT.register(player -> {
      events.add("quit:" + player.getDisplayName());
    });

    TestPlayer player = new TestPlayer("LifecyclePlayer");

    // Player joins (async event, awaited with join())
    PlayerJoinResult joinResult = PlayerJoinCallback.EVENT.invoker().onPlayerJoin(player).join();
    assertFalse(joinResult.isCancelled());

    // Player quits (sync event)
    PlayerQuitCallback.EVENT.invoker().onPlayerQuit(player);

    assertEquals(List.of("join:LifecyclePlayer", "quit:LifecyclePlayer"), events);
  }

  @Test
  @DisplayName("should handle multiple players with different outcomes")
  void shouldHandleMultiplePlayers() {
    List<String> events = new ArrayList<>();

    // Ban check for join
    PlayerJoinCallback.EVENT.register(player -> {
      if (player.getDisplayName().startsWith("Banned")) {
        events.add("blocked:" + player.getDisplayName());
        return PlayerJoinResult.cancel();
      }
      events.add("joined:" + player.getDisplayName());
      return PlayerJoinResult.pass();
    });

    // Track quits
    PlayerQuitCallback.EVENT.register(player -> {
      events.add("quit:" + player.getDisplayName());
    });

    TestPlayer player1 = new TestPlayer("Alice");
    TestPlayer player2 = new TestPlayer("BannedBob");
    TestPlayer player3 = new TestPlayer("Charlie");

    // Alice joins successfully
    PlayerJoinResult result1 = PlayerJoinCallback.EVENT.invoker().onPlayerJoin(player1).join();
    assertFalse(result1.isCancelled());

    // BannedBob is blocked
    PlayerJoinResult result2 = PlayerJoinCallback.EVENT.invoker().onPlayerJoin(player2).join();
    assertTrue(result2.isCancelled());

    // Charlie joins successfully
    PlayerJoinResult result3 = PlayerJoinCallback.EVENT.invoker().onPlayerJoin(player3).join();
    assertFalse(result3.isCancelled());

    // Alice and Charlie quit (BannedBob never joined)
    PlayerQuitCallback.EVENT.invoker().onPlayerQuit(player1);
    PlayerQuitCallback.EVENT.invoker().onPlayerQuit(player3);

    assertEquals(List.of(
        "joined:Alice",
        "blocked:BannedBob",
        "joined:Charlie",
        "quit:Alice",
        "quit:Charlie"), events);
  }

  @Test
  @DisplayName("should maintain independent event listener lists")
  void shouldMaintainIndependentListenerLists() {
    List<String> joinEvents = new ArrayList<>();
    List<String> quitEvents = new ArrayList<>();

    PlayerJoinCallback.EVENT.register(player -> {
      joinEvents.add(player.getDisplayName());
      return PlayerJoinResult.pass();
    });

    PlayerQuitCallback.EVENT.register(player -> {
      quitEvents.add(player.getDisplayName());
    });

    TestPlayer player = new TestPlayer("TestUser");

    PlayerJoinCallback.EVENT.invoker().onPlayerJoin(player).join();
    PlayerQuitCallback.EVENT.invoker().onPlayerQuit(player);

    assertEquals(List.of("TestUser"), joinEvents);
    assertEquals(List.of("TestUser"), quitEvents);
  }

  @Test
  @DisplayName("should allow cross-event communication through shared state")
  void shouldAllowCrossEventCommunication() {
    List<String> onlinePlayers = new ArrayList<>();

    PlayerJoinCallback.EVENT.register(player -> {
      onlinePlayers.add(player.getDisplayName());
      return PlayerJoinResult.pass();
    });

    PlayerQuitCallback.EVENT.register(player -> {
      onlinePlayers.remove(player.getDisplayName());
    });

    TestPlayer player1 = new TestPlayer("Player1");
    TestPlayer player2 = new TestPlayer("Player2");

    // Join events
    PlayerJoinCallback.EVENT.invoker().onPlayerJoin(player1).join();
    PlayerJoinCallback.EVENT.invoker().onPlayerJoin(player2).join();
    assertEquals(List.of("Player1", "Player2"), onlinePlayers);

    // Quit event
    PlayerQuitCallback.EVENT.invoker().onPlayerQuit(player1);
    assertEquals(List.of("Player2"), onlinePlayers);

    // Another quit
    PlayerQuitCallback.EVENT.invoker().onPlayerQuit(player2);
    assertEquals(List.of(), onlinePlayers);
  }
}
