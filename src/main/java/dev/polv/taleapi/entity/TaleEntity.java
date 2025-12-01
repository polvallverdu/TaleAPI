package dev.polv.taleapi.entity;

import dev.polv.taleapi.world.Location;

/**
 * Represents any entity in the world.
 * <p>
 * This is the base interface for all entities including players, mobs, NPCs,
 * and other game objects that exist in the world with a position.
 * </p>
 */
public interface TaleEntity {

  /**
   * Returns the unique identifier for this entity.
   * <p>
   * This ID is guaranteed to be unique across all entities in the server.
   * </p>
   *
   * @return the entity's unique identifier
   */
  String getUniqueId();

  /**
   * Returns the current location of this entity in the world.
   *
   * @return the entity's current location
   */
  Location getLocation();

  /**
   * Teleports the entity to the specified location.
   * <p>
   * This is an instant position change that bypasses normal movement.
   * Unlike regular movement, teleportation typically does not fire
   * movement events (implementation-dependent).
   * </p>
   *
   * @param location the destination location
   */
  void teleport(Location location);

}
