package dev.polv.taleapi.event.block;

import dev.polv.taleapi.block.TaleBlock;
import dev.polv.taleapi.entity.TalePlayer;
import dev.polv.taleapi.event.Event;
import dev.polv.taleapi.event.EventResult;
import dev.polv.taleapi.world.Location;

/**
 * Called when a player attempts to place a block.
 * <p>
 * This event is cancellable. If cancelled, the block will not be placed.
 * </p>
 *
 * <h2>Example Usage</h2>
 * 
 * <pre>{@code
 * BlockPlaceCallback.EVENT.register((player, block, location) -> {
 *   System.out.println(player.getDisplayName() + " placed " + block.getId());
 *   return EventResult.PASS;
 * });
 *
 * // Prevent placing blocks in a protected area
 * BlockPlaceCallback.EVENT.register(EventPriority.HIGH, (player, block, location) -> {
 *   if (isProtectedArea(location)) {
 *     player.sendMessage("You cannot build here!");
 *     return EventResult.CANCEL;
 *   }
 *   return EventResult.PASS;
 * });
 * }</pre>
 */
@FunctionalInterface
public interface BlockPlaceCallback {

  /**
   * The event instance. Use this to register listeners and fire the event.
   */
  Event<BlockPlaceCallback> EVENT = Event.create(
      callbacks -> (player, block, location) -> {
        for (BlockPlaceCallback callback : callbacks) {
          EventResult result = callback.onBlockPlace(player, block, location);
          if (result.shouldStop()) {
            return result;
          }
        }
        return EventResult.PASS;
      },
      (player, block, location) -> EventResult.PASS // Empty invoker - no listeners, just pass
  );

  /**
   * Called when a player is about to place a block.
   *
   * @param player   the player placing the block
   * @param block    the block being placed
   * @param location the location where the block will be placed
   * @return the event result - {@link EventResult#CANCEL} to prevent the block from being placed
   */
  EventResult onBlockPlace(TalePlayer player, TaleBlock block, Location location);
}
