package dev.polv.taleapi.event.player;

import dev.polv.taleapi.entity.TalePlayer;
import dev.polv.taleapi.event.Event;
import dev.polv.taleapi.event.EventResult;
import dev.polv.taleapi.world.Location;

/**
 * Called when a player moves from one location to another.
 * <p>
 * This event fires specifically for player movement. For general entity
 * movement
 * (including players), see {@code EntityMoveCallback}.
 * </p>
 * <p>
 * This event is cancellable. If cancelled, the player's movement will be
 * reverted to the original location.
 * </p>
 *
 * <h2>Example Usage</h2>
 * 
 * <pre>{@code
 * // Log player movements
 * PlayerMoveCallback.EVENT.register((player, from, to) -> {
 *   System.out.println(player.getDisplayName() + " moved from " + from + " to " + to);
 *   return EventResult.PASS;
 * });
 *
 * // Prevent players from entering a restricted area
 * PlayerMoveCallback.EVENT.register(EventPriority.HIGH, (player, from, to) -> {
 *   if (isRestrictedArea(to) && !hasPermission(player)) {
 *     player.sendMessage("You cannot enter this area!");
 *     return EventResult.CANCEL;
 *   }
 *   return EventResult.PASS;
 * });
 * }</pre>
 */
@FunctionalInterface
public interface PlayerMoveCallback {

  /**
   * The event instance. Use this to register listeners and fire the event.
   */
  Event<PlayerMoveCallback> EVENT = Event.create(
      callbacks -> (player, from, to) -> {
        for (PlayerMoveCallback callback : callbacks) {
          EventResult result = callback.onPlayerMove(player, from, to);
          if (result.shouldStop()) {
            return result;
          }
        }
        return EventResult.PASS;
      },
      (player, from, to) -> EventResult.PASS // Empty invoker - no listeners, just pass
  );

  /**
   * Called when a player moves from one location to another.
   *
   * @param player the player who is moving
   * @param from   the location the player is moving from
   * @param to     the location the player is moving to
   * @return the event result - {@link EventResult#CANCEL} to prevent the movement
   */
  EventResult onPlayerMove(TalePlayer player, Location from, Location to);
}
