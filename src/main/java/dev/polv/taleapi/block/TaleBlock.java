package dev.polv.taleapi.block;

import dev.polv.taleapi.world.Location;

/**
 * Represents a block type in the world.
 * <p>
 * Blocks are the fundamental building units of the world.
 * This interface provides the basic contract for all block types.
 * </p>
 */
public interface TaleBlock {

  /**
   * Returns the unique identifier for this block type.
   * <p>
   * Format: {@code namespace:block_name} (e.g., "mymod:magic_stone")
   * </p>
   *
   * @return the block's identifier
   */
  String getId();

}
