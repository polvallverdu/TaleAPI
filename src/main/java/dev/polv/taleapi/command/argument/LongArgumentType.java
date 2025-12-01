package dev.polv.taleapi.command.argument;

import dev.polv.taleapi.command.CommandContext;
import dev.polv.taleapi.command.CommandException;
import dev.polv.taleapi.command.suggestion.Suggestions;
import dev.polv.taleapi.command.suggestion.SuggestionsBuilder;

import java.util.concurrent.CompletableFuture;

/**
 * Argument type for long values with optional bounds.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Any long
 * Command.argument("id", LongArgumentType.longArg())
 *
 * // Long >= 0
 * Command.argument("timestamp", LongArgumentType.longArg(0))
 *
 * // Long between bounds
 * Command.argument("value", LongArgumentType.longArg(0, 1000000))
 * }</pre>
 */
public class LongArgumentType implements ArgumentType<Long> {

  private final long minimum;
  private final long maximum;

  private LongArgumentType(long minimum, long maximum) {
    this.minimum = minimum;
    this.maximum = maximum;
  }

  /**
   * Creates a long argument type with no bounds.
   *
   * @return a new long argument type
   */
  public static LongArgumentType longArg() {
    return new LongArgumentType(Long.MIN_VALUE, Long.MAX_VALUE);
  }

  /**
   * Creates a long argument type with a minimum bound.
   *
   * @param min the minimum value (inclusive)
   * @return a new long argument type
   */
  public static LongArgumentType longArg(long min) {
    return new LongArgumentType(min, Long.MAX_VALUE);
  }

  /**
   * Creates a long argument type with both bounds.
   *
   * @param min the minimum value (inclusive)
   * @param max the maximum value (inclusive)
   * @return a new long argument type
   */
  public static LongArgumentType longArg(long min, long max) {
    return new LongArgumentType(min, max);
  }

  /**
   * Returns the minimum value.
   *
   * @return the minimum bound
   */
  public long getMinimum() {
    return minimum;
  }

  /**
   * Returns the maximum value.
   *
   * @return the maximum bound
   */
  public long getMaximum() {
    return maximum;
  }

  @Override
  public Long parse(StringReader reader) throws CommandException {
    int start = reader.getCursor();
    long value = reader.readLong();
    if (value < minimum) {
      reader.setCursor(start);
      throw CommandException.argument("long",
          "Value must be at least " + minimum + ", found " + value);
    }
    if (value > maximum) {
      reader.setCursor(start);
      throw CommandException.argument("long",
          "Value must be at most " + maximum + ", found " + value);
    }
    return value;
  }

  @Override
  public CompletableFuture<Suggestions> listSuggestions(CommandContext context, SuggestionsBuilder builder) {
    if (minimum <= 0 && maximum >= 0) {
      builder.suggest("0");
    }
    if (minimum <= 1 && maximum >= 1) {
      builder.suggest("1");
    }
    return builder.buildFuture();
  }

  @Override
  public Class<Long> getResultType() {
    return Long.class;
  }

  @Override
  public String getTypeName() {
    if (minimum == Long.MIN_VALUE && maximum == Long.MAX_VALUE) {
      return "long";
    } else if (maximum == Long.MAX_VALUE) {
      return "long (>= " + minimum + ")";
    } else if (minimum == Long.MIN_VALUE) {
      return "long (<= " + maximum + ")";
    } else {
      return "long (" + minimum + ".." + maximum + ")";
    }
  }

  @Override
  public String[] getExamples() {
    return new String[]{"0", "1", "123456789"};
  }
}

