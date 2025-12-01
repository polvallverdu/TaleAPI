package dev.polv.taleapi.command.suggestion;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Builder for creating {@link Suggestions}.
 * <p>
 * Used by argument types to provide autocompletion suggestions.
 * </p>
 */
public final class SuggestionsBuilder {

  private final String input;
  private final int start;
  private final String remaining;
  private final String remainingLowerCase;
  private final List<Suggestion> suggestions = new ArrayList<>();

  /**
   * Creates a new suggestions builder.
   *
   * @param input the full input string
   * @param start the position where suggestions start
   */
  public SuggestionsBuilder(String input, int start) {
    this.input = input;
    this.start = start;
    this.remaining = input.substring(start);
    this.remainingLowerCase = remaining.toLowerCase();
  }

  /**
   * Returns the full input string.
   *
   * @return the input
   */
  public String getInput() {
    return input;
  }

  /**
   * Returns the position where suggestions start.
   *
   * @return the start position
   */
  public int getStart() {
    return start;
  }

  /**
   * Returns the remaining portion of the input after the start position.
   *
   * @return the remaining input
   */
  public String getRemaining() {
    return remaining;
  }

  /**
   * Returns the remaining portion in lowercase.
   *
   * @return the lowercase remaining input
   */
  public String getRemainingLowerCase() {
    return remainingLowerCase;
  }

  /**
   * Adds a suggestion.
   *
   * @param text the suggestion text
   * @return this builder
   */
  public SuggestionsBuilder suggest(String text) {
    if (text.toLowerCase().startsWith(remainingLowerCase)) {
      suggestions.add(new Suggestion(StringRange.between(start, input.length()), text));
    }
    return this;
  }

  /**
   * Adds a suggestion with a tooltip.
   *
   * @param text    the suggestion text
   * @param tooltip the tooltip
   * @return this builder
   */
  public SuggestionsBuilder suggest(String text, String tooltip) {
    if (text.toLowerCase().startsWith(remainingLowerCase)) {
      suggestions.add(new Suggestion(StringRange.between(start, input.length()), text, tooltip));
    }
    return this;
  }

  /**
   * Adds an integer suggestion.
   *
   * @param value the integer value
   * @return this builder
   */
  public SuggestionsBuilder suggest(int value) {
    return suggest(String.valueOf(value));
  }

  /**
   * Adds an integer suggestion with a tooltip.
   *
   * @param value   the integer value
   * @param tooltip the tooltip
   * @return this builder
   */
  public SuggestionsBuilder suggest(int value, String tooltip) {
    return suggest(String.valueOf(value), tooltip);
  }

  /**
   * Adds multiple suggestions.
   *
   * @param texts the suggestion texts
   * @return this builder
   */
  public SuggestionsBuilder suggest(Iterable<String> texts) {
    for (String text : texts) {
      suggest(text);
    }
    return this;
  }

  /**
   * Builds the suggestions.
   *
   * @return the built suggestions
   */
  public Suggestions build() {
    if (suggestions.isEmpty()) {
      return Suggestions.empty();
    }
    List<Suggestion> sorted = new ArrayList<>(suggestions);
    sorted.sort(null);
    return new Suggestions(StringRange.between(start, input.length()), sorted);
  }

  /**
   * Builds the suggestions as a completed future.
   *
   * @return a future containing the suggestions
   */
  public CompletableFuture<Suggestions> buildFuture() {
    return CompletableFuture.completedFuture(build());
  }

  /**
   * Creates a new builder restarting at the given position.
   *
   * @param start the new start position
   * @return a new builder
   */
  public SuggestionsBuilder restart(int start) {
    return new SuggestionsBuilder(input, start);
  }

  /**
   * Creates a new builder with the same input but cleared suggestions.
   *
   * @return a new builder
   */
  public SuggestionsBuilder createOffset(int start) {
    return new SuggestionsBuilder(input, start);
  }
}

