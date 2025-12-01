package dev.polv.taleapi.event.server;

import dev.polv.taleapi.event.Event;
import dev.polv.taleapi.server.TaleServer;

/**
 * Called when the server is starting up.
 * <p>
 * This event fires early in the server startup process, before the server
 * is fully initialized and ready to accept connections. Use this for
 * initialization tasks that need to happen during startup.
 * </p>
 * <p>
 * This event is NOT cancellable. The server will continue starting
 * regardless of listener behavior.
 * </p>
 *
 * <h2>Example Usage</h2>
 * 
 * <pre>{@code
 * ServerStartingCallback.EVENT.register(server -> {
 *   System.out.println("Server is starting up...");
 *   // Initialize your plugin resources here
 * });
 * }</pre>
 */
@FunctionalInterface
public interface ServerStartingCallback {

  /**
   * The event instance. Use this to register listeners and fire the event.
   */
  Event<ServerStartingCallback> EVENT = Event.create(
      callbacks -> server -> {
        for (ServerStartingCallback callback : callbacks) {
          callback.onServerStarting(server);
        }
      },
      server -> {} // Empty invoker - no listeners, do nothing
  );

  /**
   * Called when the server is starting up.
   *
   * @param server the server instance that is starting
   */
  void onServerStarting(TaleServer server);
}

