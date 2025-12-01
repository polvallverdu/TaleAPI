package dev.polv.taleapi.event.player;

import dev.polv.taleapi.event.EventPriority;
import dev.polv.taleapi.testutil.TestPlayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("PlayerQuitCallback")
class PlayerQuitCallbackTest {

  @AfterEach
  void cleanup() {
    // Clear listeners after each test to avoid interference
    PlayerQuitCallback.EVENT.clearListeners();
  }

  @Test
  @DisplayName("should notify listeners when player quits")
  void shouldNotifyOnQuit() {
    List<String> quitPlayers = new ArrayList<>();

    PlayerQuitCallback.EVENT.register(player -> {
      quitPlayers.add(player.getDisplayName());
    });

    TestPlayer player = new TestPlayer("LeavingUser");
    PlayerQuitCallback.EVENT.invoker().onPlayerQuit(player);

    assertEquals(List.of("LeavingUser"), quitPlayers);
  }

  @Test
  @DisplayName("should execute all listeners even without return value")
  void shouldExecuteAllListeners() {
    List<Integer> executionOrder = new ArrayList<>();

    PlayerQuitCallback.EVENT.register(player -> executionOrder.add(1));
    PlayerQuitCallback.EVENT.register(player -> executionOrder.add(2));
    PlayerQuitCallback.EVENT.register(player -> executionOrder.add(3));

    TestPlayer player = new TestPlayer("Player");
    PlayerQuitCallback.EVENT.invoker().onPlayerQuit(player);

    assertEquals(List.of(1, 2, 3), executionOrder);
  }

  @Test
  @DisplayName("should respect priority order")
  void shouldRespectPriorityOrder() {
    List<String> order = new ArrayList<>();

    PlayerQuitCallback.EVENT.register(EventPriority.HIGH, player -> order.add("HIGH"));
    PlayerQuitCallback.EVENT.register(EventPriority.LOWEST, player -> order.add("LOWEST"));
    PlayerQuitCallback.EVENT.register(EventPriority.NORMAL, player -> order.add("NORMAL"));

    TestPlayer player = new TestPlayer("Player");
    PlayerQuitCallback.EVENT.invoker().onPlayerQuit(player);

    assertEquals(List.of("HIGH", "NORMAL", "LOWEST"), order);
  }

  @Test
  @DisplayName("should allow cleanup operations")
  void shouldAllowCleanup() {
    List<String> savedPlayers = new ArrayList<>();

    PlayerQuitCallback.EVENT.register(EventPriority.HIGHEST, player -> {
      // Simulate saving player data first (runs first with HIGHEST priority)
      savedPlayers.add(player.getUniqueId());
    });

    TestPlayer player = new TestPlayer("SavingPlayer");
    PlayerQuitCallback.EVENT.invoker().onPlayerQuit(player);

    assertEquals(1, savedPlayers.size());
    assertEquals(player.getUniqueId(), savedPlayers.get(0));
  }

  @Test
  @DisplayName("should execute all listeners at all priorities")
  void shouldExecuteAllListenersAtAllPriorities() {
    List<String> order = new ArrayList<>();

    PlayerQuitCallback.EVENT.register(EventPriority.HIGHEST, player -> order.add("HIGHEST"));
    PlayerQuitCallback.EVENT.register(EventPriority.HIGH, player -> order.add("HIGH"));
    PlayerQuitCallback.EVENT.register(EventPriority.NORMAL, player -> order.add("NORMAL"));
    PlayerQuitCallback.EVENT.register(EventPriority.LOW, player -> order.add("LOW"));
    PlayerQuitCallback.EVENT.register(EventPriority.LOWEST, player -> order.add("LOWEST"));

    TestPlayer player = new TestPlayer("Player");
    PlayerQuitCallback.EVENT.invoker().onPlayerQuit(player);

    assertEquals(List.of("HIGHEST", "HIGH", "NORMAL", "LOW", "LOWEST"), order);
  }

  @Test
  @DisplayName("should allow multiple listeners at the same priority")
  void shouldAllowMultipleListenersAtSamePriority() {
    List<String> executed = new ArrayList<>();

    PlayerQuitCallback.EVENT.register(EventPriority.NORMAL, player -> executed.add("first"));
    PlayerQuitCallback.EVENT.register(EventPriority.NORMAL, player -> executed.add("second"));
    PlayerQuitCallback.EVENT.register(EventPriority.NORMAL, player -> executed.add("third"));

    TestPlayer player = new TestPlayer("TestUser");
    PlayerQuitCallback.EVENT.invoker().onPlayerQuit(player);

    assertEquals(List.of("first", "second", "third"), executed);
  }

  @Test
  @DisplayName("should handle empty listener list gracefully")
  void shouldHandleEmptyListenerList() {
    // Should not throw any exceptions
    TestPlayer player = new TestPlayer("TestUser");
    PlayerQuitCallback.EVENT.invoker().onPlayerQuit(player);
  }

  @Test
  @DisplayName("should pass player data to all listeners")
  void shouldPassPlayerDataToAllListeners() {
    List<String> playerIds = new ArrayList<>();
    List<String> playerNames = new ArrayList<>();

    PlayerQuitCallback.EVENT.register(player -> {
      playerIds.add(player.getUniqueId());
      playerNames.add(player.getDisplayName());
    });

    TestPlayer player = new TestPlayer("test-uuid", "TestUser");
    PlayerQuitCallback.EVENT.invoker().onPlayerQuit(player);

    assertEquals(List.of("test-uuid"), playerIds);
    assertEquals(List.of("TestUser"), playerNames);
  }
}
