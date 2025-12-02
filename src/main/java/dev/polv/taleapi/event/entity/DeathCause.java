package dev.polv.taleapi.event.entity;

import dev.polv.taleapi.entity.TaleEntity;
import dev.polv.taleapi.entity.TalePlayer;

/**
 * Represents the cause of an entity's death.
 * <p>
 * Use {@link #getKiller()} to retrieve the entity responsible for the kill,
 * if applicable. For environmental deaths (fall, void, fire), the killer will
 * be null.
 * </p>
 *
 * <h2>Example Usage</h2>
 * 
 * <pre>{@code
 * EntityDeathCallback.EVENT.register((entity, cause) -> {
 *   if (cause.getType() == DeathCause.Type.PLAYER_KILL) {
 *     TalePlayer killer = (TalePlayer) cause.getKiller();
 *     killer.sendMessage("You killed " + entity.getUniqueId());
 *   }
 *   return EventResult.PASS;
 * });
 * }</pre>
 */
public final class DeathCause {

  /**
   * The type of death.
   */
  public enum Type {
    /**
     * Killed by a player.
     */
    PLAYER_KILL,

    /**
     * Killed by a non-player entity (mob, NPC, etc.).
     */
    MOB_KILL,

    /**
     * Died from fall damage.
     */
    FALL,

    /**
     * Died from drowning.
     */
    DROWNING,

    /**
     * Died from fire or lava.
     */
    FIRE,

    /**
     * Died from falling into the void.
     */
    VOID,

    /**
     * Died from starvation.
     */
    STARVATION,

    /**
     * Died from suffocation (inside blocks).
     */
    SUFFOCATION,

    /**
     * Died from explosion.
     */
    EXPLOSION,

    /**
     * Died from magic/potion damage.
     */
    MAGIC,

    /**
     * Died from projectile (arrow, etc.).
     */
    PROJECTILE,

    /**
     * Death cause is unknown or unspecified.
     */
    UNKNOWN
  }

  private final Type type;
  private final TaleEntity killer;

  private DeathCause(Type type, TaleEntity killer) {
    this.type = type;
    this.killer = killer;
  }

  /**
   * Creates a death cause with the specified type and no killer.
   *
   * @param type the type of death
   * @return a new DeathCause instance
   */
  public static DeathCause of(Type type) {
    return new DeathCause(type, null);
  }

  /**
   * Creates a death cause where a player killed the entity.
   *
   * @param killer the player who killed the entity
   * @return a new DeathCause instance
   */
  public static DeathCause byPlayer(TalePlayer killer) {
    return new DeathCause(Type.PLAYER_KILL, killer);
  }

  /**
   * Creates a death cause where a non-player entity killed the entity.
   *
   * @param killer the entity that killed this entity
   * @return a new DeathCause instance
   */
  public static DeathCause byMob(TaleEntity killer) {
    return new DeathCause(Type.MOB_KILL, killer);
  }

  /**
   * Creates a death cause with a custom type and killer.
   *
   * @param type   the type of death
   * @param killer the entity responsible for the death (may be null)
   * @return a new DeathCause instance
   */
  public static DeathCause of(Type type, TaleEntity killer) {
    return new DeathCause(type, killer);
  }

  /**
   * Returns the type of death.
   *
   * @return the death type
   */
  public Type getType() {
    return type;
  }

  /**
   * Returns the entity responsible for the kill, if any.
   * <p>
   * This will be non-null for {@link Type#PLAYER_KILL} and {@link Type#MOB_KILL}.
   * For environmental deaths, this returns null.
   * </p>
   *
   * @return the killer entity, or null if not applicable
   */
  public TaleEntity getKiller() {
    return killer;
  }

  /**
   * Checks if this death was caused by another entity.
   *
   * @return {@code true} if there was a killer, {@code false} otherwise
   */
  public boolean hasKiller() {
    return killer != null;
  }

  /**
   * Checks if the killer was a player.
   *
   * @return {@code true} if killed by a player
   */
  public boolean isPlayerKill() {
    return type == Type.PLAYER_KILL;
  }

  @Override
  public String toString() {
    return "DeathCause{" +
        "type=" + type +
        ", killer=" + (killer != null ? killer.getUniqueId() : "none") +
        '}';
  }
}
