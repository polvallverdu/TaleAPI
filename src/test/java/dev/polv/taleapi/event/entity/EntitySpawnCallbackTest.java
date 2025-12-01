package dev.polv.taleapi.event.entity;

import dev.polv.taleapi.event.EventPriority;
import dev.polv.taleapi.event.EventResult;
import dev.polv.taleapi.testutil.TestEntity;
import dev.polv.taleapi.world.Location;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EntitySpawnCallback")
class EntitySpawnCallbackTest {

  @AfterEach
  void cleanup() {
    EntitySpawnCallback.EVENT.clearListeners();
  }

  @Test
  @DisplayName("should notify listeners when entity spawns")
  void shouldNotifyOnSpawn() {
    List<String> spawnedEntities = new ArrayList<>();

    EntitySpawnCallback.EVENT.register((entity, location) -> {
      spawnedEntities.add(entity.getUniqueId());
      return EventResult.PASS;
    });

    TestEntity entity = new TestEntity("zombie");
    Location spawnLocation = new Location(10, 64, 20);
    EntitySpawnCallback.EVENT.invoker().onEntitySpawn(entity, spawnLocation);

    assertEquals(1, spawnedEntities.size());
    assertEquals(entity.getUniqueId(), spawnedEntities.get(0));
  }

  @Test
  @DisplayName("should provide correct spawn location to listeners")
  void shouldProvideSpawnLocation() {
    Location[] receivedLocation = new Location[1];

    EntitySpawnCallback.EVENT.register((entity, location) -> {
      receivedLocation[0] = location;
      return EventResult.PASS;
    });

    TestEntity entity = new TestEntity("pig");
    Location spawnLocation = new Location(100, 70, -50);
    EntitySpawnCallback.EVENT.invoker().onEntitySpawn(entity, spawnLocation);

    assertEquals(spawnLocation, receivedLocation[0]);
    assertEquals(100, receivedLocation[0].x());
    assertEquals(70, receivedLocation[0].y());
    assertEquals(-50, receivedLocation[0].z());
  }

  @Test
  @DisplayName("should allow cancelling entity spawn")
  void shouldAllowCancellingSpawn() {
    EntitySpawnCallback.EVENT.register((entity, location) -> {
      if (entity instanceof TestEntity testEntity && testEntity.getType().equals("hostile_mob")) {
        return EventResult.CANCEL;
      }
      return EventResult.PASS;
    });

    TestEntity hostileMob = new TestEntity("hostile_mob");
    Location location = new Location(0, 64, 0);
    EventResult result = EntitySpawnCallback.EVENT.invoker().onEntitySpawn(hostileMob, location);

    assertTrue(result.isCancelled());
  }

  @Test
  @DisplayName("should execute listeners in priority order")
  void shouldExecuteInPriorityOrder() {
    List<String> order = new ArrayList<>();

    EntitySpawnCallback.EVENT.register(EventPriority.HIGHEST, (entity, location) -> {
      order.add("HIGHEST");
      return EventResult.PASS;
    });
    EntitySpawnCallback.EVENT.register(EventPriority.LOWEST, (entity, location) -> {
      order.add("LOWEST");
      return EventResult.PASS;
    });
    EntitySpawnCallback.EVENT.register(EventPriority.NORMAL, (entity, location) -> {
      order.add("NORMAL");
      return EventResult.PASS;
    });

    TestEntity entity = new TestEntity("zombie");
    EntitySpawnCallback.EVENT.invoker().onEntitySpawn(entity, new Location(0, 0, 0));

    assertEquals(List.of("HIGHEST", "NORMAL", "LOWEST"), order);
  }

  @Test
  @DisplayName("should stop execution on cancellation")
  void shouldStopExecutionOnCancellation() {
    List<String> executed = new ArrayList<>();

    EntitySpawnCallback.EVENT.register(EventPriority.HIGHEST, (entity, location) -> {
      executed.add("HIGHEST");
      return EventResult.CANCEL;
    });

    EntitySpawnCallback.EVENT.register(EventPriority.NORMAL, (entity, location) -> {
      executed.add("NORMAL");
      return EventResult.PASS;
    });

    TestEntity entity = new TestEntity("zombie");
    EventResult result = EntitySpawnCallback.EVENT.invoker().onEntitySpawn(entity, new Location(0, 0, 0));

    assertTrue(result.isCancelled());
    assertEquals(List.of("HIGHEST"), executed);
  }

  @Test
  @DisplayName("should return PASS when no listeners are registered")
  void shouldReturnPassWithNoListeners() {
    TestEntity entity = new TestEntity("zombie");
    EventResult result = EntitySpawnCallback.EVENT.invoker().onEntitySpawn(entity, new Location(0, 0, 0));

    assertFalse(result.isCancelled());
    assertEquals(EventResult.PASS, result);
  }

  @Test
  @DisplayName("should allow spawn restrictions based on location")
  void shouldAllowLocationBasedRestrictions() {
    // Prevent spawns above y=100
    EntitySpawnCallback.EVENT.register((entity, location) -> {
      if (location.y() > 100) {
        return EventResult.CANCEL;
      }
      return EventResult.PASS;
    });

    TestEntity entity = new TestEntity("zombie");

    // Should allow spawn at y=50
    EventResult allowedResult = EntitySpawnCallback.EVENT.invoker()
        .onEntitySpawn(entity, new Location(0, 50, 0));
    assertFalse(allowedResult.isCancelled());

    // Should cancel spawn at y=150
    EventResult cancelledResult = EntitySpawnCallback.EVENT.invoker()
        .onEntitySpawn(entity, new Location(0, 150, 0));
    assertTrue(cancelledResult.isCancelled());
  }
}
