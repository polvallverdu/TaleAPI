package dev.polv.taleapi.event.player;

import dev.polv.taleapi.event.EventPriority;
import dev.polv.taleapi.event.EventResult;
import dev.polv.taleapi.event.entity.DeathCause;
import dev.polv.taleapi.testutil.TestPlayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PlayerDeathCallback")
class PlayerDeathCallbackTest {

  @AfterEach
  void cleanup() {
    PlayerDeathCallback.EVENT.clearListeners();
  }

  @Test
  @DisplayName("should notify listeners when player dies")
  void shouldNotifyOnDeath() {
    List<String> deadPlayers = new ArrayList<>();

    PlayerDeathCallback.EVENT.register((player, cause) -> {
      deadPlayers.add(player.getDisplayName());
      return EventResult.PASS;
    });

    TestPlayer player = new TestPlayer("TestPlayer");
    DeathCause cause = DeathCause.of(DeathCause.Type.FALL);
    PlayerDeathCallback.EVENT.invoker().onPlayerDeath(player, cause);

    assertEquals(1, deadPlayers.size());
    assertEquals("TestPlayer", deadPlayers.get(0));
  }

  @Test
  @DisplayName("should provide death cause to listeners")
  void shouldProvideDeathCause() {
    DeathCause[] receivedCause = new DeathCause[1];

    PlayerDeathCallback.EVENT.register((player, cause) -> {
      receivedCause[0] = cause;
      return EventResult.PASS;
    });

    TestPlayer player = new TestPlayer("Victim");
    TestPlayer killer = new TestPlayer("Killer");
    DeathCause cause = DeathCause.byPlayer(killer);
    PlayerDeathCallback.EVENT.invoker().onPlayerDeath(player, cause);

    assertEquals(cause, receivedCause[0]);
    assertTrue(receivedCause[0].isPlayerKill());
    assertEquals(killer, receivedCause[0].getKiller());
  }

  @Test
  @DisplayName("should allow cancelling player death")
  void shouldAllowCancellingDeath() {
    PlayerDeathCallback.EVENT.register((player, cause) -> {
      // Players with admin permission survive
      if (player.hasPermission("server.immortal")) {
        return EventResult.CANCEL;
      }
      return EventResult.PASS;
    });

    TestPlayer adminPlayer = new TestPlayer("Admin");
    adminPlayer.addPermission("server.immortal");
    DeathCause cause = DeathCause.of(DeathCause.Type.VOID);
    EventResult result = PlayerDeathCallback.EVENT.invoker().onPlayerDeath(adminPlayer, cause);

    assertTrue(result.isCancelled());
  }

  @Test
  @DisplayName("should execute listeners in priority order")
  void shouldExecuteInPriorityOrder() {
    List<String> order = new ArrayList<>();

    PlayerDeathCallback.EVENT.register(EventPriority.HIGHEST, (player, cause) -> {
      order.add("HIGHEST");
      return EventResult.PASS;
    });
    PlayerDeathCallback.EVENT.register(EventPriority.LOWEST, (player, cause) -> {
      order.add("LOWEST");
      return EventResult.PASS;
    });
    PlayerDeathCallback.EVENT.register(EventPriority.NORMAL, (player, cause) -> {
      order.add("NORMAL");
      return EventResult.PASS;
    });

    TestPlayer player = new TestPlayer("TestPlayer");
    DeathCause cause = DeathCause.of(DeathCause.Type.FALL);
    PlayerDeathCallback.EVENT.invoker().onPlayerDeath(player, cause);

    assertEquals(List.of("HIGHEST", "NORMAL", "LOWEST"), order);
  }

  @Test
  @DisplayName("should stop execution on cancellation")
  void shouldStopExecutionOnCancellation() {
    List<String> executed = new ArrayList<>();

    PlayerDeathCallback.EVENT.register(EventPriority.HIGHEST, (player, cause) -> {
      executed.add("HIGHEST");
      return EventResult.CANCEL;
    });

    PlayerDeathCallback.EVENT.register(EventPriority.NORMAL, (player, cause) -> {
      executed.add("NORMAL");
      return EventResult.PASS;
    });

    TestPlayer player = new TestPlayer("TestPlayer");
    DeathCause cause = DeathCause.of(DeathCause.Type.FALL);
    EventResult result = PlayerDeathCallback.EVENT.invoker().onPlayerDeath(player, cause);

    assertTrue(result.isCancelled());
    assertEquals(List.of("HIGHEST"), executed);
  }

  @Test
  @DisplayName("should return PASS when no listeners are registered")
  void shouldReturnPassWithNoListeners() {
    TestPlayer player = new TestPlayer("TestPlayer");
    DeathCause cause = DeathCause.of(DeathCause.Type.FALL);
    EventResult result = PlayerDeathCallback.EVENT.invoker().onPlayerDeath(player, cause);

    assertFalse(result.isCancelled());
    assertEquals(EventResult.PASS, result);
  }

  @Test
  @DisplayName("should send message to player on death")
  void shouldSendMessageOnDeath() {
    PlayerDeathCallback.EVENT.register((player, cause) -> {
      player.sendMessage("You died from " + cause.getType() + "!");
      return EventResult.PASS;
    });

    TestPlayer player = new TestPlayer("TestPlayer");
    DeathCause cause = DeathCause.of(DeathCause.Type.DROWNING);
    PlayerDeathCallback.EVENT.invoker().onPlayerDeath(player, cause);

    assertEquals("You died from DROWNING!", player.getLastMessage());
  }

  @Test
  @DisplayName("should notify killer when player is killed by another player")
  void shouldNotifyKillerOnPlayerKill() {
    PlayerDeathCallback.EVENT.register((player, cause) -> {
      if (cause.isPlayerKill() && cause.getKiller() instanceof TestPlayer killer) {
        killer.sendMessage("You killed " + player.getDisplayName() + "!");
      }
      return EventResult.PASS;
    });

    TestPlayer victim = new TestPlayer("Victim");
    TestPlayer killer = new TestPlayer("Killer");
    DeathCause cause = DeathCause.byPlayer(killer);
    PlayerDeathCallback.EVENT.invoker().onPlayerDeath(victim, cause);

    assertEquals("You killed Victim!", killer.getLastMessage());
  }

  @Test
  @DisplayName("should handle mob kills correctly")
  void shouldHandleMobKills() {
    List<String> mobTypes = new ArrayList<>();

    PlayerDeathCallback.EVENT.register((player, cause) -> {
      if (cause.getType() == DeathCause.Type.MOB_KILL && cause.hasKiller()) {
        mobTypes.add(cause.getKiller().getUniqueId());
      }
      return EventResult.PASS;
    });

    TestPlayer player = new TestPlayer("Player");
    dev.polv.taleapi.testutil.TestEntity zombie = new dev.polv.taleapi.testutil.TestEntity("zombie-123", "zombie");
    DeathCause cause = DeathCause.byMob(zombie);
    PlayerDeathCallback.EVENT.invoker().onPlayerDeath(player, cause);

    assertEquals(1, mobTypes.size());
    assertEquals("zombie-123", mobTypes.get(0));
  }
}
