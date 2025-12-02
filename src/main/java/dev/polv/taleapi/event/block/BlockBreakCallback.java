package dev.polv.taleapi.event.block;

import dev.polv.taleapi.block.TaleBlock;
import dev.polv.taleapi.entity.TalePlayer;
import dev.polv.taleapi.event.Event;
import dev.polv.taleapi.event.EventResult;
import dev.polv.taleapi.world.Location;

/**
 * Called when a player attempts to break a block.
 * <p>
 * This event is cancellable. If cancelled, the block will not be broken.
 * </p>
 *
 * <h2>Example Usage</h2>
 * 
 * <pre>{@code
 * BlockBreakCallback.EVENT.register((player, block, location) -> {
 *   System.out.println(player.getDisplayName() + " broke " + block.getId());
 *   return EventResult.PASS;
 * });
 *
 * // Prevent breaking blocks in a protected area
 * BlockBreakCallback.EVENT.register(EventPriority.HIGH, (player, block, location) -> {
 *   if (isProtectedArea(location)) {
 *     player.sendMessage("This area is protected!");
 *     return EventResult.CANCEL;
 *   }
 *   return EventResult.PASS;
 * });
 * }</pre>
 */
@FunctionalInterface
public interface BlockBreakCallback {

  /**
   * The event instance. Use this to register listeners and fire the event.
   */
  Event<BlockBreakCallback> EVENT = Event.create(
      callbacks -> (player, block, location) -> {
        for (BlockBreakCallback callback : callbacks) {
          EventResult result = callback.onBlockBreak(player, block, location);
          if (result.shouldStop()) {
            return result;
          }
        }
        return EventResult.PASS;
      },
      (player, block, location) -> EventResult.PASS // Empty invoker - no listeners, just pass
  );

  /**
   * Called when a player is about to break a block.
   *
   * @param player   the player breaking the block
   * @param block    the block being broken
   * @param location the location of the block
   * @return the event result - {@link EventResult#CANCEL} to prevent the block from being broken
   */
  EventResult onBlockBreak(TalePlayer player, TaleBlock block, Location location);
}
