package dev.polv.taleapi.event.player;

import dev.polv.taleapi.entity.TalePlayer;
import dev.polv.taleapi.event.Event;

/**
 * Called when a player quits the server.
 * <p>
 * This event is <b>not</b> cancellable - the player is already leaving.
 * Use this event to perform cleanup or notify other players.
 * </p>
 *
 * <h2>Example Usage</h2>
 * 
 * <pre>{@code
 * PlayerQuitCallback.EVENT.register(player -> {
 *   System.out.println(player.getName() + " has left the server.");
 * });
 *
 * // With priority - HIGHEST runs first
 * PlayerQuitCallback.EVENT.register(EventPriority.HIGHEST, player -> {
 *   savePlayerData(player); // Run first to ensure data is saved
 * });
 * }</pre>
 */
@FunctionalInterface
public interface PlayerQuitCallback {

  /**
   * The event instance. Use this to register listeners and fire the event.
   */
  Event<PlayerQuitCallback> EVENT = Event.create(
      callbacks -> player -> {
        for (PlayerQuitCallback callback : callbacks) {
          callback.onPlayerQuit(player);
        }
      },
      player -> {
      } // Empty invoker - no-op when no listeners
  );

  /**
   * Called when a player quits the server.
   *
   * @param player the player who is quitting
   */
  void onPlayerQuit(TalePlayer player);
}
