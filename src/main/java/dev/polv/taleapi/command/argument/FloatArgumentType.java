package dev.polv.taleapi.command.argument;

import dev.polv.taleapi.command.CommandContext;
import dev.polv.taleapi.command.CommandException;
import dev.polv.taleapi.command.suggestion.Suggestions;
import dev.polv.taleapi.command.suggestion.SuggestionsBuilder;

import java.util.concurrent.CompletableFuture;

/**
 * Argument type for float values with optional bounds.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Any float
 * Command.argument("value", FloatArgumentType.floatArg())
 *
 * // Float >= 0.0f
 * Command.argument("amount", FloatArgumentType.floatArg(0.0f))
 *
 * // Float between 0.0f and 1.0f
 * Command.argument("percent", FloatArgumentType.floatArg(0.0f, 1.0f))
 * }</pre>
 */
public class FloatArgumentType implements ArgumentType<Float> {

  private final float minimum;
  private final float maximum;

  private FloatArgumentType(float minimum, float maximum) {
    this.minimum = minimum;
    this.maximum = maximum;
  }

  /**
   * Creates a float argument type with no bounds.
   *
   * @return a new float argument type
   */
  public static FloatArgumentType floatArg() {
    return new FloatArgumentType(-Float.MAX_VALUE, Float.MAX_VALUE);
  }

  /**
   * Creates a float argument type with a minimum bound.
   *
   * @param min the minimum value (inclusive)
   * @return a new float argument type
   */
  public static FloatArgumentType floatArg(float min) {
    return new FloatArgumentType(min, Float.MAX_VALUE);
  }

  /**
   * Creates a float argument type with both bounds.
   *
   * @param min the minimum value (inclusive)
   * @param max the maximum value (inclusive)
   * @return a new float argument type
   */
  public static FloatArgumentType floatArg(float min, float max) {
    return new FloatArgumentType(min, max);
  }

  /**
   * Returns the minimum value.
   *
   * @return the minimum bound
   */
  public float getMinimum() {
    return minimum;
  }

  /**
   * Returns the maximum value.
   *
   * @return the maximum bound
   */
  public float getMaximum() {
    return maximum;
  }

  @Override
  public Float parse(StringReader reader) throws CommandException {
    int start = reader.getCursor();
    float value = reader.readFloat();
    if (value < minimum) {
      reader.setCursor(start);
      throw CommandException.argument("float",
          "Value must be at least " + minimum + ", found " + value);
    }
    if (value > maximum) {
      reader.setCursor(start);
      throw CommandException.argument("float",
          "Value must be at most " + maximum + ", found " + value);
    }
    return value;
  }

  @Override
  public CompletableFuture<Suggestions> listSuggestions(CommandContext context, SuggestionsBuilder builder) {
    if (minimum <= 0.0f && maximum >= 0.0f) {
      builder.suggest("0.0");
    }
    if (minimum <= 1.0f && maximum >= 1.0f) {
      builder.suggest("1.0");
    }
    return builder.buildFuture();
  }

  @Override
  public Class<Float> getResultType() {
    return Float.class;
  }

  @Override
  public String getTypeName() {
    if (minimum == -Float.MAX_VALUE && maximum == Float.MAX_VALUE) {
      return "float";
    } else if (maximum == Float.MAX_VALUE) {
      return "float (>= " + minimum + ")";
    } else if (minimum == -Float.MAX_VALUE) {
      return "float (<= " + maximum + ")";
    } else {
      return "float (" + minimum + ".." + maximum + ")";
    }
  }

  @Override
  public String[] getExamples() {
    return new String[]{"0.0", "1.5", "3.14"};
  }
}

