package dev.polv.taleapi.command.suggestion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A collection of suggestions for command autocompletion.
 */
public final class Suggestions {

  private static final Suggestions EMPTY = new Suggestions(StringRange.at(0), Collections.emptyList());

  private final StringRange range;
  private final List<Suggestion> suggestions;

  /**
   * Creates a new Suggestions instance.
   *
   * @param range       the range these suggestions cover
   * @param suggestions the list of suggestions
   */
  public Suggestions(StringRange range, List<Suggestion> suggestions) {
    this.range = Objects.requireNonNull(range, "range");
    this.suggestions = Collections.unmodifiableList(new ArrayList<>(suggestions));
  }

  /**
   * Returns an empty suggestions instance.
   *
   * @return empty suggestions
   */
  public static Suggestions empty() {
    return EMPTY;
  }

  /**
   * Merges multiple suggestions into one.
   *
   * @param input       the input string
   * @param suggestions the suggestions to merge
   * @return merged suggestions
   */
  public static Suggestions merge(String input, Collection<Suggestions> suggestions) {
    if (suggestions.isEmpty()) {
      return EMPTY;
    }
    if (suggestions.size() == 1) {
      return suggestions.iterator().next();
    }

    List<Suggestion> merged = new ArrayList<>();
    for (Suggestions s : suggestions) {
      merged.addAll(s.suggestions);
    }

    if (merged.isEmpty()) {
      return EMPTY;
    }

    // Sort and deduplicate
    merged.sort(null);
    List<Suggestion> deduped = new ArrayList<>();
    String lastText = null;
    for (Suggestion s : merged) {
      if (!s.getText().equals(lastText)) {
        deduped.add(s);
        lastText = s.getText();
      }
    }

    // Find encompassing range
    StringRange range = deduped.get(0).getRange();
    for (int i = 1; i < deduped.size(); i++) {
      range = StringRange.encompassing(range, deduped.get(i).getRange());
    }

    return new Suggestions(range, deduped);
  }

  /**
   * Returns the range these suggestions cover.
   *
   * @return the range
   */
  public StringRange getRange() {
    return range;
  }

  /**
   * Returns the list of suggestions.
   *
   * @return the suggestions
   */
  public List<Suggestion> getList() {
    return suggestions;
  }

  /**
   * Checks if there are no suggestions.
   *
   * @return {@code true} if empty
   */
  public boolean isEmpty() {
    return suggestions.isEmpty();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof Suggestions other)) return false;
    return range.equals(other.range) && suggestions.equals(other.suggestions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(range, suggestions);
  }

  @Override
  public String toString() {
    return "Suggestions{" +
        "range=" + range +
        ", suggestions=" + suggestions +
        '}';
  }
}

