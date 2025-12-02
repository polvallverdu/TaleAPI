package dev.polv.taleapi.item;

import java.util.Objects;

/**
 * Represents a stack of items in an inventory or in the world.
 * <p>
 * While {@link TaleItem} represents an item type, TaleItemStack represents
 * an actual instance of items with a specific quantity. This is what players
 * hold, store in inventories, and drop in the world.
 * </p>
 *
 * <h2>Example Usage</h2>
 * 
 * <pre>{@code
 * TaleItem diamond = ...;
 * 
 * // Create a stack of 64 diamonds
 * TaleItemStack stack = TaleItemStack.of(diamond, 64);
 * 
 * // Create a single item
 * TaleItemStack singleItem = TaleItemStack.of(diamond);
 * 
 * // Check the stack
 * System.out.println(stack.getItem().getId()); // "minecraft:diamond"
 * System.out.println(stack.getAmount());        // 64
 * }</pre>
 *
 * @see TaleItem
 */
public final class TaleItemStack {

  private final TaleItem item;
  private final int amount;

  private TaleItemStack(TaleItem item, int amount) {
    this.item = Objects.requireNonNull(item, "item cannot be null");
    if (amount <= 0) {
      throw new IllegalArgumentException("amount must be positive, got: " + amount);
    }
    this.amount = amount;
  }

  /**
   * Creates a new item stack with the specified item and amount.
   *
   * @param item   the item type
   * @param amount the stack size (must be positive)
   * @return a new TaleItemStack
   * @throws NullPointerException     if item is null
   * @throws IllegalArgumentException if amount is not positive
   */
  public static TaleItemStack of(TaleItem item, int amount) {
    return new TaleItemStack(item, amount);
  }

  /**
   * Creates a new item stack with a single item.
   *
   * @param item the item type
   * @return a new TaleItemStack with amount 1
   * @throws NullPointerException if item is null
   */
  public static TaleItemStack of(TaleItem item) {
    return new TaleItemStack(item, 1);
  }

  /**
   * Returns the item type of this stack.
   *
   * @return the item type
   */
  public TaleItem getItem() {
    return item;
  }

  /**
   * Returns the number of items in this stack.
   *
   * @return the stack amount
   */
  public int getAmount() {
    return amount;
  }

  /**
   * Creates a new item stack with a different amount.
   * <p>
   * This does not modify the current stack; it returns a new instance.
   * </p>
   *
   * @param newAmount the new amount (must be positive)
   * @return a new TaleItemStack with the specified amount
   * @throws IllegalArgumentException if newAmount is not positive
   */
  public TaleItemStack withAmount(int newAmount) {
    return new TaleItemStack(this.item, newAmount);
  }

  /**
   * Checks if this stack contains the same item type as another stack.
   *
   * @param other the other stack to compare
   * @return {@code true} if both stacks contain the same item type
   */
  public boolean isSameItem(TaleItemStack other) {
    if (other == null) {
      return false;
    }
    return this.item.getId().equals(other.item.getId());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    TaleItemStack that = (TaleItemStack) o;
    return amount == that.amount && item.getId().equals(that.item.getId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(item.getId(), amount);
  }

  @Override
  public String toString() {
    return "TaleItemStack{" +
        "item=" + item.getId() +
        ", amount=" + amount +
        '}';
  }
}
