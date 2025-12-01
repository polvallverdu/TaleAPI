package dev.polv.taleapi.testutil;

import dev.polv.taleapi.entity.TaleEntity;
import dev.polv.taleapi.world.Location;

import java.util.UUID;

/**
 * Test implementation of TaleEntity for testing purposes.
 * <p>
 * This class provides a simple in-memory implementation of the TaleEntity
 * interface that can be used for testing entity-related functionality.
 * </p>
 */
public class TestEntity implements TaleEntity {
  private final String uuid;
  private final String type;
  private Location location;

  /**
   * Creates a new test entity with a randomly generated UUID.
   *
   * @param type the entity type (e.g., "zombie", "pig")
   */
  public TestEntity(String type) {
    this.uuid = UUID.randomUUID().toString();
    this.type = type;
    this.location = new Location(0, 0, 0);
  }

  /**
   * Creates a new test entity with the specified UUID.
   *
   * @param uuid the unique identifier
   * @param type the entity type
   */
  public TestEntity(String uuid, String type) {
    this.uuid = uuid;
    this.type = type;
    this.location = new Location(0, 0, 0);
  }

  /**
   * Creates a new test entity with the specified UUID and location.
   *
   * @param uuid     the unique identifier
   * @param type     the entity type
   * @param location the initial location
   */
  public TestEntity(String uuid, String type, Location location) {
    this.uuid = uuid;
    this.type = type;
    this.location = location;
  }

  @Override
  public String getUniqueId() {
    return uuid;
  }

  @Override
  public Location getLocation() {
    return location;
  }

  @Override
  public void teleport(Location location) {
    this.location = location;
  }

  /**
   * @return the entity type
   */
  public String getType() {
    return type;
  }

  @Override
  public String toString() {
    return "TestEntity{" +
        "uuid='" + uuid + '\'' +
        ", type='" + type + '\'' +
        ", location=" + location +
        '}';
  }
}
