package dev.polv.taleapi.event.player;

import dev.polv.taleapi.event.EventPriority;
import dev.polv.taleapi.event.EventResult;
import dev.polv.taleapi.testutil.TestPlayer;
import dev.polv.taleapi.world.Location;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PlayerMoveCallback")
class PlayerMoveCallbackTest {

  @AfterEach
  void cleanup() {
    PlayerMoveCallback.EVENT.clearListeners();
  }

  @Test
  @DisplayName("should notify listeners when player moves")
  void shouldNotifyOnMove() {
    List<String> movedPlayers = new ArrayList<>();

    PlayerMoveCallback.EVENT.register((player, from, to) -> {
      movedPlayers.add(player.getDisplayName());
      return EventResult.PASS;
    });

    TestPlayer player = new TestPlayer("Steve");
    Location from = new Location(0, 64, 0);
    Location to = new Location(5, 64, 5);
    PlayerMoveCallback.EVENT.invoker().onPlayerMove(player, from, to);

    assertEquals(List.of("Steve"), movedPlayers);
  }

  @Test
  @DisplayName("should provide correct from and to locations")
  void shouldProvideCorrectLocations() {
    Location[] receivedFrom = new Location[1];
    Location[] receivedTo = new Location[1];

    PlayerMoveCallback.EVENT.register((player, from, to) -> {
      receivedFrom[0] = from;
      receivedTo[0] = to;
      return EventResult.PASS;
    });

    TestPlayer player = new TestPlayer("Alex");
    Location from = new Location(10, 64, 20, 90.0f, 0.0f);
    Location to = new Location(15, 65, 25, 180.0f, -45.0f);
    PlayerMoveCallback.EVENT.invoker().onPlayerMove(player, from, to);

    assertEquals(from, receivedFrom[0]);
    assertEquals(to, receivedTo[0]);
    assertEquals(90.0f, receivedFrom[0].yaw());
    assertEquals(-45.0f, receivedTo[0].pitch());
  }

  @Test
  @DisplayName("should allow cancelling player movement")
  void shouldAllowCancellingMovement() {
    // Prevent movement for frozen players
    PlayerMoveCallback.EVENT.register((player, from, to) -> {
      if (player.getDisplayName().startsWith("Frozen_")) {
        return EventResult.CANCEL;
      }
      return EventResult.PASS;
    });

    TestPlayer frozenPlayer = new TestPlayer("Frozen_Steve");
    Location from = new Location(0, 64, 0);
    Location to = new Location(1, 64, 1);
    EventResult result = PlayerMoveCallback.EVENT.invoker().onPlayerMove(frozenPlayer, from, to);

    assertTrue(result.isCancelled());
  }

  @Test
  @DisplayName("should execute listeners in priority order")
  void shouldExecuteInPriorityOrder() {
    List<String> order = new ArrayList<>();

    PlayerMoveCallback.EVENT.register(EventPriority.HIGHEST, (player, from, to) -> {
      order.add("HIGHEST");
      return EventResult.PASS;
    });
    PlayerMoveCallback.EVENT.register(EventPriority.LOWEST, (player, from, to) -> {
      order.add("LOWEST");
      return EventResult.PASS;
    });
    PlayerMoveCallback.EVENT.register(EventPriority.NORMAL, (player, from, to) -> {
      order.add("NORMAL");
      return EventResult.PASS;
    });

    TestPlayer player = new TestPlayer("Player");
    PlayerMoveCallback.EVENT.invoker().onPlayerMove(player, new Location(0, 0, 0), new Location(1, 0, 1));

    assertEquals(List.of("HIGHEST", "NORMAL", "LOWEST"), order);
  }

  @Test
  @DisplayName("should stop execution on cancellation")
  void shouldStopExecutionOnCancellation() {
    List<String> executed = new ArrayList<>();

    PlayerMoveCallback.EVENT.register(EventPriority.HIGHEST, (player, from, to) -> {
      executed.add("HIGHEST");
      return EventResult.CANCEL;
    });

    PlayerMoveCallback.EVENT.register(EventPriority.NORMAL, (player, from, to) -> {
      executed.add("NORMAL");
      return EventResult.PASS;
    });

    TestPlayer player = new TestPlayer("Player");
    EventResult result = PlayerMoveCallback.EVENT.invoker()
        .onPlayerMove(player, new Location(0, 0, 0), new Location(1, 0, 1));

    assertTrue(result.isCancelled());
    assertEquals(List.of("HIGHEST"), executed);
  }

  @Test
  @DisplayName("should return PASS when no listeners are registered")
  void shouldReturnPassWithNoListeners() {
    TestPlayer player = new TestPlayer("Player");
    EventResult result = PlayerMoveCallback.EVENT.invoker()
        .onPlayerMove(player, new Location(0, 0, 0), new Location(1, 0, 1));

    assertFalse(result.isCancelled());
    assertEquals(EventResult.PASS, result);
  }

  @Test
  @DisplayName("should allow boundary checks")
  void shouldAllowBoundaryChecks() {
    // Prevent players from leaving world boundary (-1000 to 1000)
    PlayerMoveCallback.EVENT.register((player, from, to) -> {
      if (Math.abs(to.x()) > 1000 || Math.abs(to.z()) > 1000) {
        return EventResult.CANCEL;
      }
      return EventResult.PASS;
    });

    TestPlayer player = new TestPlayer("Explorer");

    // Movement within bounds should be allowed
    EventResult allowedResult = PlayerMoveCallback.EVENT.invoker()
        .onPlayerMove(player, new Location(500, 64, 500), new Location(600, 64, 600));
    assertFalse(allowedResult.isCancelled());

    // Movement out of bounds should be cancelled
    EventResult cancelledResult = PlayerMoveCallback.EVENT.invoker()
        .onPlayerMove(player, new Location(990, 64, 990), new Location(1100, 64, 1100));
    assertTrue(cancelledResult.isCancelled());
  }

  @Test
  @DisplayName("should allow speed checks using distance")
  void shouldAllowSpeedChecks() {
    final double MAX_SPEED = 10.0; // max blocks per move

    PlayerMoveCallback.EVENT.register((player, from, to) -> {
      double distance = from.distance(to);
      if (distance > MAX_SPEED) {
        // Potential speed hacker
        return EventResult.CANCEL;
      }
      return EventResult.PASS;
    });

    TestPlayer player = new TestPlayer("Player");

    // Normal movement should pass
    EventResult normalMove = PlayerMoveCallback.EVENT.invoker()
        .onPlayerMove(player, new Location(0, 64, 0), new Location(5, 64, 5));
    assertFalse(normalMove.isCancelled());

    // Suspicious movement should be cancelled
    EventResult suspiciousMove = PlayerMoveCallback.EVENT.invoker()
        .onPlayerMove(player, new Location(0, 64, 0), new Location(100, 64, 100));
    assertTrue(suspiciousMove.isCancelled());
  }

  @Test
  @DisplayName("should handle rotation-only changes")
  void shouldHandleRotationOnlyChanges() {
    boolean[] rotationChanged = { false };

    PlayerMoveCallback.EVENT.register((player, from, to) -> {
      boolean positionSame = from.x() == to.x() && from.y() == to.y() && from.z() == to.z();
      boolean rotationDifferent = from.yaw() != to.yaw() || from.pitch() != to.pitch();
      if (positionSame && rotationDifferent) {
        rotationChanged[0] = true;
      }
      return EventResult.PASS;
    });

    TestPlayer player = new TestPlayer("Player");
    // Same position, different rotation
    Location from = new Location(10, 64, 10, 0.0f, 0.0f);
    Location to = new Location(10, 64, 10, 90.0f, 45.0f);
    PlayerMoveCallback.EVENT.invoker().onPlayerMove(player, from, to);

    assertTrue(rotationChanged[0]);
  }

  @Test
  @DisplayName("should allow multiple listeners at same priority")
  void shouldAllowMultipleListenersAtSamePriority() {
    List<String> executed = new ArrayList<>();

    PlayerMoveCallback.EVENT.register(EventPriority.NORMAL, (player, from, to) -> {
      executed.add("first");
      return EventResult.PASS;
    });

    PlayerMoveCallback.EVENT.register(EventPriority.NORMAL, (player, from, to) -> {
      executed.add("second");
      return EventResult.PASS;
    });

    TestPlayer player = new TestPlayer("Player");
    PlayerMoveCallback.EVENT.invoker().onPlayerMove(player, new Location(0, 0, 0), new Location(1, 0, 1));

    assertEquals(List.of("first", "second"), executed);
  }
}
