package dev.polv.taleapi.testutil;

import dev.polv.taleapi.item.TaleItem;

/**
 * Test implementation of TaleItem for testing purposes.
 * <p>
 * This class provides a simple in-memory implementation of the TaleItem
 * interface that can be used for testing item-related functionality.
 * </p>
 */
public class TestItem implements TaleItem {
  private final String id;

  /**
   * Creates a new test item with the specified ID.
   *
   * @param id the item identifier (e.g., "minecraft:diamond")
   */
  public TestItem(String id) {
    this.id = id;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String toString() {
    return "TestItem{" +
        "id='" + id + '\'' +
        '}';
  }
}
