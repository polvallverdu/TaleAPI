package dev.polv.taleapi.event.player;

import dev.polv.taleapi.event.EventPriority;
import dev.polv.taleapi.testutil.TestPlayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PlayerJoinCallback")
class PlayerJoinCallbackTest {

  @AfterEach
  void cleanup() {
    // Clear listeners after each test to avoid interference
    PlayerJoinCallback.EVENT.clearListeners();
  }

  @Nested
  @DisplayName("Synchronous Handlers")
  class SynchronousHandlers {

    @Test
    @DisplayName("should notify listeners when player joins")
    void shouldNotifyOnJoin() {
      List<String> joinedPlayers = new ArrayList<>();

      PlayerJoinCallback.EVENT.register(player -> {
        joinedPlayers.add(player.getDisplayName());
        return PlayerJoinResult.pass();
      });

      TestPlayer player = new TestPlayer("TestUser");
      PlayerJoinResult result = PlayerJoinCallback.EVENT.invoker().onPlayerJoin(player).join();

      assertEquals(List.of("TestUser"), joinedPlayers);
      assertFalse(result.isCancelled());
    }

    @Test
    @DisplayName("should allow cancelling player join")
    void shouldAllowCancellingJoin() {
      PlayerJoinCallback.EVENT.register(player -> {
        if (player.getDisplayName().equals("BannedUser")) {
          return PlayerJoinResult.cancel();
        }
        return PlayerJoinResult.pass();
      });

      TestPlayer bannedPlayer = new TestPlayer("BannedUser");
      PlayerJoinResult result = PlayerJoinCallback.EVENT.invoker().onPlayerJoin(bannedPlayer).join();

      assertTrue(result.isCancelled());
    }

    @Test
    @DisplayName("should execute listeners in priority order")
    void shouldExecuteInPriorityOrder() {
      List<String> order = new ArrayList<>();

      PlayerJoinCallback.EVENT.register(EventPriority.HIGHEST, player -> {
        order.add("HIGHEST");
        return PlayerJoinResult.pass();
      });
      PlayerJoinCallback.EVENT.register(EventPriority.LOWEST, player -> {
        order.add("LOWEST");
        return PlayerJoinResult.pass();
      });
      PlayerJoinCallback.EVENT.register(EventPriority.NORMAL, player -> {
        order.add("NORMAL");
        return PlayerJoinResult.pass();
      });

      TestPlayer player = new TestPlayer("Player");
      PlayerJoinCallback.EVENT.invoker().onPlayerJoin(player).join();

      assertEquals(List.of("HIGHEST", "NORMAL", "LOWEST"), order);
    }

    @Test
    @DisplayName("higher priority runs first and can cancel before lower priority")
    void higherPriorityRunsFirst() {
      List<String> order = new ArrayList<>();

      // Low priority - won't run if HIGH cancels
      PlayerJoinCallback.EVENT.register(EventPriority.LOW, player -> {
        order.add("LOW");
        return PlayerJoinResult.pass();
      });

      // High priority runs first and blocks bots
      PlayerJoinCallback.EVENT.register(EventPriority.HIGH, player -> {
        order.add("HIGH");
        if (player.getDisplayName().startsWith("Bot_")) {
          return PlayerJoinResult.cancel();
        }
        return PlayerJoinResult.pass();
      });

      TestPlayer bot = new TestPlayer("Bot_Spammer");
      PlayerJoinResult result = PlayerJoinCallback.EVENT.invoker().onPlayerJoin(bot).join();

      assertTrue(result.isCancelled());
      assertEquals(List.of("HIGH"), order); // LOW never ran
    }

    @Test
    @DisplayName("should not execute lower priority listeners after cancellation")
    void shouldStopExecutionOnCancellation() {
      List<String> executed = new ArrayList<>();

      PlayerJoinCallback.EVENT.register(EventPriority.HIGHEST, player -> {
        executed.add("HIGHEST");
        return PlayerJoinResult.cancel(); // Cancel the event
      });

      PlayerJoinCallback.EVENT.register(EventPriority.NORMAL, player -> {
        executed.add("NORMAL");
        return PlayerJoinResult.pass();
      });

      TestPlayer player = new TestPlayer("TestUser");
      PlayerJoinResult result = PlayerJoinCallback.EVENT.invoker().onPlayerJoin(player).join();

      assertTrue(result.isCancelled());
      assertEquals(List.of("HIGHEST"), executed); // NORMAL never ran
    }

    @Test
    @DisplayName("should allow multiple listeners at the same priority")
    void shouldAllowMultipleListenersAtSamePriority() {
      List<String> executed = new ArrayList<>();

      PlayerJoinCallback.EVENT.register(EventPriority.NORMAL, player -> {
        executed.add("first");
        return PlayerJoinResult.pass();
      });

      PlayerJoinCallback.EVENT.register(EventPriority.NORMAL, player -> {
        executed.add("second");
        return PlayerJoinResult.pass();
      });

      PlayerJoinCallback.EVENT.register(EventPriority.NORMAL, player -> {
        executed.add("third");
        return PlayerJoinResult.pass();
      });

      TestPlayer player = new TestPlayer("TestUser");
      PlayerJoinCallback.EVENT.invoker().onPlayerJoin(player).join();

      assertEquals(List.of("first", "second", "third"), executed);
    }

    @Test
    @DisplayName("should return PASS when no listeners are registered")
    void shouldReturnPassWithNoListeners() {
      TestPlayer player = new TestPlayer("TestUser");
      PlayerJoinResult result = PlayerJoinCallback.EVENT.invoker().onPlayerJoin(player).join();

      assertFalse(result.isCancelled());
    }
  }

  @Nested
  @DisplayName("Asynchronous Handlers")
  class AsynchronousHandlers {

    @Test
    @DisplayName("should handle async handler that passes")
    void shouldHandleAsyncPass() {
      PlayerJoinCallback.EVENT.register(player -> CompletableFuture.supplyAsync(() -> {
        // Simulate async work
        try {
          Thread.sleep(10);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        return PlayerJoinResult.passed();
      }));

      TestPlayer player = new TestPlayer("AsyncUser");
      PlayerJoinResult result = PlayerJoinCallback.EVENT.invoker().onPlayerJoin(player).join();

      assertFalse(result.isCancelled());
    }

    @Test
    @DisplayName("should handle async handler that cancels")
    void shouldHandleAsyncCancel() {
      PlayerJoinCallback.EVENT.register(player -> CompletableFuture.supplyAsync(PlayerJoinResult::cancelled));

      TestPlayer player = new TestPlayer("BannedUser");
      PlayerJoinResult result = PlayerJoinCallback.EVENT.invoker().onPlayerJoin(player).join();

      assertTrue(result.isCancelled());
    }

    @Test
    @DisplayName("should maintain priority order with async handlers")
    void shouldMaintainPriorityOrderWithAsync() {
      List<String> order = new ArrayList<>();

      PlayerJoinCallback.EVENT.register(EventPriority.HIGHEST, player -> CompletableFuture.supplyAsync(() -> {
        order.add("HIGHEST-async");
        return PlayerJoinResult.passed();
      }));

      PlayerJoinCallback.EVENT.register(EventPriority.NORMAL, player -> {
        order.add("NORMAL-sync");
        return PlayerJoinResult.pass();
      });

      PlayerJoinCallback.EVENT.register(EventPriority.LOWEST, player -> CompletableFuture.supplyAsync(() -> {
        order.add("LOWEST-async");
        return PlayerJoinResult.passed();
      }));

      TestPlayer player = new TestPlayer("MixedUser");
      PlayerJoinCallback.EVENT.invoker().onPlayerJoin(player).join();

      assertEquals(List.of("HIGHEST-async", "NORMAL-sync", "LOWEST-async"), order);
    }

    @Test
    @DisplayName("async handler cancellation should stop subsequent handlers")
    void asyncCancellationShouldStopSubsequent() {
      List<String> executed = new ArrayList<>();

      PlayerJoinCallback.EVENT.register(EventPriority.HIGHEST, player -> CompletableFuture.supplyAsync(() -> {
        executed.add("HIGHEST");
        return PlayerJoinResult.cancelled();
      }));

      PlayerJoinCallback.EVENT.register(EventPriority.NORMAL, player -> {
        executed.add("NORMAL");
        return PlayerJoinResult.pass();
      });

      TestPlayer player = new TestPlayer("TestUser");
      PlayerJoinResult result = PlayerJoinCallback.EVENT.invoker().onPlayerJoin(player).join();

      assertTrue(result.isCancelled());
      assertEquals(List.of("HIGHEST"), executed); // NORMAL never ran
    }

    @Test
    @DisplayName("should handle CompletableFuture with thenApply")
    void shouldHandleCompletableFutureWithThenApply() {
      AtomicBoolean handlerRan = new AtomicBoolean(false);

      PlayerJoinCallback.EVENT.register(player -> CompletableFuture.supplyAsync(() -> {
        handlerRan.set(true);
        return PlayerJoinResult.passed();
      }));

      TestPlayer player = new TestPlayer("FutureUser");
      PlayerJoinResult result = PlayerJoinCallback.EVENT.invoker().onPlayerJoin(player).join();

      assertTrue(handlerRan.get());
      assertFalse(result.isCancelled());
    }

    @Test
    @DisplayName("should handle async with custom executor")
    void shouldHandleAsyncWithCustomExecutor() throws Exception {
      var executor = Executors.newSingleThreadExecutor();
      AtomicBoolean handlerRan = new AtomicBoolean(false);

      try {
        PlayerJoinCallback.EVENT.register(player -> CompletableFuture.supplyAsync(() -> {
          handlerRan.set(true);
          return PlayerJoinResult.passed();
        }, executor));

        TestPlayer player = new TestPlayer("CustomExecutorUser");
        PlayerJoinResult result = PlayerJoinCallback.EVENT.invoker().onPlayerJoin(player).join();

        assertTrue(handlerRan.get());
        assertFalse(result.isCancelled());
      } finally {
        executor.shutdown();
      }
    }

    @Test
    @DisplayName("should properly chain multiple async handlers")
    void shouldChainMultipleAsyncHandlers() {
      AtomicInteger counter = new AtomicInteger(0);

      for (int i = 0; i < 5; i++) {
        PlayerJoinCallback.EVENT.register(player -> CompletableFuture.supplyAsync(() -> {
          counter.incrementAndGet();
          return PlayerJoinResult.passed();
        }));
      }

      TestPlayer player = new TestPlayer("ChainUser");
      PlayerJoinResult result = PlayerJoinCallback.EVENT.invoker().onPlayerJoin(player).join();

      assertEquals(5, counter.get());
      assertFalse(result.isCancelled());
    }

    @Test
    @DisplayName("should wait for slow async handler before continuing")
    void shouldWaitForSlowAsyncHandler() throws Exception {
      List<Integer> order = new ArrayList<>();
      CountDownLatch latch = new CountDownLatch(1);

      // First handler is slow
      PlayerJoinCallback.EVENT.register(EventPriority.HIGHEST, player -> CompletableFuture.supplyAsync(() -> {
        try {
          Thread.sleep(50);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        order.add(1);
        latch.countDown();
        return PlayerJoinResult.passed();
      }));

      // Second handler should wait for first
      PlayerJoinCallback.EVENT.register(EventPriority.NORMAL, player -> {
        order.add(2);
        return PlayerJoinResult.pass();
      });

      TestPlayer player = new TestPlayer("SlowUser");
      PlayerJoinCallback.EVENT.invoker().onPlayerJoin(player).join();

      assertTrue(latch.await(1, TimeUnit.SECONDS));
      assertEquals(List.of(1, 2), order);
    }
  }

  @Nested
  @DisplayName("Kick Message")
  class KickMessageTests {

    @Test
    @DisplayName("should return empty kick message when cancelled without message")
    void shouldReturnEmptyKickMessageWhenNoMessage() {
      PlayerJoinCallback.EVENT.register(player -> PlayerJoinResult.cancel());

      TestPlayer player = new TestPlayer("BannedUser");
      PlayerJoinResult result = PlayerJoinCallback.EVENT.invoker().onPlayerJoin(player).join();

      assertTrue(result.isCancelled());
      assertTrue(result.getKickMessage().isEmpty());
    }

    @Test
    @DisplayName("should return custom kick message when cancelled with message")
    void shouldReturnCustomKickMessage() {
      String kickMessage = "You are banned from this server!";
      PlayerJoinCallback.EVENT.register(player -> PlayerJoinResult.cancel(kickMessage));

      TestPlayer player = new TestPlayer("BannedUser");
      PlayerJoinResult result = PlayerJoinCallback.EVENT.invoker().onPlayerJoin(player).join();

      assertTrue(result.isCancelled());
      assertTrue(result.getKickMessage().isPresent());
      assertEquals(kickMessage, result.getKickMessage().get());
    }

    @Test
    @DisplayName("should return empty kick message when event passes")
    void shouldReturnEmptyKickMessageOnPass() {
      PlayerJoinCallback.EVENT.register(player -> PlayerJoinResult.pass());

      TestPlayer player = new TestPlayer("NormalUser");
      PlayerJoinResult result = PlayerJoinCallback.EVENT.invoker().onPlayerJoin(player).join();

      assertFalse(result.isCancelled());
      assertTrue(result.getKickMessage().isEmpty());
    }

    @Test
    @DisplayName("should preserve kick message from async handler")
    void shouldPreserveKickMessageFromAsyncHandler() {
      String kickMessage = "Async ban check failed!";
      PlayerJoinCallback.EVENT.register(player -> CompletableFuture.supplyAsync(() ->
          PlayerJoinResult.cancelled(kickMessage)
      ));

      TestPlayer player = new TestPlayer("AsyncBannedUser");
      PlayerJoinResult result = PlayerJoinCallback.EVENT.invoker().onPlayerJoin(player).join();

      assertTrue(result.isCancelled());
      assertTrue(result.getKickMessage().isPresent());
      assertEquals(kickMessage, result.getKickMessage().get());
    }

    @Test
    @DisplayName("should preserve kick message from higher priority handler")
    void shouldPreserveKickMessageFromHigherPriority() {
      String expectedMessage = "VIP only server!";

      PlayerJoinCallback.EVENT.register(EventPriority.HIGHEST, player ->
          PlayerJoinResult.cancel(expectedMessage));

      // This won't run because HIGHEST cancelled
      PlayerJoinCallback.EVENT.register(EventPriority.NORMAL, player ->
          PlayerJoinResult.cancel("Different message"));

      TestPlayer player = new TestPlayer("NonVIPUser");
      PlayerJoinResult result = PlayerJoinCallback.EVENT.invoker().onPlayerJoin(player).join();

      assertTrue(result.isCancelled());
      assertTrue(result.getKickMessage().isPresent());
      assertEquals(expectedMessage, result.getKickMessage().get());
    }

    @Test
    @DisplayName("cancelled() static method should create result without message")
    void cancelledStaticMethodShouldNotHaveMessage() {
      PlayerJoinResult result = PlayerJoinResult.cancelled();
      assertTrue(result.isCancelled());
      assertTrue(result.getKickMessage().isEmpty());
    }

    @Test
    @DisplayName("cancelled(message) static method should create result with message")
    void cancelledWithMessageStaticMethod() {
      String message = "Test message";
      PlayerJoinResult result = PlayerJoinResult.cancelled(message);
      assertTrue(result.isCancelled());
      assertTrue(result.getKickMessage().isPresent());
      assertEquals(message, result.getKickMessage().get());
    }
  }
}
