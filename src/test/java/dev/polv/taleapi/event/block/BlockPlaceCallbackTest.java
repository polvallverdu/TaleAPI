package dev.polv.taleapi.event.block;

import dev.polv.taleapi.event.EventPriority;
import dev.polv.taleapi.event.EventResult;
import dev.polv.taleapi.testutil.TestBlock;
import dev.polv.taleapi.testutil.TestPlayer;
import dev.polv.taleapi.world.Location;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BlockPlaceCallback")
class BlockPlaceCallbackTest {

  @AfterEach
  void cleanup() {
    BlockPlaceCallback.EVENT.clearListeners();
  }

  @Test
  @DisplayName("should notify listeners when block is placed")
  void shouldNotifyOnPlace() {
    List<String> placedBlocks = new ArrayList<>();

    BlockPlaceCallback.EVENT.register((player, block, location) -> {
      placedBlocks.add(block.getId());
      return EventResult.PASS;
    });

    TestPlayer player = new TestPlayer("Builder");
    TestBlock block = new TestBlock("minecraft:cobblestone");
    Location location = new Location(10, 65, 20);
    BlockPlaceCallback.EVENT.invoker().onBlockPlace(player, block, location);

    assertEquals(List.of("minecraft:cobblestone"), placedBlocks);
  }

  @Test
  @DisplayName("should provide correct player, block, and location to listeners")
  void shouldProvideCorrectEventData() {
    TestPlayer[] receivedPlayer = new TestPlayer[1];
    TestBlock[] receivedBlock = new TestBlock[1];
    Location[] receivedLocation = new Location[1];

    BlockPlaceCallback.EVENT.register((player, block, location) -> {
      receivedPlayer[0] = (TestPlayer) player;
      receivedBlock[0] = (TestBlock) block;
      receivedLocation[0] = location;
      return EventResult.PASS;
    });

    TestPlayer player = new TestPlayer("Builder");
    TestBlock block = new TestBlock("minecraft:oak_planks");
    Location location = new Location(200, 80, -100);
    BlockPlaceCallback.EVENT.invoker().onBlockPlace(player, block, location);

    assertEquals("Builder", receivedPlayer[0].getDisplayName());
    assertEquals("minecraft:oak_planks", receivedBlock[0].getId());
    assertEquals(200, receivedLocation[0].x());
    assertEquals(80, receivedLocation[0].y());
    assertEquals(-100, receivedLocation[0].z());
  }

  @Test
  @DisplayName("should allow cancelling block place")
  void shouldAllowCancellingPlace() {
    BlockPlaceCallback.EVENT.register((player, block, location) -> {
      if (block.getId().equals("minecraft:tnt")) {
        return EventResult.CANCEL;
      }
      return EventResult.PASS;
    });

    TestPlayer player = new TestPlayer("Griefer");
    TestBlock tnt = new TestBlock("minecraft:tnt");
    Location location = new Location(0, 64, 0);
    EventResult result = BlockPlaceCallback.EVENT.invoker().onBlockPlace(player, tnt, location);

    assertTrue(result.isCancelled());
  }

  @Test
  @DisplayName("should allow place when not cancelled")
  void shouldAllowPlaceWhenNotCancelled() {
    BlockPlaceCallback.EVENT.register((player, block, location) -> {
      if (block.getId().equals("minecraft:tnt")) {
        return EventResult.CANCEL;
      }
      return EventResult.PASS;
    });

    TestPlayer player = new TestPlayer("Builder");
    TestBlock stone = new TestBlock("minecraft:stone");
    Location location = new Location(0, 64, 0);
    EventResult result = BlockPlaceCallback.EVENT.invoker().onBlockPlace(player, stone, location);

    assertFalse(result.isCancelled());
  }

  @Test
  @DisplayName("should execute listeners in priority order")
  void shouldExecuteInPriorityOrder() {
    List<String> order = new ArrayList<>();

    BlockPlaceCallback.EVENT.register(EventPriority.HIGHEST, (player, block, location) -> {
      order.add("HIGHEST");
      return EventResult.PASS;
    });
    BlockPlaceCallback.EVENT.register(EventPriority.LOWEST, (player, block, location) -> {
      order.add("LOWEST");
      return EventResult.PASS;
    });
    BlockPlaceCallback.EVENT.register(EventPriority.NORMAL, (player, block, location) -> {
      order.add("NORMAL");
      return EventResult.PASS;
    });

    TestPlayer player = new TestPlayer("Builder");
    TestBlock block = new TestBlock("minecraft:stone");
    BlockPlaceCallback.EVENT.invoker().onBlockPlace(player, block, new Location(0, 0, 0));

    assertEquals(List.of("HIGHEST", "NORMAL", "LOWEST"), order);
  }

  @Test
  @DisplayName("should stop execution on cancellation")
  void shouldStopExecutionOnCancellation() {
    List<String> executed = new ArrayList<>();

    BlockPlaceCallback.EVENT.register(EventPriority.HIGHEST, (player, block, location) -> {
      executed.add("HIGHEST");
      return EventResult.CANCEL;
    });

    BlockPlaceCallback.EVENT.register(EventPriority.NORMAL, (player, block, location) -> {
      executed.add("NORMAL");
      return EventResult.PASS;
    });

    TestPlayer player = new TestPlayer("Builder");
    TestBlock block = new TestBlock("minecraft:stone");
    EventResult result = BlockPlaceCallback.EVENT.invoker().onBlockPlace(player, block, new Location(0, 0, 0));

    assertTrue(result.isCancelled());
    assertEquals(List.of("HIGHEST"), executed);
  }

  @Test
  @DisplayName("should return PASS when no listeners are registered")
  void shouldReturnPassWithNoListeners() {
    TestPlayer player = new TestPlayer("Builder");
    TestBlock block = new TestBlock("minecraft:stone");
    EventResult result = BlockPlaceCallback.EVENT.invoker().onBlockPlace(player, block, new Location(0, 0, 0));

    assertFalse(result.isCancelled());
    assertEquals(EventResult.PASS, result);
  }

  @Test
  @DisplayName("should allow location-based restrictions")
  void shouldAllowLocationBasedRestrictions() {
    // Prevent placing blocks above y=200 (build height limit)
    BlockPlaceCallback.EVENT.register((player, block, location) -> {
      if (location.y() > 200) {
        return EventResult.CANCEL;
      }
      return EventResult.PASS;
    });

    TestPlayer player = new TestPlayer("Builder");
    TestBlock block = new TestBlock("minecraft:stone");

    // Should allow place below build limit
    EventResult allowedResult = BlockPlaceCallback.EVENT.invoker()
        .onBlockPlace(player, block, new Location(0, 100, 0));
    assertFalse(allowedResult.isCancelled());

    // Should cancel place above build limit
    EventResult cancelledResult = BlockPlaceCallback.EVENT.invoker()
        .onBlockPlace(player, block, new Location(0, 250, 0));
    assertTrue(cancelledResult.isCancelled());
  }

  @Test
  @DisplayName("should allow player-based restrictions")
  void shouldAllowPlayerBasedRestrictions() {
    BlockPlaceCallback.EVENT.register((player, block, location) -> {
      if (!player.hasPermission("block.place")) {
        return EventResult.CANCEL;
      }
      return EventResult.PASS;
    });

    TestBlock block = new TestBlock("minecraft:stone");
    Location location = new Location(0, 64, 0);

    // Player without permission - should cancel
    TestPlayer noPermPlayer = new TestPlayer("Guest");
    EventResult cancelledResult = BlockPlaceCallback.EVENT.invoker()
        .onBlockPlace(noPermPlayer, block, location);
    assertTrue(cancelledResult.isCancelled());

    // Player with permission - should allow
    TestPlayer permPlayer = new TestPlayer("Builder").addPermission("block.place");
    EventResult allowedResult = BlockPlaceCallback.EVENT.invoker()
        .onBlockPlace(permPlayer, block, location);
    assertFalse(allowedResult.isCancelled());
  }

  @Test
  @DisplayName("should allow multiple listeners at the same priority")
  void shouldAllowMultipleListenersAtSamePriority() {
    List<String> executed = new ArrayList<>();

    BlockPlaceCallback.EVENT.register(EventPriority.NORMAL, (player, block, location) -> {
      executed.add("first");
      return EventResult.PASS;
    });

    BlockPlaceCallback.EVENT.register(EventPriority.NORMAL, (player, block, location) -> {
      executed.add("second");
      return EventResult.PASS;
    });

    BlockPlaceCallback.EVENT.register(EventPriority.NORMAL, (player, block, location) -> {
      executed.add("third");
      return EventResult.PASS;
    });

    TestPlayer player = new TestPlayer("Builder");
    TestBlock block = new TestBlock("minecraft:stone");
    BlockPlaceCallback.EVENT.invoker().onBlockPlace(player, block, new Location(0, 0, 0));

    assertEquals(List.of("first", "second", "third"), executed);
  }

  @Test
  @DisplayName("should allow block-type restrictions")
  void shouldAllowBlockTypeRestrictions() {
    // Prevent placing dangerous blocks
    List<String> dangerousBlocks = List.of("minecraft:tnt", "minecraft:lava", "minecraft:fire");
    
    BlockPlaceCallback.EVENT.register((player, block, location) -> {
      if (dangerousBlocks.contains(block.getId())) {
        return EventResult.CANCEL;
      }
      return EventResult.PASS;
    });

    TestPlayer player = new TestPlayer("Builder");
    Location location = new Location(0, 64, 0);

    // Safe blocks should be allowed
    EventResult stoneResult = BlockPlaceCallback.EVENT.invoker()
        .onBlockPlace(player, new TestBlock("minecraft:stone"), location);
    assertFalse(stoneResult.isCancelled());

    // Dangerous blocks should be cancelled
    EventResult tntResult = BlockPlaceCallback.EVENT.invoker()
        .onBlockPlace(player, new TestBlock("minecraft:tnt"), location);
    assertTrue(tntResult.isCancelled());

    EventResult lavaResult = BlockPlaceCallback.EVENT.invoker()
        .onBlockPlace(player, new TestBlock("minecraft:lava"), location);
    assertTrue(lavaResult.isCancelled());
  }
}
