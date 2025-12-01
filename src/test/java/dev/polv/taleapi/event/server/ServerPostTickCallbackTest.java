package dev.polv.taleapi.event.server;

import dev.polv.taleapi.event.EventPriority;
import dev.polv.taleapi.testutil.TestServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ServerPostTickCallback")
class ServerPostTickCallbackTest {

  @AfterEach
  void cleanup() {
    ServerPostTickCallback.EVENT.clearListeners();
  }

  @Test
  @DisplayName("should notify listeners on post-tick")
  void shouldNotifyOnPostTick() {
    List<Long> receivedTicks = new ArrayList<>();

    ServerPostTickCallback.EVENT.register((server, tick) -> {
      receivedTicks.add(tick);
    });

    TestServer server = new TestServer();
    ServerPostTickCallback.EVENT.invoker().onPostTick(server, 0L);
    ServerPostTickCallback.EVENT.invoker().onPostTick(server, 1L);
    ServerPostTickCallback.EVENT.invoker().onPostTick(server, 2L);

    assertEquals(List.of(0L, 1L, 2L), receivedTicks);
  }

  @Test
  @DisplayName("should execute listeners in priority order")
  void shouldExecuteInPriorityOrder() {
    List<String> order = new ArrayList<>();

    ServerPostTickCallback.EVENT.register(EventPriority.HIGHEST, (server, tick) -> {
      order.add("HIGHEST");
    });
    ServerPostTickCallback.EVENT.register(EventPriority.LOWEST, (server, tick) -> {
      order.add("LOWEST");
    });
    ServerPostTickCallback.EVENT.register(EventPriority.NORMAL, (server, tick) -> {
      order.add("NORMAL");
    });

    TestServer server = new TestServer();
    ServerPostTickCallback.EVENT.invoker().onPostTick(server, 0L);

    assertEquals(List.of("HIGHEST", "NORMAL", "LOWEST"), order);
  }

  @Test
  @DisplayName("should execute all listeners (non-cancellable)")
  void shouldExecuteAllListeners() {
    List<String> executed = new ArrayList<>();

    ServerPostTickCallback.EVENT.register(EventPriority.HIGHEST, (server, tick) -> {
      executed.add("HIGHEST");
    });

    ServerPostTickCallback.EVENT.register(EventPriority.NORMAL, (server, tick) -> {
      executed.add("NORMAL");
    });

    ServerPostTickCallback.EVENT.register(EventPriority.LOWEST, (server, tick) -> {
      executed.add("LOWEST");
    });

    TestServer server = new TestServer();
    ServerPostTickCallback.EVENT.invoker().onPostTick(server, 0L);

    assertEquals(List.of("HIGHEST", "NORMAL", "LOWEST"), executed);
  }

  @Test
  @DisplayName("should allow multiple listeners at the same priority")
  void shouldAllowMultipleListenersAtSamePriority() {
    List<String> executed = new ArrayList<>();

    ServerPostTickCallback.EVENT.register(EventPriority.NORMAL, (server, tick) -> {
      executed.add("first");
    });

    ServerPostTickCallback.EVENT.register(EventPriority.NORMAL, (server, tick) -> {
      executed.add("second");
    });

    ServerPostTickCallback.EVENT.register(EventPriority.NORMAL, (server, tick) -> {
      executed.add("third");
    });

    TestServer server = new TestServer();
    ServerPostTickCallback.EVENT.invoker().onPostTick(server, 0L);

    assertEquals(List.of("first", "second", "third"), executed);
  }

  @Test
  @DisplayName("should do nothing when no listeners are registered")
  void shouldDoNothingWithNoListeners() {
    TestServer server = new TestServer();
    assertDoesNotThrow(() -> ServerPostTickCallback.EVENT.invoker().onPostTick(server, 0L));
  }

  @Test
  @DisplayName("should pass the correct server instance and tick to listeners")
  void shouldPassCorrectServerInstanceAndTick() {
    TestServer[] receivedServer = new TestServer[1];
    long[] receivedTick = new long[1];

    ServerPostTickCallback.EVENT.register((server, tick) -> {
      receivedServer[0] = (TestServer) server;
      receivedTick[0] = tick;
    });

    TestServer server = new TestServer("PostTickServer");
    ServerPostTickCallback.EVENT.invoker().onPostTick(server, 42L);

    assertSame(server, receivedServer[0]);
    assertEquals("PostTickServer", receivedServer[0].getName());
    assertEquals(42L, receivedTick[0]);
  }

  @Test
  @DisplayName("should handle large tick numbers")
  void shouldHandleLargeTickNumbers() {
    long[] receivedTick = new long[1];

    ServerPostTickCallback.EVENT.register((server, tick) -> {
      receivedTick[0] = tick;
    });

    TestServer server = new TestServer();
    long largeTick = Long.MAX_VALUE - 100;
    ServerPostTickCallback.EVENT.invoker().onPostTick(server, largeTick);

    assertEquals(largeTick, receivedTick[0]);
  }

  @Test
  @DisplayName("should support periodic task patterns")
  void shouldSupportPeriodicTaskPatterns() {
    List<Long> everySecondTicks = new ArrayList<>();
    List<Long> everyFiveSecondTicks = new ArrayList<>();

    // 20 ticks = 1 second
    ServerPostTickCallback.EVENT.register((server, tick) -> {
      if (tick % 20 == 0) {
        everySecondTicks.add(tick);
      }
      if (tick % 100 == 0) {
        everyFiveSecondTicks.add(tick);
      }
    });

    TestServer server = new TestServer();
    // Simulate 5 seconds of ticks (100 ticks)
    for (long tick = 0; tick <= 100; tick++) {
      ServerPostTickCallback.EVENT.invoker().onPostTick(server, tick);
    }

    // Should have 6 second boundaries: 0, 20, 40, 60, 80, 100
    assertEquals(List.of(0L, 20L, 40L, 60L, 80L, 100L), everySecondTicks);
    // Should have 2 five-second boundaries: 0, 100
    assertEquals(List.of(0L, 100L), everyFiveSecondTicks);
  }

  @Test
  @DisplayName("pre-tick should fire before post-tick in a complete tick cycle")
  void preTickShouldFireBeforePostTick() {
    List<String> executionOrder = new ArrayList<>();

    ServerPreTickCallback.EVENT.register((server, tick) -> {
      executionOrder.add("PRE-" + tick);
    });

    ServerPostTickCallback.EVENT.register((server, tick) -> {
      executionOrder.add("POST-" + tick);
    });

    TestServer server = new TestServer();

    // Simulate a tick cycle
    ServerPreTickCallback.EVENT.invoker().onPreTick(server, 0L);
    ServerPostTickCallback.EVENT.invoker().onPostTick(server, 0L);
    ServerPreTickCallback.EVENT.invoker().onPreTick(server, 1L);
    ServerPostTickCallback.EVENT.invoker().onPostTick(server, 1L);

    assertEquals(List.of("PRE-0", "POST-0", "PRE-1", "POST-1"), executionOrder);

    // Cleanup
    ServerPreTickCallback.EVENT.clearListeners();
  }
}

