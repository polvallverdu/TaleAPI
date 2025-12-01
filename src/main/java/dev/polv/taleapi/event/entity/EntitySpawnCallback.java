package dev.polv.taleapi.event.entity;

import dev.polv.taleapi.entity.TaleEntity;
import dev.polv.taleapi.event.Event;
import dev.polv.taleapi.event.EventResult;
import dev.polv.taleapi.world.Location;

/**
 * Called when a non-player entity spawns in the world.
 * <p>
 * This event fires for mobs, NPCs, and other entities, but NOT for players.
 * For player spawning/joining, use {@code PlayerJoinCallback} instead.
 * </p>
 * <p>
 * This event is cancellable. If cancelled, the entity will not spawn.
 * </p>
 *
 * <h2>Example Usage</h2>
 * 
 * <pre>{@code
 * EntitySpawnCallback.EVENT.register((entity, location) -> {
 *   System.out.println("Entity spawned at " + location);
 *   return EventResult.PASS;
 * });
 *
 * // Prevent hostile mobs from spawning in a safe zone
 * EntitySpawnCallback.EVENT.register(EventPriority.HIGH, (entity, location) -> {
 *   if (isHostile(entity) && isInSafeZone(location)) {
 *     return EventResult.CANCEL;
 *   }
 *   return EventResult.PASS;
 * });
 * }</pre>
 */
@FunctionalInterface
public interface EntitySpawnCallback {

  /**
   * The event instance. Use this to register listeners and fire the event.
   */
  Event<EntitySpawnCallback> EVENT = Event.create(
      callbacks -> (entity, location) -> {
        for (EntitySpawnCallback callback : callbacks) {
          EventResult result = callback.onEntitySpawn(entity, location);
          if (result.shouldStop()) {
            return result;
          }
        }
        return EventResult.PASS;
      },
      (entity, location) -> EventResult.PASS // Empty invoker - no listeners, just pass
  );

  /**
   * Called when an entity is about to spawn in the world.
   *
   * @param entity   the entity that is spawning
   * @param location the location where the entity will spawn
   * @return the event result - {@link EventResult#CANCEL} to prevent the spawn
   */
  EventResult onEntitySpawn(TaleEntity entity, Location location);
}
