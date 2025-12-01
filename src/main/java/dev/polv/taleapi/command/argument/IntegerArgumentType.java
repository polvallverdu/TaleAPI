package dev.polv.taleapi.command.argument;

import dev.polv.taleapi.command.CommandContext;
import dev.polv.taleapi.command.CommandException;
import dev.polv.taleapi.command.suggestion.Suggestions;
import dev.polv.taleapi.command.suggestion.SuggestionsBuilder;

import java.util.concurrent.CompletableFuture;

/**
 * Argument type for integer values with optional bounds.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Any integer
 * Command.argument("count", IntegerArgumentType.integer())
 *
 * // Integer >= 0
 * Command.argument("amount", IntegerArgumentType.integer(0))
 *
 * // Integer between 1 and 100
 * Command.argument("level", IntegerArgumentType.integer(1, 100))
 * }</pre>
 */
public class IntegerArgumentType implements ArgumentType<Integer> {

  private final int minimum;
  private final int maximum;

  private IntegerArgumentType(int minimum, int maximum) {
    this.minimum = minimum;
    this.maximum = maximum;
  }

  /**
   * Creates an integer argument type with no bounds.
   *
   * @return a new integer argument type
   */
  public static IntegerArgumentType integer() {
    return new IntegerArgumentType(Integer.MIN_VALUE, Integer.MAX_VALUE);
  }

  /**
   * Creates an integer argument type with a minimum bound.
   *
   * @param min the minimum value (inclusive)
   * @return a new integer argument type
   */
  public static IntegerArgumentType integer(int min) {
    return new IntegerArgumentType(min, Integer.MAX_VALUE);
  }

  /**
   * Creates an integer argument type with both bounds.
   *
   * @param min the minimum value (inclusive)
   * @param max the maximum value (inclusive)
   * @return a new integer argument type
   */
  public static IntegerArgumentType integer(int min, int max) {
    return new IntegerArgumentType(min, max);
  }

  /**
   * Returns the minimum value.
   *
   * @return the minimum bound
   */
  public int getMinimum() {
    return minimum;
  }

  /**
   * Returns the maximum value.
   *
   * @return the maximum bound
   */
  public int getMaximum() {
    return maximum;
  }

  @Override
  public Integer parse(StringReader reader) throws CommandException {
    int start = reader.getCursor();
    int value = reader.readInt();
    if (value < minimum) {
      reader.setCursor(start);
      throw CommandException.argument("integer",
          "Value must be at least " + minimum + ", found " + value);
    }
    if (value > maximum) {
      reader.setCursor(start);
      throw CommandException.argument("integer",
          "Value must be at most " + maximum + ", found " + value);
    }
    return value;
  }

  @Override
  public CompletableFuture<Suggestions> listSuggestions(CommandContext context, SuggestionsBuilder builder) {
    // Suggest common values within bounds
    if (minimum <= 0 && maximum >= 0) {
      builder.suggest("0");
    }
    if (minimum <= 1 && maximum >= 1) {
      builder.suggest("1");
    }
    if (minimum <= 10 && maximum >= 10) {
      builder.suggest("10");
    }
    return builder.buildFuture();
  }

  @Override
  public Class<Integer> getResultType() {
    return Integer.class;
  }

  @Override
  public String getTypeName() {
    if (minimum == Integer.MIN_VALUE && maximum == Integer.MAX_VALUE) {
      return "integer";
    } else if (maximum == Integer.MAX_VALUE) {
      return "integer (>= " + minimum + ")";
    } else if (minimum == Integer.MIN_VALUE) {
      return "integer (<= " + maximum + ")";
    } else {
      return "integer (" + minimum + ".." + maximum + ")";
    }
  }

  @Override
  public String[] getExamples() {
    return new String[]{"0", "1", "123", "-456"};
  }
}

