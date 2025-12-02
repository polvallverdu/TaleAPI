package dev.polv.taleapi.event.entity;

import dev.polv.taleapi.event.EventPriority;
import dev.polv.taleapi.event.EventResult;
import dev.polv.taleapi.item.TaleItemStack;
import dev.polv.taleapi.testutil.TestEntity;
import dev.polv.taleapi.testutil.TestItem;
import dev.polv.taleapi.testutil.TestPlayer;
import dev.polv.taleapi.world.Location;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ItemDropCallback")
class ItemDropCallbackTest {

  @AfterEach
  void cleanup() {
    ItemDropCallback.EVENT.clearListeners();
  }

  @Test
  @DisplayName("should notify listeners when item is dropped")
  void shouldNotifyOnItemDrop() {
    List<String> droppedItems = new ArrayList<>();

    ItemDropCallback.EVENT.register((entity, itemStack, location) -> {
      droppedItems.add(itemStack.getItem().getId());
      return EventResult.PASS;
    });

    TestEntity entity = new TestEntity("zombie");
    TestItem item = new TestItem("minecraft:diamond");
    TaleItemStack stack = TaleItemStack.of(item, 3);
    Location location = new Location(10, 64, 20);

    ItemDropCallback.EVENT.invoker().onItemDrop(entity, stack, location);

    assertEquals(1, droppedItems.size());
    assertEquals("minecraft:diamond", droppedItems.get(0));
  }

  @Test
  @DisplayName("should provide correct drop location to listeners")
  void shouldProvideDropLocation() {
    Location[] receivedLocation = new Location[1];

    ItemDropCallback.EVENT.register((entity, itemStack, location) -> {
      receivedLocation[0] = location;
      return EventResult.PASS;
    });

    TestEntity entity = new TestEntity("pig");
    TestItem item = new TestItem("minecraft:porkchop");
    TaleItemStack stack = TaleItemStack.of(item, 2);
    Location dropLocation = new Location(100, 70, -50);

    ItemDropCallback.EVENT.invoker().onItemDrop(entity, stack, dropLocation);

    assertEquals(dropLocation, receivedLocation[0]);
    assertEquals(100, receivedLocation[0].x());
    assertEquals(70, receivedLocation[0].y());
    assertEquals(-50, receivedLocation[0].z());
  }

  @Test
  @DisplayName("should provide correct item stack amount to listeners")
  void shouldProvideItemStackAmount() {
    int[] receivedAmount = new int[1];

    ItemDropCallback.EVENT.register((entity, itemStack, location) -> {
      receivedAmount[0] = itemStack.getAmount();
      return EventResult.PASS;
    });

    TestEntity entity = new TestEntity("cow");
    TestItem item = new TestItem("minecraft:leather");
    TaleItemStack stack = TaleItemStack.of(item, 5);
    Location location = new Location(0, 64, 0);

    ItemDropCallback.EVENT.invoker().onItemDrop(entity, stack, location);

    assertEquals(5, receivedAmount[0]);
  }

  @Test
  @DisplayName("should allow cancelling item drop")
  void shouldAllowCancellingDrop() {
    ItemDropCallback.EVENT.register((entity, itemStack, location) -> {
      // Prevent dropping valuable items
      if (itemStack.getItem().getId().contains("diamond")) {
        return EventResult.CANCEL;
      }
      return EventResult.PASS;
    });

    TestEntity entity = new TestEntity("player_sim");
    TestItem diamond = new TestItem("minecraft:diamond");
    TaleItemStack stack = TaleItemStack.of(diamond, 1);
    Location location = new Location(0, 64, 0);

    EventResult result = ItemDropCallback.EVENT.invoker().onItemDrop(entity, stack, location);

    assertTrue(result.isCancelled());
  }

  @Test
  @DisplayName("should execute listeners in priority order")
  void shouldExecuteInPriorityOrder() {
    List<String> order = new ArrayList<>();

    ItemDropCallback.EVENT.register(EventPriority.HIGHEST, (entity, itemStack, location) -> {
      order.add("HIGHEST");
      return EventResult.PASS;
    });
    ItemDropCallback.EVENT.register(EventPriority.LOWEST, (entity, itemStack, location) -> {
      order.add("LOWEST");
      return EventResult.PASS;
    });
    ItemDropCallback.EVENT.register(EventPriority.NORMAL, (entity, itemStack, location) -> {
      order.add("NORMAL");
      return EventResult.PASS;
    });

    TestEntity entity = new TestEntity("zombie");
    TestItem item = new TestItem("minecraft:rotten_flesh");
    TaleItemStack stack = TaleItemStack.of(item, 1);

    ItemDropCallback.EVENT.invoker().onItemDrop(entity, stack, new Location(0, 0, 0));

    assertEquals(List.of("HIGHEST", "NORMAL", "LOWEST"), order);
  }

  @Test
  @DisplayName("should stop execution on cancellation")
  void shouldStopExecutionOnCancellation() {
    List<String> executed = new ArrayList<>();

    ItemDropCallback.EVENT.register(EventPriority.HIGHEST, (entity, itemStack, location) -> {
      executed.add("HIGHEST");
      return EventResult.CANCEL;
    });

    ItemDropCallback.EVENT.register(EventPriority.NORMAL, (entity, itemStack, location) -> {
      executed.add("NORMAL");
      return EventResult.PASS;
    });

    TestEntity entity = new TestEntity("zombie");
    TestItem item = new TestItem("minecraft:bone");
    TaleItemStack stack = TaleItemStack.of(item, 1);

    EventResult result = ItemDropCallback.EVENT.invoker().onItemDrop(entity, stack, new Location(0, 0, 0));

    assertTrue(result.isCancelled());
    assertEquals(List.of("HIGHEST"), executed);
  }

  @Test
  @DisplayName("should return PASS when no listeners are registered")
  void shouldReturnPassWithNoListeners() {
    TestEntity entity = new TestEntity("zombie");
    TestItem item = new TestItem("minecraft:sword");
    TaleItemStack stack = TaleItemStack.of(item, 1);

    EventResult result = ItemDropCallback.EVENT.invoker().onItemDrop(entity, stack, new Location(0, 0, 0));

    assertFalse(result.isCancelled());
    assertEquals(EventResult.PASS, result);
  }

  @Test
  @DisplayName("should handle player drops")
  void shouldHandlePlayerDrops() {
    List<String> playerDrops = new ArrayList<>();

    ItemDropCallback.EVENT.register((entity, itemStack, location) -> {
      if (entity instanceof TestPlayer player) {
        playerDrops.add(player.getDisplayName() + " dropped " + itemStack.getItem().getId());
      }
      return EventResult.PASS;
    });

    TestPlayer player = new TestPlayer("Steve");
    TestItem item = new TestItem("minecraft:apple");
    TaleItemStack stack = TaleItemStack.of(item, 3);
    Location location = new Location(50, 64, 50);

    ItemDropCallback.EVENT.invoker().onItemDrop(player, stack, location);

    assertEquals(1, playerDrops.size());
    assertEquals("Steve dropped minecraft:apple", playerDrops.get(0));
  }

  @Test
  @DisplayName("should prevent players from dropping certain items")
  void shouldPreventPlayersFromDroppingItems() {
    ItemDropCallback.EVENT.register((entity, itemStack, location) -> {
      if (entity instanceof TestPlayer && itemStack.getItem().getId().equals("minecraft:nether_star")) {
        ((TestPlayer) entity).sendMessage("You cannot drop this item!");
        return EventResult.CANCEL;
      }
      return EventResult.PASS;
    });

    TestPlayer player = new TestPlayer("Player");
    TestItem netherStar = new TestItem("minecraft:nether_star");
    TaleItemStack stack = TaleItemStack.of(netherStar, 1);

    EventResult result = ItemDropCallback.EVENT.invoker().onItemDrop(player, stack, new Location(0, 64, 0));

    assertTrue(result.isCancelled());
    assertEquals("You cannot drop this item!", player.getLastMessage());
  }
}
