package dev.polv.taleapi.event.server;

import dev.polv.taleapi.event.Event;
import dev.polv.taleapi.server.TaleServer;

/**
 * Called when the server is shutting down.
 * <p>
 * This event fires when the server begins its shutdown sequence.
 * Use this for cleanup tasks, saving data, or graceful disconnection
 * of resources.
 * </p>
 * <p>
 * This event is NOT cancellable. The server will continue shutting
 * down regardless of listener behavior.
 * </p>
 *
 * <h2>Example Usage</h2>
 * 
 * <pre>{@code
 * ServerShutdownCallback.EVENT.register(server -> {
 *   System.out.println("Server is shutting down...");
 *   // Save data, close connections, cleanup resources
 * });
 * }</pre>
 */
@FunctionalInterface
public interface ServerShutdownCallback {

  /**
   * The event instance. Use this to register listeners and fire the event.
   */
  Event<ServerShutdownCallback> EVENT = Event.create(
      callbacks -> server -> {
        for (ServerShutdownCallback callback : callbacks) {
          callback.onServerShutdown(server);
        }
      },
      server -> {} // Empty invoker - no listeners, do nothing
  );

  /**
   * Called when the server is shutting down.
   *
   * @param server the server instance that is shutting down
   */
  void onServerShutdown(TaleServer server);
}

