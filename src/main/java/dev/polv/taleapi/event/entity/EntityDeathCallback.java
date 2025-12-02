package dev.polv.taleapi.event.entity;

import dev.polv.taleapi.entity.TaleEntity;
import dev.polv.taleapi.event.Event;
import dev.polv.taleapi.event.EventResult;

/**
 * Called when any entity dies, including players.
 * <p>
 * This event fires for all entity types. For player-specific death handling,
 * use {@link dev.polv.taleapi.event.player.PlayerDeathCallback} instead.
 * Note that for player deaths, BOTH this event and PlayerDeathCallback will
 * fire.
 * </p>
 * <p>
 * This event is cancellable. If cancelled, the death will be prevented
 * (the entity survives).
 * </p>
 *
 * <h2>Example Usage</h2>
 * 
 * <pre>{@code
 * EntityDeathCallback.EVENT.register((entity, cause) -> {
 *   System.out.println(entity.getUniqueId() + " died from " + cause.getType());
 *   return EventResult.PASS;
 * });
 *
 * // Track player kills
 * EntityDeathCallback.EVENT.register((entity, cause) -> {
 *   if (cause.isPlayerKill()) {
 *     TalePlayer killer = (TalePlayer) cause.getKiller();
 *     incrementKillCount(killer);
 *   }
 *   return EventResult.PASS;
 * });
 *
 * // Prevent deaths in safe zones
 * EntityDeathCallback.EVENT.register(EventPriority.HIGH, (entity, cause) -> {
 *   if (isInSafeZone(entity.getLocation())) {
 *     return EventResult.CANCEL; // Prevent death
 *   }
 *   return EventResult.PASS;
 * });
 * }</pre>
 *
 * @see DeathCause
 * @see dev.polv.taleapi.event.player.PlayerDeathCallback
 */
@FunctionalInterface
public interface EntityDeathCallback {

  /**
   * The event instance. Use this to register listeners and fire the event.
   */
  Event<EntityDeathCallback> EVENT = Event.create(
      callbacks -> (entity, cause) -> {
        for (EntityDeathCallback callback : callbacks) {
          EventResult result = callback.onEntityDeath(entity, cause);
          if (result.shouldStop()) {
            return result;
          }
        }
        return EventResult.PASS;
      },
      (entity, cause) -> EventResult.PASS // Empty invoker - no listeners, just pass
  );

  /**
   * Called when an entity dies.
   *
   * @param entity the entity that died
   * @param cause  the cause of death, including killer information if applicable
   * @return the event result - {@link EventResult#CANCEL} to prevent the death
   */
  EventResult onEntityDeath(TaleEntity entity, DeathCause cause);
}
