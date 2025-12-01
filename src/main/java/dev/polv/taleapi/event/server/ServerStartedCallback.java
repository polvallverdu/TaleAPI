package dev.polv.taleapi.event.server;

import dev.polv.taleapi.event.Event;
import dev.polv.taleapi.server.TaleServer;

/**
 * Called when the server has fully started.
 * <p>
 * This event fires after the server has completed all initialization
 * and is ready to accept player connections. Use this for tasks that
 * require the server to be fully operational.
 * </p>
 * <p>
 * This event is NOT cancellable. The server is already started
 * when this event fires.
 * </p>
 *
 * <h2>Example Usage</h2>
 * 
 * <pre>{@code
 * ServerStartedCallback.EVENT.register(server -> {
 *   System.out.println("Server has started!");
 *   // Start scheduled tasks, announce server is online, etc.
 * });
 * }</pre>
 */
@FunctionalInterface
public interface ServerStartedCallback {

  /**
   * The event instance. Use this to register listeners and fire the event.
   */
  Event<ServerStartedCallback> EVENT = Event.create(
      callbacks -> server -> {
        for (ServerStartedCallback callback : callbacks) {
          callback.onServerStarted(server);
        }
      },
      server -> {} // Empty invoker - no listeners, do nothing
  );

  /**
   * Called when the server has fully started.
   *
   * @param server the server instance that has started
   */
  void onServerStarted(TaleServer server);
}

