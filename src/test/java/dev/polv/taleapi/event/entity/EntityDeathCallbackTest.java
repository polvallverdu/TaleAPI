package dev.polv.taleapi.event.entity;

import dev.polv.taleapi.event.EventPriority;
import dev.polv.taleapi.event.EventResult;
import dev.polv.taleapi.testutil.TestEntity;
import dev.polv.taleapi.testutil.TestPlayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EntityDeathCallback")
class EntityDeathCallbackTest {

  @AfterEach
  void cleanup() {
    EntityDeathCallback.EVENT.clearListeners();
  }

  @Test
  @DisplayName("should notify listeners when entity dies")
  void shouldNotifyOnDeath() {
    List<String> deadEntities = new ArrayList<>();

    EntityDeathCallback.EVENT.register((entity, cause) -> {
      deadEntities.add(entity.getUniqueId());
      return EventResult.PASS;
    });

    TestEntity entity = new TestEntity("zombie");
    DeathCause cause = DeathCause.of(DeathCause.Type.FALL);
    EntityDeathCallback.EVENT.invoker().onEntityDeath(entity, cause);

    assertEquals(1, deadEntities.size());
    assertEquals(entity.getUniqueId(), deadEntities.get(0));
  }

  @Test
  @DisplayName("should provide death cause to listeners")
  void shouldProvideDeathCause() {
    DeathCause[] receivedCause = new DeathCause[1];

    EntityDeathCallback.EVENT.register((entity, cause) -> {
      receivedCause[0] = cause;
      return EventResult.PASS;
    });

    TestEntity entity = new TestEntity("pig");
    TestPlayer killer = new TestPlayer("player1");
    DeathCause cause = DeathCause.byPlayer(killer);
    EntityDeathCallback.EVENT.invoker().onEntityDeath(entity, cause);

    assertEquals(cause, receivedCause[0]);
    assertTrue(receivedCause[0].isPlayerKill());
    assertEquals(killer, receivedCause[0].getKiller());
  }

  @Test
  @DisplayName("should allow cancelling entity death")
  void shouldAllowCancellingDeath() {
    EntityDeathCallback.EVENT.register((entity, cause) -> {
      // Prevent deaths in "safe zones" (simulated check)
      if (entity instanceof TestEntity testEntity && testEntity.getType().equals("protected")) {
        return EventResult.CANCEL;
      }
      return EventResult.PASS;
    });

    TestEntity protectedEntity = new TestEntity("protected");
    DeathCause cause = DeathCause.of(DeathCause.Type.FALL);
    EventResult result = EntityDeathCallback.EVENT.invoker().onEntityDeath(protectedEntity, cause);

    assertTrue(result.isCancelled());
  }

  @Test
  @DisplayName("should execute listeners in priority order")
  void shouldExecuteInPriorityOrder() {
    List<String> order = new ArrayList<>();

    EntityDeathCallback.EVENT.register(EventPriority.HIGHEST, (entity, cause) -> {
      order.add("HIGHEST");
      return EventResult.PASS;
    });
    EntityDeathCallback.EVENT.register(EventPriority.LOWEST, (entity, cause) -> {
      order.add("LOWEST");
      return EventResult.PASS;
    });
    EntityDeathCallback.EVENT.register(EventPriority.NORMAL, (entity, cause) -> {
      order.add("NORMAL");
      return EventResult.PASS;
    });

    TestEntity entity = new TestEntity("zombie");
    DeathCause cause = DeathCause.of(DeathCause.Type.VOID);
    EntityDeathCallback.EVENT.invoker().onEntityDeath(entity, cause);

    assertEquals(List.of("HIGHEST", "NORMAL", "LOWEST"), order);
  }

  @Test
  @DisplayName("should stop execution on cancellation")
  void shouldStopExecutionOnCancellation() {
    List<String> executed = new ArrayList<>();

    EntityDeathCallback.EVENT.register(EventPriority.HIGHEST, (entity, cause) -> {
      executed.add("HIGHEST");
      return EventResult.CANCEL;
    });

    EntityDeathCallback.EVENT.register(EventPriority.NORMAL, (entity, cause) -> {
      executed.add("NORMAL");
      return EventResult.PASS;
    });

    TestEntity entity = new TestEntity("zombie");
    DeathCause cause = DeathCause.of(DeathCause.Type.FALL);
    EventResult result = EntityDeathCallback.EVENT.invoker().onEntityDeath(entity, cause);

    assertTrue(result.isCancelled());
    assertEquals(List.of("HIGHEST"), executed);
  }

  @Test
  @DisplayName("should return PASS when no listeners are registered")
  void shouldReturnPassWithNoListeners() {
    TestEntity entity = new TestEntity("zombie");
    DeathCause cause = DeathCause.of(DeathCause.Type.FALL);
    EventResult result = EntityDeathCallback.EVENT.invoker().onEntityDeath(entity, cause);

    assertFalse(result.isCancelled());
    assertEquals(EventResult.PASS, result);
  }

  @Test
  @DisplayName("should track player kills")
  void shouldTrackPlayerKills() {
    List<String> killerIds = new ArrayList<>();

    EntityDeathCallback.EVENT.register((entity, cause) -> {
      if (cause.isPlayerKill()) {
        killerIds.add(cause.getKiller().getUniqueId());
      }
      return EventResult.PASS;
    });

    TestEntity victim = new TestEntity("zombie");
    TestPlayer killer = new TestPlayer("killer-uuid", "Killer");
    DeathCause cause = DeathCause.byPlayer(killer);
    EntityDeathCallback.EVENT.invoker().onEntityDeath(victim, cause);

    assertEquals(1, killerIds.size());
    assertEquals("killer-uuid", killerIds.get(0));
  }

  @Test
  @DisplayName("should handle different death causes correctly")
  void shouldHandleDifferentDeathCauses() {
    List<DeathCause.Type> deathTypes = new ArrayList<>();

    EntityDeathCallback.EVENT.register((entity, cause) -> {
      deathTypes.add(cause.getType());
      return EventResult.PASS;
    });

    TestEntity entity = new TestEntity("entity");

    EntityDeathCallback.EVENT.invoker().onEntityDeath(entity, DeathCause.of(DeathCause.Type.FALL));
    EntityDeathCallback.EVENT.invoker().onEntityDeath(entity, DeathCause.of(DeathCause.Type.DROWNING));
    EntityDeathCallback.EVENT.invoker().onEntityDeath(entity, DeathCause.of(DeathCause.Type.FIRE));

    assertEquals(3, deathTypes.size());
    assertTrue(deathTypes.contains(DeathCause.Type.FALL));
    assertTrue(deathTypes.contains(DeathCause.Type.DROWNING));
    assertTrue(deathTypes.contains(DeathCause.Type.FIRE));
  }
}
