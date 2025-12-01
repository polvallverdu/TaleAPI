package dev.polv.taleapi.event.entity;

import dev.polv.taleapi.entity.TalePlayer;
import dev.polv.taleapi.event.EventPriority;
import dev.polv.taleapi.event.EventResult;
import dev.polv.taleapi.testutil.TestEntity;
import dev.polv.taleapi.testutil.TestPlayer;
import dev.polv.taleapi.world.Location;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EntityMoveCallback")
class EntityMoveCallbackTest {

  @AfterEach
  void cleanup() {
    EntityMoveCallback.EVENT.clearListeners();
  }

  @Test
  @DisplayName("should notify listeners when entity moves")
  void shouldNotifyOnMove() {
    List<String> movedEntities = new ArrayList<>();

    EntityMoveCallback.EVENT.register((entity, from, to) -> {
      movedEntities.add(entity.getUniqueId());
      return EventResult.PASS;
    });

    TestEntity entity = new TestEntity("zombie");
    Location from = new Location(0, 64, 0);
    Location to = new Location(5, 64, 5);
    EntityMoveCallback.EVENT.invoker().onEntityMove(entity, from, to);

    assertEquals(1, movedEntities.size());
    assertEquals(entity.getUniqueId(), movedEntities.get(0));
  }

  @Test
  @DisplayName("should provide correct from and to locations")
  void shouldProvideCorrectLocations() {
    Location[] receivedFrom = new Location[1];
    Location[] receivedTo = new Location[1];

    EntityMoveCallback.EVENT.register((entity, from, to) -> {
      receivedFrom[0] = from;
      receivedTo[0] = to;
      return EventResult.PASS;
    });

    TestEntity entity = new TestEntity("pig");
    Location from = new Location(10, 64, 20);
    Location to = new Location(15, 65, 25);
    EntityMoveCallback.EVENT.invoker().onEntityMove(entity, from, to);

    assertEquals(from, receivedFrom[0]);
    assertEquals(to, receivedTo[0]);
  }

  @Test
  @DisplayName("should allow cancelling entity movement")
  void shouldAllowCancellingMovement() {
    // Prevent movement into restricted zone (x > 100)
    EntityMoveCallback.EVENT.register((entity, from, to) -> {
      if (to.x() > 100) {
        return EventResult.CANCEL;
      }
      return EventResult.PASS;
    });

    TestEntity entity = new TestEntity("zombie");
    Location from = new Location(95, 64, 0);
    Location to = new Location(105, 64, 0);
    EventResult result = EntityMoveCallback.EVENT.invoker().onEntityMove(entity, from, to);

    assertTrue(result.isCancelled());
  }

  @Test
  @DisplayName("should fire for players (players are entities)")
  void shouldFireForPlayers() {
    List<String> movedEntities = new ArrayList<>();
    boolean[] wasPlayer = { false };

    EntityMoveCallback.EVENT.register((entity, from, to) -> {
      movedEntities.add(entity.getUniqueId());
      if (entity instanceof TalePlayer) {
        wasPlayer[0] = true;
      }
      return EventResult.PASS;
    });

    TestPlayer player = new TestPlayer("Steve");
    Location from = new Location(0, 64, 0);
    Location to = new Location(1, 64, 1);
    EntityMoveCallback.EVENT.invoker().onEntityMove(player, from, to);

    assertEquals(1, movedEntities.size());
    assertTrue(wasPlayer[0], "Entity should be detected as a player");
  }

  @Test
  @DisplayName("should execute listeners in priority order")
  void shouldExecuteInPriorityOrder() {
    List<String> order = new ArrayList<>();

    EntityMoveCallback.EVENT.register(EventPriority.HIGHEST, (entity, from, to) -> {
      order.add("HIGHEST");
      return EventResult.PASS;
    });
    EntityMoveCallback.EVENT.register(EventPriority.LOWEST, (entity, from, to) -> {
      order.add("LOWEST");
      return EventResult.PASS;
    });
    EntityMoveCallback.EVENT.register(EventPriority.NORMAL, (entity, from, to) -> {
      order.add("NORMAL");
      return EventResult.PASS;
    });

    TestEntity entity = new TestEntity("zombie");
    EntityMoveCallback.EVENT.invoker().onEntityMove(entity, new Location(0, 0, 0), new Location(1, 0, 1));

    assertEquals(List.of("HIGHEST", "NORMAL", "LOWEST"), order);
  }

  @Test
  @DisplayName("should stop execution on cancellation")
  void shouldStopExecutionOnCancellation() {
    List<String> executed = new ArrayList<>();

    EntityMoveCallback.EVENT.register(EventPriority.HIGHEST, (entity, from, to) -> {
      executed.add("HIGHEST");
      return EventResult.CANCEL;
    });

    EntityMoveCallback.EVENT.register(EventPriority.NORMAL, (entity, from, to) -> {
      executed.add("NORMAL");
      return EventResult.PASS;
    });

    TestEntity entity = new TestEntity("zombie");
    EventResult result = EntityMoveCallback.EVENT.invoker()
        .onEntityMove(entity, new Location(0, 0, 0), new Location(1, 0, 1));

    assertTrue(result.isCancelled());
    assertEquals(List.of("HIGHEST"), executed);
  }

  @Test
  @DisplayName("should return PASS when no listeners are registered")
  void shouldReturnPassWithNoListeners() {
    TestEntity entity = new TestEntity("zombie");
    EventResult result = EntityMoveCallback.EVENT.invoker()
        .onEntityMove(entity, new Location(0, 0, 0), new Location(1, 0, 1));

    assertFalse(result.isCancelled());
    assertEquals(EventResult.PASS, result);
  }

  @Test
  @DisplayName("should allow different handling for players vs other entities")
  void shouldAllowDifferentHandlingForPlayersVsEntities() {
    List<String> playerMoves = new ArrayList<>();
    List<String> entityMoves = new ArrayList<>();

    EntityMoveCallback.EVENT.register((entity, from, to) -> {
      if (entity instanceof TalePlayer player) {
        playerMoves.add(player.getDisplayName());
      } else {
        entityMoves.add(entity.getUniqueId());
      }
      return EventResult.PASS;
    });

    TestPlayer player = new TestPlayer("Alex");
    TestEntity zombie = new TestEntity("zombie");
    Location from = new Location(0, 64, 0);
    Location to = new Location(1, 64, 1);

    EntityMoveCallback.EVENT.invoker().onEntityMove(player, from, to);
    EntityMoveCallback.EVENT.invoker().onEntityMove(zombie, from, to);

    assertEquals(List.of("Alex"), playerMoves);
    assertEquals(1, entityMoves.size());
  }

  @Test
  @DisplayName("should calculate distance correctly using Location")
  void shouldCalculateDistanceCorrectly() {
    double[] calculatedDistance = { 0 };

    EntityMoveCallback.EVENT.register((entity, from, to) -> {
      calculatedDistance[0] = from.distance(to);
      return EventResult.PASS;
    });

    TestEntity entity = new TestEntity("zombie");
    Location from = new Location(0, 0, 0);
    Location to = new Location(3, 4, 0); // 3-4-5 triangle, distance should be 5
    EntityMoveCallback.EVENT.invoker().onEntityMove(entity, from, to);

    assertEquals(5.0, calculatedDistance[0], 0.0001);
  }
}
