package dev.polv.taleapi.item;

import dev.polv.taleapi.testutil.TestItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TaleItemStack")
class TaleItemStackTest {

  @Test
  @DisplayName("should create item stack with item and amount")
  void shouldCreateItemStackWithAmount() {
    TestItem diamond = new TestItem("minecraft:diamond");
    TaleItemStack stack = TaleItemStack.of(diamond, 64);

    assertEquals(diamond, stack.getItem());
    assertEquals(64, stack.getAmount());
  }

  @Test
  @DisplayName("should create item stack with single item")
  void shouldCreateSingleItemStack() {
    TestItem sword = new TestItem("minecraft:diamond_sword");
    TaleItemStack stack = TaleItemStack.of(sword);

    assertEquals(sword, stack.getItem());
    assertEquals(1, stack.getAmount());
  }

  @Test
  @DisplayName("should throw exception for null item")
  void shouldThrowForNullItem() {
    assertThrows(NullPointerException.class, () -> TaleItemStack.of(null, 1));
    assertThrows(NullPointerException.class, () -> TaleItemStack.of(null));
  }

  @Test
  @DisplayName("should throw exception for non-positive amount")
  void shouldThrowForNonPositiveAmount() {
    TestItem item = new TestItem("minecraft:stone");

    assertThrows(IllegalArgumentException.class, () -> TaleItemStack.of(item, 0));
    assertThrows(IllegalArgumentException.class, () -> TaleItemStack.of(item, -1));
    assertThrows(IllegalArgumentException.class, () -> TaleItemStack.of(item, -100));
  }

  @Test
  @DisplayName("should create new stack with different amount")
  void shouldCreateNewStackWithDifferentAmount() {
    TestItem item = new TestItem("minecraft:gold_ingot");
    TaleItemStack original = TaleItemStack.of(item, 32);
    TaleItemStack modified = original.withAmount(16);

    // Original unchanged
    assertEquals(32, original.getAmount());

    // New stack has new amount
    assertEquals(16, modified.getAmount());
    assertEquals(item, modified.getItem());
  }

  @Test
  @DisplayName("should check if stacks have same item type")
  void shouldCheckSameItemType() {
    TestItem diamond = new TestItem("minecraft:diamond");
    TestItem anotherDiamond = new TestItem("minecraft:diamond");
    TestItem gold = new TestItem("minecraft:gold_ingot");

    TaleItemStack stack1 = TaleItemStack.of(diamond, 32);
    TaleItemStack stack2 = TaleItemStack.of(anotherDiamond, 16);
    TaleItemStack stack3 = TaleItemStack.of(gold, 32);

    assertTrue(stack1.isSameItem(stack2));
    assertFalse(stack1.isSameItem(stack3));
    assertFalse(stack1.isSameItem(null));
  }

  @Test
  @DisplayName("should implement equals correctly")
  void shouldImplementEquals() {
    TestItem diamond = new TestItem("minecraft:diamond");
    TestItem anotherDiamond = new TestItem("minecraft:diamond");

    TaleItemStack stack1 = TaleItemStack.of(diamond, 32);
    TaleItemStack stack2 = TaleItemStack.of(anotherDiamond, 32);
    TaleItemStack stack3 = TaleItemStack.of(diamond, 16);

    assertEquals(stack1, stack2);
    assertNotEquals(stack1, stack3);
    assertNotEquals(stack1, null);
    assertNotEquals(stack1, "not a stack");
  }

  @Test
  @DisplayName("should implement hashCode correctly")
  void shouldImplementHashCode() {
    TestItem diamond = new TestItem("minecraft:diamond");
    TestItem anotherDiamond = new TestItem("minecraft:diamond");

    TaleItemStack stack1 = TaleItemStack.of(diamond, 32);
    TaleItemStack stack2 = TaleItemStack.of(anotherDiamond, 32);

    assertEquals(stack1.hashCode(), stack2.hashCode());
  }

  @Test
  @DisplayName("should provide meaningful toString")
  void shouldProvideMeaningfulToString() {
    TestItem diamond = new TestItem("minecraft:diamond");
    TaleItemStack stack = TaleItemStack.of(diamond, 64);

    String str = stack.toString();
    assertTrue(str.contains("minecraft:diamond"));
    assertTrue(str.contains("64"));
  }
}
