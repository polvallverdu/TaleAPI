package dev.polv.taleapi.command.suggestion;

import java.util.Objects;

/**
 * Represents a range within a string, used for suggestion placement.
 */
public final class StringRange {

  private final int start;
  private final int end;

  /**
   * Creates a new range.
   *
   * @param start the start index (inclusive)
   * @param end   the end index (exclusive)
   */
  public StringRange(int start, int end) {
    this.start = start;
    this.end = end;
  }

  /**
   * Creates a range at a single position.
   *
   * @param pos the position
   * @return a zero-length range at that position
   */
  public static StringRange at(int pos) {
    return new StringRange(pos, pos);
  }

  /**
   * Creates a range between two positions.
   *
   * @param start the start index
   * @param end   the end index
   * @return a new range
   */
  public static StringRange between(int start, int end) {
    return new StringRange(start, end);
  }

  /**
   * Creates a range encompassing both given ranges.
   *
   * @param a the first range
   * @param b the second range
   * @return a range spanning both
   */
  public static StringRange encompassing(StringRange a, StringRange b) {
    return new StringRange(Math.min(a.start, b.start), Math.max(a.end, b.end));
  }

  /**
   * Returns the start index (inclusive).
   *
   * @return the start index
   */
  public int getStart() {
    return start;
  }

  /**
   * Returns the end index (exclusive).
   *
   * @return the end index
   */
  public int getEnd() {
    return end;
  }

  /**
   * Returns the length of this range.
   *
   * @return the length
   */
  public int getLength() {
    return end - start;
  }

  /**
   * Checks if this range is empty.
   *
   * @return {@code true} if the range has zero length
   */
  public boolean isEmpty() {
    return start == end;
  }

  /**
   * Extracts the substring covered by this range.
   *
   * @param string the string to extract from
   * @return the substring
   */
  public String get(String string) {
    return string.substring(start, end);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof StringRange other)) return false;
    return start == other.start && end == other.end;
  }

  @Override
  public int hashCode() {
    return Objects.hash(start, end);
  }

  @Override
  public String toString() {
    return "StringRange{" + start + ".." + end + '}';
  }
}

