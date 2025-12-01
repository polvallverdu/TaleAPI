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

@DisplayName("BlockBreakCallback")
class BlockBreakCallbackTest {

  @AfterEach
  void cleanup() {
    BlockBreakCallback.EVENT.clearListeners();
  }

  @Test
  @DisplayName("should notify listeners when block is broken")
  void shouldNotifyOnBreak() {
    List<String> brokenBlocks = new ArrayList<>();

    BlockBreakCallback.EVENT.register((player, block, location) -> {
      brokenBlocks.add(block.getId());
      return EventResult.PASS;
    });

    TestPlayer player = new TestPlayer("Miner");
    TestBlock block = new TestBlock("minecraft:stone");
    Location location = new Location(10, 64, 20);
    BlockBreakCallback.EVENT.invoker().onBlockBreak(player, block, location);

    assertEquals(List.of("minecraft:stone"), brokenBlocks);
  }

  @Test
  @DisplayName("should provide correct player, block, and location to listeners")
  void shouldProvideCorrectEventData() {
    TestPlayer[] receivedPlayer = new TestPlayer[1];
    TestBlock[] receivedBlock = new TestBlock[1];
    Location[] receivedLocation = new Location[1];

    BlockBreakCallback.EVENT.register((player, block, location) -> {
      receivedPlayer[0] = (TestPlayer) player;
      receivedBlock[0] = (TestBlock) block;
      receivedLocation[0] = location;
      return EventResult.PASS;
    });

    TestPlayer player = new TestPlayer("Miner");
    TestBlock block = new TestBlock("minecraft:diamond_ore");
    Location location = new Location(100, 12, -50);
    BlockBreakCallback.EVENT.invoker().onBlockBreak(player, block, location);

    assertEquals("Miner", receivedPlayer[0].getDisplayName());
    assertEquals("minecraft:diamond_ore", receivedBlock[0].getId());
    assertEquals(100, receivedLocation[0].x());
    assertEquals(12, receivedLocation[0].y());
    assertEquals(-50, receivedLocation[0].z());
  }

  @Test
  @DisplayName("should allow cancelling block break")
  void shouldAllowCancellingBreak() {
    BlockBreakCallback.EVENT.register((player, block, location) -> {
      if (block.getId().equals("minecraft:bedrock")) {
        return EventResult.CANCEL;
      }
      return EventResult.PASS;
    });

    TestPlayer player = new TestPlayer("Miner");
    TestBlock bedrock = new TestBlock("minecraft:bedrock");
    Location location = new Location(0, 0, 0);
    EventResult result = BlockBreakCallback.EVENT.invoker().onBlockBreak(player, bedrock, location);

    assertTrue(result.isCancelled());
  }

  @Test
  @DisplayName("should allow break when not cancelled")
  void shouldAllowBreakWhenNotCancelled() {
    BlockBreakCallback.EVENT.register((player, block, location) -> {
      if (block.getId().equals("minecraft:bedrock")) {
        return EventResult.CANCEL;
      }
      return EventResult.PASS;
    });

    TestPlayer player = new TestPlayer("Miner");
    TestBlock stone = new TestBlock("minecraft:stone");
    Location location = new Location(0, 64, 0);
    EventResult result = BlockBreakCallback.EVENT.invoker().onBlockBreak(player, stone, location);

    assertFalse(result.isCancelled());
  }

  @Test
  @DisplayName("should execute listeners in priority order")
  void shouldExecuteInPriorityOrder() {
    List<String> order = new ArrayList<>();

    BlockBreakCallback.EVENT.register(EventPriority.HIGHEST, (player, block, location) -> {
      order.add("HIGHEST");
      return EventResult.PASS;
    });
    BlockBreakCallback.EVENT.register(EventPriority.LOWEST, (player, block, location) -> {
      order.add("LOWEST");
      return EventResult.PASS;
    });
    BlockBreakCallback.EVENT.register(EventPriority.NORMAL, (player, block, location) -> {
      order.add("NORMAL");
      return EventResult.PASS;
    });

    TestPlayer player = new TestPlayer("Miner");
    TestBlock block = new TestBlock("minecraft:stone");
    BlockBreakCallback.EVENT.invoker().onBlockBreak(player, block, new Location(0, 0, 0));

    assertEquals(List.of("HIGHEST", "NORMAL", "LOWEST"), order);
  }

  @Test
  @DisplayName("should stop execution on cancellation")
  void shouldStopExecutionOnCancellation() {
    List<String> executed = new ArrayList<>();

    BlockBreakCallback.EVENT.register(EventPriority.HIGHEST, (player, block, location) -> {
      executed.add("HIGHEST");
      return EventResult.CANCEL;
    });

    BlockBreakCallback.EVENT.register(EventPriority.NORMAL, (player, block, location) -> {
      executed.add("NORMAL");
      return EventResult.PASS;
    });

    TestPlayer player = new TestPlayer("Miner");
    TestBlock block = new TestBlock("minecraft:stone");
    EventResult result = BlockBreakCallback.EVENT.invoker().onBlockBreak(player, block, new Location(0, 0, 0));

    assertTrue(result.isCancelled());
    assertEquals(List.of("HIGHEST"), executed);
  }

  @Test
  @DisplayName("should return PASS when no listeners are registered")
  void shouldReturnPassWithNoListeners() {
    TestPlayer player = new TestPlayer("Miner");
    TestBlock block = new TestBlock("minecraft:stone");
    EventResult result = BlockBreakCallback.EVENT.invoker().onBlockBreak(player, block, new Location(0, 0, 0));

    assertFalse(result.isCancelled());
    assertEquals(EventResult.PASS, result);
  }

  @Test
  @DisplayName("should allow location-based restrictions")
  void shouldAllowLocationBasedRestrictions() {
    // Prevent breaking blocks in a protected spawn area (within 10 blocks of origin)
    BlockBreakCallback.EVENT.register((player, block, location) -> {
      if (Math.abs(location.x()) < 10 && Math.abs(location.z()) < 10) {
        return EventResult.CANCEL;
      }
      return EventResult.PASS;
    });

    TestPlayer player = new TestPlayer("Miner");
    TestBlock block = new TestBlock("minecraft:stone");

    // Should cancel break in protected area
    EventResult cancelledResult = BlockBreakCallback.EVENT.invoker()
        .onBlockBreak(player, block, new Location(5, 64, 5));
    assertTrue(cancelledResult.isCancelled());

    // Should allow break outside protected area
    EventResult allowedResult = BlockBreakCallback.EVENT.invoker()
        .onBlockBreak(player, block, new Location(50, 64, 50));
    assertFalse(allowedResult.isCancelled());
  }

  @Test
  @DisplayName("should allow player-based restrictions")
  void shouldAllowPlayerBasedRestrictions() {
    BlockBreakCallback.EVENT.register((player, block, location) -> {
      if (!player.hasPermission("block.break")) {
        return EventResult.CANCEL;
      }
      return EventResult.PASS;
    });

    TestBlock block = new TestBlock("minecraft:stone");
    Location location = new Location(0, 64, 0);

    // Player without permission - should cancel
    TestPlayer noPermPlayer = new TestPlayer("Guest");
    EventResult cancelledResult = BlockBreakCallback.EVENT.invoker()
        .onBlockBreak(noPermPlayer, block, location);
    assertTrue(cancelledResult.isCancelled());

    // Player with permission - should allow
    TestPlayer permPlayer = new TestPlayer("Builder").addPermission("block.break");
    EventResult allowedResult = BlockBreakCallback.EVENT.invoker()
        .onBlockBreak(permPlayer, block, location);
    assertFalse(allowedResult.isCancelled());
  }

  @Test
  @DisplayName("should allow multiple listeners at the same priority")
  void shouldAllowMultipleListenersAtSamePriority() {
    List<String> executed = new ArrayList<>();

    BlockBreakCallback.EVENT.register(EventPriority.NORMAL, (player, block, location) -> {
      executed.add("first");
      return EventResult.PASS;
    });

    BlockBreakCallback.EVENT.register(EventPriority.NORMAL, (player, block, location) -> {
      executed.add("second");
      return EventResult.PASS;
    });

    BlockBreakCallback.EVENT.register(EventPriority.NORMAL, (player, block, location) -> {
      executed.add("third");
      return EventResult.PASS;
    });

    TestPlayer player = new TestPlayer("Miner");
    TestBlock block = new TestBlock("minecraft:stone");
    BlockBreakCallback.EVENT.invoker().onBlockBreak(player, block, new Location(0, 0, 0));

    assertEquals(List.of("first", "second", "third"), executed);
  }
}
