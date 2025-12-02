package dev.polv.taleapi.event.entity;

import dev.polv.taleapi.testutil.TestEntity;
import dev.polv.taleapi.testutil.TestPlayer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DeathCause")
class DeathCauseTest {

  @Test
  @DisplayName("should create environmental death cause without killer")
  void shouldCreateEnvironmentalDeathCause() {
    DeathCause cause = DeathCause.of(DeathCause.Type.FALL);

    assertEquals(DeathCause.Type.FALL, cause.getType());
    assertNull(cause.getKiller());
    assertFalse(cause.hasKiller());
    assertFalse(cause.isPlayerKill());
  }

  @Test
  @DisplayName("should create player kill death cause")
  void shouldCreatePlayerKillDeathCause() {
    TestPlayer killer = new TestPlayer("killer");
    DeathCause cause = DeathCause.byPlayer(killer);

    assertEquals(DeathCause.Type.PLAYER_KILL, cause.getType());
    assertEquals(killer, cause.getKiller());
    assertTrue(cause.hasKiller());
    assertTrue(cause.isPlayerKill());
  }

  @Test
  @DisplayName("should create mob kill death cause")
  void shouldCreateMobKillDeathCause() {
    TestEntity killer = new TestEntity("zombie");
    DeathCause cause = DeathCause.byMob(killer);

    assertEquals(DeathCause.Type.MOB_KILL, cause.getType());
    assertEquals(killer, cause.getKiller());
    assertTrue(cause.hasKiller());
    assertFalse(cause.isPlayerKill());
  }

  @Test
  @DisplayName("should create death cause with custom type and killer")
  void shouldCreateCustomDeathCause() {
    TestEntity shooter = new TestEntity("skeleton");
    DeathCause cause = DeathCause.of(DeathCause.Type.PROJECTILE, shooter);

    assertEquals(DeathCause.Type.PROJECTILE, cause.getType());
    assertEquals(shooter, cause.getKiller());
    assertTrue(cause.hasKiller());
  }

  @Test
  @DisplayName("should have all death types")
  void shouldHaveAllDeathTypes() {
    // Verify all expected death types exist
    assertNotNull(DeathCause.Type.PLAYER_KILL);
    assertNotNull(DeathCause.Type.MOB_KILL);
    assertNotNull(DeathCause.Type.FALL);
    assertNotNull(DeathCause.Type.DROWNING);
    assertNotNull(DeathCause.Type.FIRE);
    assertNotNull(DeathCause.Type.VOID);
    assertNotNull(DeathCause.Type.STARVATION);
    assertNotNull(DeathCause.Type.SUFFOCATION);
    assertNotNull(DeathCause.Type.EXPLOSION);
    assertNotNull(DeathCause.Type.MAGIC);
    assertNotNull(DeathCause.Type.PROJECTILE);
    assertNotNull(DeathCause.Type.UNKNOWN);
  }

  @Test
  @DisplayName("should provide meaningful toString")
  void shouldProvideMeaningfulToString() {
    DeathCause fallCause = DeathCause.of(DeathCause.Type.FALL);
    assertTrue(fallCause.toString().contains("FALL"));
    assertTrue(fallCause.toString().contains("none"));

    TestPlayer killer = new TestPlayer("id-123", "killer");
    DeathCause playerKillCause = DeathCause.byPlayer(killer);
    assertTrue(playerKillCause.toString().contains("PLAYER_KILL"));
    assertTrue(playerKillCause.toString().contains("id-123"));
  }
}
