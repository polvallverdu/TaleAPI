package dev.polv.taleapi.event.entity;

import dev.polv.taleapi.entity.TaleEntity;
import dev.polv.taleapi.event.Event;
import dev.polv.taleapi.event.EventResult;
import dev.polv.taleapi.item.TaleItemStack;
import dev.polv.taleapi.world.Location;

/**
 * Called when an entity drops an item.
 * <p>
 * This event fires whenever an entity (including players) drops items into the
 * world,
 * whether intentionally (player pressing drop key), on death, or through other
 * means.
 * </p>
 * <p>
 * This event is cancellable. If cancelled, the item will not be dropped.
 * </p>
 *
 * <h2>Example Usage</h2>
 * 
 * <pre>{@code
 * ItemDropCallback.EVENT.register((entity, itemStack, location) -> {
 *   System.out.println(entity.getUniqueId() + " dropped " +
 *       itemStack.getAmount() + "x " + itemStack.getItem().getId());
 *   return EventResult.PASS;
 * });
 *
 * // Prevent players from dropping certain items
 * ItemDropCallback.EVENT.register(EventPriority.HIGH, (entity, itemStack, location) -> {
 *   if (entity instanceof TalePlayer && isValuableItem(itemStack.getItem())) {
 *     ((TalePlayer) entity).sendMessage("You cannot drop this item!");
 *     return EventResult.CANCEL;
 *   }
 *   return EventResult.PASS;
 * });
 *
 * // Log all drops in a specific area
 * ItemDropCallback.EVENT.register((entity, itemStack, location) -> {
 *   if (isInLoggedZone(location)) {
 *     logDrop(entity, itemStack, location);
 *   }
 *   return EventResult.PASS;
 * });
 * }</pre>
 *
 * @see TaleItemStack
 */
@FunctionalInterface
public interface ItemDropCallback {

  /**
   * The event instance. Use this to register listeners and fire the event.
   */
  Event<ItemDropCallback> EVENT = Event.create(
      callbacks -> (entity, itemStack, location) -> {
        for (ItemDropCallback callback : callbacks) {
          EventResult result = callback.onItemDrop(entity, itemStack, location);
          if (result.shouldStop()) {
            return result;
          }
        }
        return EventResult.PASS;
      },
      (entity, itemStack, location) -> EventResult.PASS // Empty invoker - no listeners, just pass
  );

  /**
   * Called when an entity drops an item.
   *
   * @param entity    the entity dropping the item
   * @param itemStack the item stack being dropped
   * @param location  the location where the item will be dropped
   * @return the event result - {@link EventResult#CANCEL} to prevent the drop
   */
  EventResult onItemDrop(TaleEntity entity, TaleItemStack itemStack, Location location);
}
