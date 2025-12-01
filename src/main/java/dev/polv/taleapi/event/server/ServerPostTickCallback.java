package dev.polv.taleapi.event.server;

import dev.polv.taleapi.event.Event;
import dev.polv.taleapi.server.TaleServer;

/**
 * Called at the end of each server tick, after tick processing completes.
 * <p>
 * This event fires at the end of each server tick (typically 20 times
 * per second), providing the current tick number. Use this for tasks that
 * need to run after the main tick logic executes, such as cleanup,
 * synchronization, or deferred updates.
 * </p>
 * <p>
 * This event is NOT cancellable. The server tick has already completed
 * when this event fires.
 * </p>
 *
 * <h2>Example Usage</h2>
 * 
 * <pre>{@code
 * ServerPostTickCallback.EVENT.register((server, tick) -> {
 *   // Run every second (20 ticks) after tick processing
 *   if (tick % 20 == 0) {
 *     flushPendingChanges();
 *   }
 * });
 * }</pre>
 *
 * <h2>Performance Considerations</h2>
 * <p>
 * Since this event fires frequently, keep listener logic lightweight.
 * Avoid heavy computations or blocking operations in tick handlers.
 * </p>
 *
 * @see ServerPreTickCallback for running logic before tick processing
 */
@FunctionalInterface
public interface ServerPostTickCallback {

  /**
   * The event instance. Use this to register listeners and fire the event.
   */
  Event<ServerPostTickCallback> EVENT = Event.create(
      callbacks -> (server, tick) -> {
        for (ServerPostTickCallback callback : callbacks) {
          callback.onPostTick(server, tick);
        }
      },
      (server, tick) -> {} // Empty invoker - no listeners, do nothing
  );

  /**
   * Called at the end of each server tick, after tick processing completes.
   *
   * @param server the server instance
   * @param tick   the current tick number (starts at 0, increments each tick)
   */
  void onPostTick(TaleServer server, long tick);
}

