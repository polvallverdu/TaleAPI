package dev.polv.taleapi.testutil;

import dev.polv.taleapi.block.TaleBlock;

/**
 * Test implementation of TaleBlock for testing purposes.
 * <p>
 * This class provides a simple in-memory implementation of the TaleBlock
 * interface that can be used for testing block-related functionality.
 * </p>
 */
public class TestBlock implements TaleBlock {
  private final String id;

  /**
   * Creates a new test block with the specified identifier.
   *
   * @param id the block identifier (e.g., "minecraft:stone")
   */
  public TestBlock(String id) {
    this.id = id;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String toString() {
    return "TestBlock{" +
        "id='" + id + '\'' +
        '}';
  }
}
