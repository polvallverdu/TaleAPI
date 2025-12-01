package dev.polv.taleapi.event.server;

import dev.polv.taleapi.event.EventPriority;
import dev.polv.taleapi.testutil.TestServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ServerPreTickCallback")
class ServerPreTickCallbackTest {

  @AfterEach
  void cleanup() {
    ServerPreTickCallback.EVENT.clearListeners();
  }

  @Test
  @DisplayName("should notify listeners on pre-tick")
  void shouldNotifyOnPreTick() {
    List<Long> receivedTicks = new ArrayList<>();

    ServerPreTickCallback.EVENT.register((server, tick) -> {
      receivedTicks.add(tick);
    });

    TestServer server = new TestServer();
    ServerPreTickCallback.EVENT.invoker().onPreTick(server, 0L);
    ServerPreTickCallback.EVENT.invoker().onPreTick(server, 1L);
    ServerPreTickCallback.EVENT.invoker().onPreTick(server, 2L);

    assertEquals(List.of(0L, 1L, 2L), receivedTicks);
  }

  @Test
  @DisplayName("should execute listeners in priority order")
  void shouldExecuteInPriorityOrder() {
    List<String> order = new ArrayList<>();

    ServerPreTickCallback.EVENT.register(EventPriority.HIGHEST, (server, tick) -> {
      order.add("HIGHEST");
    });
    ServerPreTickCallback.EVENT.register(EventPriority.LOWEST, (server, tick) -> {
      order.add("LOWEST");
    });
    ServerPreTickCallback.EVENT.register(EventPriority.NORMAL, (server, tick) -> {
      order.add("NORMAL");
    });

    TestServer server = new TestServer();
    ServerPreTickCallback.EVENT.invoker().onPreTick(server, 0L);

    assertEquals(List.of("HIGHEST", "NORMAL", "LOWEST"), order);
  }

  @Test
  @DisplayName("should execute all listeners (non-cancellable)")
  void shouldExecuteAllListeners() {
    List<String> executed = new ArrayList<>();

    ServerPreTickCallback.EVENT.register(EventPriority.HIGHEST, (server, tick) -> {
      executed.add("HIGHEST");
    });

    ServerPreTickCallback.EVENT.register(EventPriority.NORMAL, (server, tick) -> {
      executed.add("NORMAL");
    });

    ServerPreTickCallback.EVENT.register(EventPriority.LOWEST, (server, tick) -> {
      executed.add("LOWEST");
    });

    TestServer server = new TestServer();
    ServerPreTickCallback.EVENT.invoker().onPreTick(server, 0L);

    assertEquals(List.of("HIGHEST", "NORMAL", "LOWEST"), executed);
  }

  @Test
  @DisplayName("should allow multiple listeners at the same priority")
  void shouldAllowMultipleListenersAtSamePriority() {
    List<String> executed = new ArrayList<>();

    ServerPreTickCallback.EVENT.register(EventPriority.NORMAL, (server, tick) -> {
      executed.add("first");
    });

    ServerPreTickCallback.EVENT.register(EventPriority.NORMAL, (server, tick) -> {
      executed.add("second");
    });

    ServerPreTickCallback.EVENT.register(EventPriority.NORMAL, (server, tick) -> {
      executed.add("third");
    });

    TestServer server = new TestServer();
    ServerPreTickCallback.EVENT.invoker().onPreTick(server, 0L);

    assertEquals(List.of("first", "second", "third"), executed);
  }

  @Test
  @DisplayName("should do nothing when no listeners are registered")
  void shouldDoNothingWithNoListeners() {
    TestServer server = new TestServer();
    assertDoesNotThrow(() -> ServerPreTickCallback.EVENT.invoker().onPreTick(server, 0L));
  }

  @Test
  @DisplayName("should pass the correct server instance and tick to listeners")
  void shouldPassCorrectServerInstanceAndTick() {
    TestServer[] receivedServer = new TestServer[1];
    long[] receivedTick = new long[1];

    ServerPreTickCallback.EVENT.register((server, tick) -> {
      receivedServer[0] = (TestServer) server;
      receivedTick[0] = tick;
    });

    TestServer server = new TestServer("PreTickServer");
    ServerPreTickCallback.EVENT.invoker().onPreTick(server, 42L);

    assertSame(server, receivedServer[0]);
    assertEquals("PreTickServer", receivedServer[0].getName());
    assertEquals(42L, receivedTick[0]);
  }

  @Test
  @DisplayName("should handle large tick numbers")
  void shouldHandleLargeTickNumbers() {
    long[] receivedTick = new long[1];

    ServerPreTickCallback.EVENT.register((server, tick) -> {
      receivedTick[0] = tick;
    });

    TestServer server = new TestServer();
    long largeTick = Long.MAX_VALUE - 100;
    ServerPreTickCallback.EVENT.invoker().onPreTick(server, largeTick);

    assertEquals(largeTick, receivedTick[0]);
  }
}

