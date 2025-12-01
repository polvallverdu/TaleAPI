package dev.polv.taleapi.testutil;

import dev.polv.taleapi.entity.TalePlayer;
import dev.polv.taleapi.world.Location;

import java.util.UUID;

/**
 * Test implementation of TalePlayer for testing purposes.
 * <p>
 * This class provides a simple in-memory implementation of the TalePlayer
 * interface that can be used across all tests.
 * </p>
 */
public class TestPlayer implements TalePlayer {
  private final String uuid;
  private final String name;
  private Location location;

  /**
   * Creates a new test player with the given name and a randomly generated UUID.
   *
   * @param name the display name for the player
   */
  public TestPlayer(String name) {
    this.uuid = UUID.randomUUID().toString();
    this.name = name;
    this.location = new Location(0, 0, 0);
  }

  /**
   * Creates a new test player with the specified UUID and name.
   *
   * @param uuid the unique identifier
   * @param name the display name
   */
  public TestPlayer(String uuid, String name) {
    this.uuid = uuid;
    this.name = name;
    this.location = new Location(0, 0, 0);
  }

  @Override
  public String getUniqueId() {
    return uuid;
  }

  @Override
  public String getDisplayName() {
    return name;
  }

  @Override
  public Location getLocation() {
    return location;
  }

  @Override
  public void teleport(Location location) {
    this.location = location;
  }

  @Override
  public String toString() {
    return "TestPlayer{" +
        "uuid='" + uuid + '\'' +
        ", name='" + name + '\'' +
        '}';
  }
}
