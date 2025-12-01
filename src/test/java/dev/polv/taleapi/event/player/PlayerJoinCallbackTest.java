package dev.polv.taleapi.event.player;

import dev.polv.taleapi.event.EventPriority;
import dev.polv.taleapi.event.EventResult;
import dev.polv.taleapi.testutil.TestPlayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PlayerJoinCallback")
class PlayerJoinCallbackTest {

  @AfterEach
  void cleanup() {
    // Clear listeners after each test to avoid interference
    PlayerJoinCallback.EVENT.clearListeners();
  }

  @Test
  @DisplayName("should notify listeners when player joins")
  void shouldNotifyOnJoin() {
    List<String> joinedPlayers = new ArrayList<>();

    PlayerJoinCallback.EVENT.register(player -> {
      joinedPlayers.add(player.getDisplayName());
      return EventResult.PASS;
    });

    TestPlayer player = new TestPlayer("TestUser");
    PlayerJoinCallback.EVENT.invoker().onPlayerJoin(player);

    assertEquals(List.of("TestUser"), joinedPlayers);
  }

  @Test
  @DisplayName("should allow cancelling player join")
  void shouldAllowCancellingJoin() {
    PlayerJoinCallback.EVENT.register(player -> {
      if (player.getDisplayName().equals("BannedUser")) {
        return EventResult.CANCEL;
      }
      return EventResult.PASS;
    });

    TestPlayer bannedPlayer = new TestPlayer("BannedUser");
    EventResult result = PlayerJoinCallback.EVENT.invoker().onPlayerJoin(bannedPlayer);

    assertTrue(result.isCancelled());
  }

  @Test
  @DisplayName("should execute listeners in priority order")
  void shouldExecuteInPriorityOrder() {
    List<String> order = new ArrayList<>();

    PlayerJoinCallback.EVENT.register(EventPriority.HIGHEST, player -> {
      order.add("HIGHEST");
      return EventResult.PASS;
    });
    PlayerJoinCallback.EVENT.register(EventPriority.LOWEST, player -> {
      order.add("LOWEST");
      return EventResult.PASS;
    });
    PlayerJoinCallback.EVENT.register(EventPriority.NORMAL, player -> {
      order.add("NORMAL");
      return EventResult.PASS;
    });

    TestPlayer player = new TestPlayer("Player");
    PlayerJoinCallback.EVENT.invoker().onPlayerJoin(player);

    assertEquals(List.of("HIGHEST", "NORMAL", "LOWEST"), order);
  }

  @Test
  @DisplayName("higher priority runs first and can cancel before lower priority")
  void higherPriorityRunsFirst() {
    List<String> order = new ArrayList<>();

    // Low priority - won't run if HIGH cancels
    PlayerJoinCallback.EVENT.register(EventPriority.LOW, player -> {
      order.add("LOW");
      return EventResult.PASS;
    });

    // High priority runs first and blocks bots
    PlayerJoinCallback.EVENT.register(EventPriority.HIGH, player -> {
      order.add("HIGH");
      if (player.getDisplayName().startsWith("Bot_")) {
        return EventResult.CANCEL;
      }
      return EventResult.PASS;
    });

    TestPlayer bot = new TestPlayer("Bot_Spammer");
    EventResult result = PlayerJoinCallback.EVENT.invoker().onPlayerJoin(bot);

    assertTrue(result.isCancelled());
    assertEquals(List.of("HIGH"), order); // LOW never ran
  }

  @Test
  @DisplayName("should not execute lower priority listeners after cancellation")
  void shouldStopExecutionOnCancellation() {
    List<String> executed = new ArrayList<>();

    PlayerJoinCallback.EVENT.register(EventPriority.HIGHEST, player -> {
      executed.add("HIGHEST");
      return EventResult.CANCEL; // Cancel the event
    });

    PlayerJoinCallback.EVENT.register(EventPriority.NORMAL, player -> {
      executed.add("NORMAL");
      return EventResult.PASS;
    });

    TestPlayer player = new TestPlayer("TestUser");
    EventResult result = PlayerJoinCallback.EVENT.invoker().onPlayerJoin(player);

    assertTrue(result.isCancelled());
    assertEquals(List.of("HIGHEST"), executed); // NORMAL never ran
  }

  @Test
  @DisplayName("should allow multiple listeners at the same priority")
  void shouldAllowMultipleListenersAtSamePriority() {
    List<String> executed = new ArrayList<>();

    PlayerJoinCallback.EVENT.register(EventPriority.NORMAL, player -> {
      executed.add("first");
      return EventResult.PASS;
    });

    PlayerJoinCallback.EVENT.register(EventPriority.NORMAL, player -> {
      executed.add("second");
      return EventResult.PASS;
    });

    PlayerJoinCallback.EVENT.register(EventPriority.NORMAL, player -> {
      executed.add("third");
      return EventResult.PASS;
    });

    TestPlayer player = new TestPlayer("TestUser");
    PlayerJoinCallback.EVENT.invoker().onPlayerJoin(player);

    assertEquals(List.of("first", "second", "third"), executed);
  }

  @Test
  @DisplayName("should return PASS when no listeners are registered")
  void shouldReturnPassWithNoListeners() {
    TestPlayer player = new TestPlayer("TestUser");
    EventResult result = PlayerJoinCallback.EVENT.invoker().onPlayerJoin(player);

    assertFalse(result.isCancelled());
    assertEquals(EventResult.PASS, result);
  }
}
