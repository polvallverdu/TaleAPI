package dev.polv.taleapi.event.server;

import dev.polv.taleapi.event.Event;
import dev.polv.taleapi.server.TaleServer;

/**
 * Called at the start of each server tick, before tick processing begins.
 * <p>
 * This event fires at the beginning of each server tick (typically 20 times
 * per second), providing the current tick number. Use this for tasks that
 * need to run before the main tick logic executes, such as input processing
 * or state preparation.
 * </p>
 * <p>
 * This event is NOT cancellable. The server tick will always proceed
 * regardless of listener behavior.
 * </p>
 *
 * <h2>Example Usage</h2>
 * 
 * <pre>{@code
 * ServerPreTickCallback.EVENT.register((server, tick) -> {
 *   // Prepare state before tick processing
 *   processQueuedInputs();
 * });
 * }</pre>
 *
 * <h2>Performance Considerations</h2>
 * <p>
 * Since this event fires frequently, keep listener logic lightweight.
 * Avoid heavy computations or blocking operations in tick handlers.
 * </p>
 *
 * @see ServerPostTickCallback for running logic after tick processing
 */
@FunctionalInterface
public interface ServerPreTickCallback {

  /**
   * The event instance. Use this to register listeners and fire the event.
   */
  Event<ServerPreTickCallback> EVENT = Event.create(
      callbacks -> (server, tick) -> {
        for (ServerPreTickCallback callback : callbacks) {
          callback.onPreTick(server, tick);
        }
      },
      (server, tick) -> {} // Empty invoker - no listeners, do nothing
  );

  /**
   * Called at the start of each server tick, before tick processing begins.
   *
   * @param server the server instance
   * @param tick   the current tick number (starts at 0, increments each tick)
   */
  void onPreTick(TaleServer server, long tick);
}

