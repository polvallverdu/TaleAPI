package dev.polv.taleapi.event.server;

import dev.polv.taleapi.event.EventPriority;
import dev.polv.taleapi.testutil.TestServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ServerStartingCallback")
class ServerStartingCallbackTest {

  @AfterEach
  void cleanup() {
    ServerStartingCallback.EVENT.clearListeners();
  }

  @Test
  @DisplayName("should notify listeners when server is starting")
  void shouldNotifyOnServerStarting() {
    List<String> startingServers = new ArrayList<>();

    ServerStartingCallback.EVENT.register(server -> {
      startingServers.add(((TestServer) server).getName());
    });

    TestServer server = new TestServer("MyServer");
    ServerStartingCallback.EVENT.invoker().onServerStarting(server);

    assertEquals(List.of("MyServer"), startingServers);
  }

  @Test
  @DisplayName("should execute listeners in priority order")
  void shouldExecuteInPriorityOrder() {
    List<String> order = new ArrayList<>();

    ServerStartingCallback.EVENT.register(EventPriority.HIGHEST, server -> {
      order.add("HIGHEST");
    });
    ServerStartingCallback.EVENT.register(EventPriority.LOWEST, server -> {
      order.add("LOWEST");
    });
    ServerStartingCallback.EVENT.register(EventPriority.NORMAL, server -> {
      order.add("NORMAL");
    });

    TestServer server = new TestServer();
    ServerStartingCallback.EVENT.invoker().onServerStarting(server);

    assertEquals(List.of("HIGHEST", "NORMAL", "LOWEST"), order);
  }

  @Test
  @DisplayName("should execute all listeners (non-cancellable)")
  void shouldExecuteAllListeners() {
    List<String> executed = new ArrayList<>();

    ServerStartingCallback.EVENT.register(EventPriority.HIGHEST, server -> {
      executed.add("HIGHEST");
    });

    ServerStartingCallback.EVENT.register(EventPriority.NORMAL, server -> {
      executed.add("NORMAL");
    });

    ServerStartingCallback.EVENT.register(EventPriority.LOWEST, server -> {
      executed.add("LOWEST");
    });

    TestServer server = new TestServer();
    ServerStartingCallback.EVENT.invoker().onServerStarting(server);

    // All listeners should execute since the event is not cancellable
    assertEquals(List.of("HIGHEST", "NORMAL", "LOWEST"), executed);
  }

  @Test
  @DisplayName("should allow multiple listeners at the same priority")
  void shouldAllowMultipleListenersAtSamePriority() {
    List<String> executed = new ArrayList<>();

    ServerStartingCallback.EVENT.register(EventPriority.NORMAL, server -> {
      executed.add("first");
    });

    ServerStartingCallback.EVENT.register(EventPriority.NORMAL, server -> {
      executed.add("second");
    });

    ServerStartingCallback.EVENT.register(EventPriority.NORMAL, server -> {
      executed.add("third");
    });

    TestServer server = new TestServer();
    ServerStartingCallback.EVENT.invoker().onServerStarting(server);

    assertEquals(List.of("first", "second", "third"), executed);
  }

  @Test
  @DisplayName("should do nothing when no listeners are registered")
  void shouldDoNothingWithNoListeners() {
    TestServer server = new TestServer();
    // Should not throw any exception
    assertDoesNotThrow(() -> ServerStartingCallback.EVENT.invoker().onServerStarting(server));
  }

  @Test
  @DisplayName("should pass the correct server instance to listeners")
  void shouldPassCorrectServerInstance() {
    TestServer[] receivedServer = new TestServer[1];

    ServerStartingCallback.EVENT.register(server -> {
      receivedServer[0] = (TestServer) server;
    });

    TestServer server = new TestServer("UniqueServer");
    ServerStartingCallback.EVENT.invoker().onServerStarting(server);

    assertSame(server, receivedServer[0]);
    assertEquals("UniqueServer", receivedServer[0].getName());
  }
}
