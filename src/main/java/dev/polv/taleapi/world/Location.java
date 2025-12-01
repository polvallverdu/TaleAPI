package dev.polv.taleapi.world;

import java.util.Objects;

/**
 * Represents a position in the world with coordinates and optional rotation.
 * <p>
 * Locations are immutable. All modification methods return a new Location
 * instance.
 * </p>
 */
public final class Location {

  private final double x;
  private final double y;
  private final double z;
  private final float yaw;
  private final float pitch;

  /**
   * Creates a new location with the specified coordinates and no rotation.
   *
   * @param x the x coordinate
   * @param y the y coordinate
   * @param z the z coordinate
   */
  public Location(double x, double y, double z) {
    this(x, y, z, 0.0f, 0.0f);
  }

  /**
   * Creates a new location with the specified coordinates and rotation.
   *
   * @param x     the x coordinate
   * @param y     the y coordinate
   * @param z     the z coordinate
   * @param yaw   the yaw rotation (horizontal, 0-360)
   * @param pitch the pitch rotation (vertical, -90 to 90)
   */
  public Location(double x, double y, double z, float yaw, float pitch) {
    this.x = x;
    this.y = y;
    this.z = z;
    this.yaw = yaw;
    this.pitch = pitch;
  }

  /**
   * @return the x coordinate
   */
  public double x() {
    return x;
  }

  /**
   * @return the y coordinate
   */
  public double y() {
    return y;
  }

  /**
   * @return the z coordinate
   */
  public double z() {
    return z;
  }

  /**
   * @return the yaw rotation (horizontal angle)
   */
  public float yaw() {
    return yaw;
  }

  /**
   * @return the pitch rotation (vertical angle)
   */
  public float pitch() {
    return pitch;
  }

  /**
   * Creates a new location with modified x coordinate.
   *
   * @param x the new x coordinate
   * @return a new Location with the modified value
   */
  public Location withX(double x) {
    return new Location(x, this.y, this.z, this.yaw, this.pitch);
  }

  /**
   * Creates a new location with modified y coordinate.
   *
   * @param y the new y coordinate
   * @return a new Location with the modified value
   */
  public Location withY(double y) {
    return new Location(this.x, y, this.z, this.yaw, this.pitch);
  }

  /**
   * Creates a new location with modified z coordinate.
   *
   * @param z the new z coordinate
   * @return a new Location with the modified value
   */
  public Location withZ(double z) {
    return new Location(this.x, this.y, z, this.yaw, this.pitch);
  }

  /**
   * Creates a new location with modified rotation.
   *
   * @param yaw   the new yaw rotation
   * @param pitch the new pitch rotation
   * @return a new Location with the modified values
   */
  public Location withRotation(float yaw, float pitch) {
    return new Location(this.x, this.y, this.z, yaw, pitch);
  }

  /**
   * Creates a new location by adding the given offsets.
   *
   * @param dx the x offset
   * @param dy the y offset
   * @param dz the z offset
   * @return a new Location with the offsets applied
   */
  public Location add(double dx, double dy, double dz) {
    return new Location(this.x + dx, this.y + dy, this.z + dz, this.yaw, this.pitch);
  }

  /**
   * Calculates the distance between this location and another.
   *
   * @param other the other location
   * @return the distance between the two locations
   */
  public double distance(Location other) {
    double dx = this.x - other.x;
    double dy = this.y - other.y;
    double dz = this.z - other.z;
    return Math.sqrt(dx * dx + dy * dy + dz * dz);
  }

  /**
   * Calculates the squared distance between this location and another.
   * <p>
   * This is faster than {@link #distance(Location)} as it avoids the square root.
   * Useful for distance comparisons.
   * </p>
   *
   * @param other the other location
   * @return the squared distance between the two locations
   */
  public double distanceSquared(Location other) {
    double dx = this.x - other.x;
    double dy = this.y - other.y;
    double dz = this.z - other.z;
    return dx * dx + dy * dy + dz * dz;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof Location location))
      return false;
    return Double.compare(x, location.x) == 0
        && Double.compare(y, location.y) == 0
        && Double.compare(z, location.z) == 0
        && Float.compare(yaw, location.yaw) == 0
        && Float.compare(pitch, location.pitch) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(x, y, z, yaw, pitch);
  }

  @Override
  public String toString() {
    return "Location{x=" + x + ", y=" + y + ", z=" + z + ", yaw=" + yaw + ", pitch=" + pitch + "}";
  }
}
