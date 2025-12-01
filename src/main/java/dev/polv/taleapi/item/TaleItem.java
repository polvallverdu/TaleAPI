package dev.polv.taleapi.item;

/**
 * Represents an item type that can exist in inventories.
 * <p>
 * Items are objects that players can hold, use, and store.
 * This interface provides the basic contract for all item types.
 * </p>
 */
public interface TaleItem {

  /**
   * Returns the unique identifier for this item type.
   * <p>
   * Format: {@code namespace:item_name} (e.g., "mymod:ruby_sword")
   * </p>
   *
   * @return the item's identifier
   */
  String getId();

}
