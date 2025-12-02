package dev.polv.taleapi.event.player;

import dev.polv.taleapi.entity.TalePlayer;
import dev.polv.taleapi.event.Event;
import dev.polv.taleapi.event.EventResult;
import dev.polv.taleapi.event.entity.DeathCause;

/**
 * Called when a player dies.
 * <p>
 * This event fires specifically for player deaths. For general entity death
 * handling
 * (including players), use
 * {@link dev.polv.taleapi.event.entity.EntityDeathCallback}.
 * Note that for player deaths, BOTH events will fire.
 * </p>
 * <p>
 * This event is cancellable. If cancelled, the death will be prevented
 * (the player survives).
 * </p>
 *
 * <h2>Example Usage</h2>
 * 
 * <pre>{@code
 * PlayerDeathCallback.EVENT.register((player, cause) -> {
 *   player.sendMessage("You died!");
 *   return EventResult.PASS;
 * });
 *
 * // Notify killer
 * PlayerDeathCallback.EVENT.register((player, cause) -> {
 *   if (cause.isPlayerKill()) {
 *     TalePlayer killer = (TalePlayer) cause.getKiller();
 *     killer.sendMessage("You killed " + player.getDisplayName() + "!");
 *   }
 *   return EventResult.PASS;
 * });
 *
 * // Keep inventory on death
 * PlayerDeathCallback.EVENT.register(EventPriority.HIGH, (player, cause) -> {
 *   if (player.hasPermission("server.keepinventory")) {
 *     // Handle keep inventory logic
 *   }
 *   return EventResult.PASS;
 * });
 * }</pre>
 *
 * @see DeathCause
 * @see dev.polv.taleapi.event.entity.EntityDeathCallback
 */
@FunctionalInterface
public interface PlayerDeathCallback {

  /**
   * The event instance. Use this to register listeners and fire the event.
   */
  Event<PlayerDeathCallback> EVENT = Event.create(
      callbacks -> (player, cause) -> {
        for (PlayerDeathCallback callback : callbacks) {
          EventResult result = callback.onPlayerDeath(player, cause);
          if (result.shouldStop()) {
            return result;
          }
        }
        return EventResult.PASS;
      },
      (player, cause) -> EventResult.PASS // Empty invoker - no listeners, just pass
  );

  /**
   * Called when a player dies.
   *
   * @param player the player who died
   * @param cause  the cause of death, including killer information if applicable
   * @return the event result - {@link EventResult#CANCEL} to prevent the death
   */
  EventResult onPlayerDeath(TalePlayer player, DeathCause cause);
}
