package dev.polv.taleapi.command.suggestion;

import java.util.Objects;

/**
 * A single suggestion for command autocompletion.
 * <p>
 * Each suggestion has text to insert and an optional tooltip for display.
 * </p>
 */
public final class Suggestion implements Comparable<Suggestion> {

  private final StringRange range;
  private final String text;
  private final String tooltip;

  /**
   * Creates a suggestion without a tooltip.
   *
   * @param range the range in the input to replace
   * @param text  the text to suggest
   */
  public Suggestion(StringRange range, String text) {
    this(range, text, null);
  }

  /**
   * Creates a suggestion with a tooltip.
   *
   * @param range   the range in the input to replace
   * @param text    the text to suggest
   * @param tooltip the tooltip to display (may be null)
   */
  public Suggestion(StringRange range, String text, String tooltip) {
    this.range = Objects.requireNonNull(range, "range");
    this.text = Objects.requireNonNull(text, "text");
    this.tooltip = tooltip;
  }

  /**
   * Returns the range in the input string that this suggestion would replace.
   *
   * @return the replacement range
   */
  public StringRange getRange() {
    return range;
  }

  /**
   * Returns the suggested text.
   *
   * @return the suggestion text
   */
  public String getText() {
    return text;
  }

  /**
   * Returns the tooltip, if any.
   *
   * @return the tooltip, or null if none
   */
  public String getTooltip() {
    return tooltip;
  }

  /**
   * Applies this suggestion to the given input string.
   *
   * @param input the original input
   * @return the input with this suggestion applied
   */
  public String apply(String input) {
    if (range.getStart() == 0 && range.getEnd() == input.length()) {
      return text;
    }
    StringBuilder result = new StringBuilder();
    if (range.getStart() > 0) {
      result.append(input, 0, range.getStart());
    }
    result.append(text);
    if (range.getEnd() < input.length()) {
      result.append(input, range.getEnd(), input.length());
    }
    return result.toString();
  }

  @Override
  public int compareTo(Suggestion other) {
    return text.compareToIgnoreCase(other.text);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof Suggestion other)) return false;
    return range.equals(other.range) && text.equals(other.text)
        && Objects.equals(tooltip, other.tooltip);
  }

  @Override
  public int hashCode() {
    return Objects.hash(range, text, tooltip);
  }

  @Override
  public String toString() {
    return "Suggestion{" +
        "range=" + range +
        ", text='" + text + '\'' +
        (tooltip != null ? ", tooltip='" + tooltip + '\'' : "") +
        '}';
  }
}

