package dev.polv.taleapi.event.server;

import dev.polv.taleapi.event.EventPriority;
import dev.polv.taleapi.testutil.TestServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ServerShutdownCallback")
class ServerShutdownCallbackTest {

  @AfterEach
  void cleanup() {
    ServerShutdownCallback.EVENT.clearListeners();
  }

  @Test
  @DisplayName("should notify listeners when server is shutting down")
  void shouldNotifyOnServerShutdown() {
    List<String> shutdownServers = new ArrayList<>();

    ServerShutdownCallback.EVENT.register(server -> {
      shutdownServers.add(((TestServer) server).getName());
    });

    TestServer server = new TestServer("MyServer");
    ServerShutdownCallback.EVENT.invoker().onServerShutdown(server);

    assertEquals(List.of("MyServer"), shutdownServers);
  }

  @Test
  @DisplayName("should execute listeners in priority order")
  void shouldExecuteInPriorityOrder() {
    List<String> order = new ArrayList<>();

    ServerShutdownCallback.EVENT.register(EventPriority.HIGHEST, server -> {
      order.add("HIGHEST");
    });
    ServerShutdownCallback.EVENT.register(EventPriority.LOWEST, server -> {
      order.add("LOWEST");
    });
    ServerShutdownCallback.EVENT.register(EventPriority.NORMAL, server -> {
      order.add("NORMAL");
    });

    TestServer server = new TestServer();
    ServerShutdownCallback.EVENT.invoker().onServerShutdown(server);

    assertEquals(List.of("HIGHEST", "NORMAL", "LOWEST"), order);
  }

  @Test
  @DisplayName("should execute all listeners (non-cancellable)")
  void shouldExecuteAllListeners() {
    List<String> executed = new ArrayList<>();

    ServerShutdownCallback.EVENT.register(EventPriority.HIGHEST, server -> {
      executed.add("HIGHEST");
    });

    ServerShutdownCallback.EVENT.register(EventPriority.NORMAL, server -> {
      executed.add("NORMAL");
    });

    ServerShutdownCallback.EVENT.register(EventPriority.LOWEST, server -> {
      executed.add("LOWEST");
    });

    TestServer server = new TestServer();
    ServerShutdownCallback.EVENT.invoker().onServerShutdown(server);

    // All listeners should execute since the event is not cancellable
    assertEquals(List.of("HIGHEST", "NORMAL", "LOWEST"), executed);
  }

  @Test
  @DisplayName("should allow multiple listeners at the same priority")
  void shouldAllowMultipleListenersAtSamePriority() {
    List<String> executed = new ArrayList<>();

    ServerShutdownCallback.EVENT.register(EventPriority.NORMAL, server -> {
      executed.add("first");
    });

    ServerShutdownCallback.EVENT.register(EventPriority.NORMAL, server -> {
      executed.add("second");
    });

    ServerShutdownCallback.EVENT.register(EventPriority.NORMAL, server -> {
      executed.add("third");
    });

    TestServer server = new TestServer();
    ServerShutdownCallback.EVENT.invoker().onServerShutdown(server);

    assertEquals(List.of("first", "second", "third"), executed);
  }

  @Test
  @DisplayName("should do nothing when no listeners are registered")
  void shouldDoNothingWithNoListeners() {
    TestServer server = new TestServer();
    // Should not throw any exception
    assertDoesNotThrow(() -> ServerShutdownCallback.EVENT.invoker().onServerShutdown(server));
  }

  @Test
  @DisplayName("should pass the correct server instance to listeners")
  void shouldPassCorrectServerInstance() {
    TestServer[] receivedServer = new TestServer[1];

    ServerShutdownCallback.EVENT.register(server -> {
      receivedServer[0] = (TestServer) server;
    });

    TestServer server = new TestServer("UniqueServer");
    ServerShutdownCallback.EVENT.invoker().onServerShutdown(server);

    assertSame(server, receivedServer[0]);
    assertEquals("UniqueServer", receivedServer[0].getName());
  }

  @Test
  @DisplayName("should allow cleanup tasks during shutdown")
  void shouldAllowCleanupTasks() {
    List<String> cleanupOrder = new ArrayList<>();

    // Simulate cleanup order: save data first, close connections last
    ServerShutdownCallback.EVENT.register(EventPriority.HIGHEST, server -> {
      cleanupOrder.add("save_player_data");
    });

    ServerShutdownCallback.EVENT.register(EventPriority.HIGH, server -> {
      cleanupOrder.add("save_world_data");
    });

    ServerShutdownCallback.EVENT.register(EventPriority.NORMAL, server -> {
      cleanupOrder.add("notify_plugins");
    });

    ServerShutdownCallback.EVENT.register(EventPriority.LOWEST, server -> {
      cleanupOrder.add("close_connections");
    });

    TestServer server = new TestServer();
    ServerShutdownCallback.EVENT.invoker().onServerShutdown(server);

    assertEquals(
        List.of("save_player_data", "save_world_data", "notify_plugins", "close_connections"),
        cleanupOrder
    );
  }
}

