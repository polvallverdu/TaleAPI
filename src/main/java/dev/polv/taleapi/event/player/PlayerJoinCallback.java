package dev.polv.taleapi.event.player;

import dev.polv.taleapi.entity.TalePlayer;
import dev.polv.taleapi.event.Event;
import dev.polv.taleapi.event.EventResult;

/**
 * Called when a player joins the server.
 * <p>
 * This event is cancellable. If cancelled, the player will be
 * prevented from joining (kicked).
 * </p>
 *
 * <h2>Example Usage</h2>
 * 
 * <pre>{@code
 * PlayerJoinCallback.EVENT.register(player -> {
 *   player.sendMessage("Welcome to the server, " + player.getName() + "!");
 *   return EventResult.PASS;
 * });
 *
 * // With priority
 * PlayerJoinCallback.EVENT.register(EventPriority.HIGH, player -> {
 *   if (isBanned(player)) {
 *     return EventResult.CANCEL; // Prevent join
 *   }
 *   return EventResult.PASS;
 * });
 * }</pre>
 */
@FunctionalInterface
public interface PlayerJoinCallback {

  /**
   * The event instance. Use this to register listeners and fire the event.
   */
  Event<PlayerJoinCallback> EVENT = Event.create(
      callbacks -> player -> {
        for (PlayerJoinCallback callback : callbacks) {
          EventResult result = callback.onPlayerJoin(player);
          if (result.shouldStop()) {
            return result;
          }
        }
        return EventResult.PASS;
      },
      player -> EventResult.PASS // Empty invoker - no listeners, just pass
  );

  /**
   * Called when a player joins the server.
   *
   * @param player the player who is joining
   * @return the event result - {@link EventResult#CANCEL} to prevent the join
   */
  EventResult onPlayerJoin(TalePlayer player);
}
