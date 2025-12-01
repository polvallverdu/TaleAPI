package dev.polv.taleapi.event.entity;

import dev.polv.taleapi.entity.TaleEntity;
import dev.polv.taleapi.event.Event;
import dev.polv.taleapi.event.EventResult;
import dev.polv.taleapi.world.Location;

/**
 * Called when an entity moves from one location to another.
 * <p>
 * This event fires for ALL entities, including players. To check if the
 * moving entity is a player, use {@code entity instanceof TalePlayer}.
 * </p>
 * <p>
 * This event is cancellable. If cancelled, the entity's movement will be
 * reverted to the original location.
 * </p>
 *
 * <h2>Example Usage</h2>
 * 
 * <pre>{@code
 * // Log all entity movements
 * EntityMoveCallback.EVENT.register((entity, from, to) -> {
 *   System.out.println(entity.getUniqueId() + " moved from " + from + " to " + to);
 *   return EventResult.PASS;
 * });
 *
 * // Prevent entities from entering a restricted area
 * EntityMoveCallback.EVENT.register(EventPriority.HIGH, (entity, from, to) -> {
 *   if (isRestrictedArea(to)) {
 *     return EventResult.CANCEL; // Block the movement
 *   }
 *   return EventResult.PASS;
 * });
 *
 * // Player-specific movement handling
 * EntityMoveCallback.EVENT.register((entity, from, to) -> {
 *   if (entity instanceof TalePlayer player) {
 *     // Handle player-specific movement logic
 *     updatePlayerChunks(player, from, to);
 *   }
 *   return EventResult.PASS;
 * });
 * }</pre>
 */
@FunctionalInterface
public interface EntityMoveCallback {

  /**
   * The event instance. Use this to register listeners and fire the event.
   */
  Event<EntityMoveCallback> EVENT = Event.create(
      callbacks -> (entity, from, to) -> {
        for (EntityMoveCallback callback : callbacks) {
          EventResult result = callback.onEntityMove(entity, from, to);
          if (result.shouldStop()) {
            return result;
          }
        }
        return EventResult.PASS;
      },
      (entity, from, to) -> EventResult.PASS // Empty invoker - no listeners, just pass
  );

  /**
   * Called when an entity moves from one location to another.
   *
   * @param entity the entity that is moving
   * @param from   the location the entity is moving from
   * @param to     the location the entity is moving to
   * @return the event result - {@link EventResult#CANCEL} to prevent the movement
   */
  EventResult onEntityMove(TaleEntity entity, Location from, Location to);
}
