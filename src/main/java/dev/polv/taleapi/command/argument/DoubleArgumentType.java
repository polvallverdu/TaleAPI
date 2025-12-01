package dev.polv.taleapi.command.argument;

import dev.polv.taleapi.command.CommandContext;
import dev.polv.taleapi.command.CommandException;
import dev.polv.taleapi.command.suggestion.Suggestions;
import dev.polv.taleapi.command.suggestion.SuggestionsBuilder;

import java.util.concurrent.CompletableFuture;

/**
 * Argument type for double values with optional bounds.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Any double
 * Command.argument("value", DoubleArgumentType.doubleArg())
 *
 * // Double >= 0.0
 * Command.argument("amount", DoubleArgumentType.doubleArg(0.0))
 *
 * // Double between 0.0 and 1.0
 * Command.argument("percent", DoubleArgumentType.doubleArg(0.0, 1.0))
 * }</pre>
 */
public class DoubleArgumentType implements ArgumentType<Double> {

  private final double minimum;
  private final double maximum;

  private DoubleArgumentType(double minimum, double maximum) {
    this.minimum = minimum;
    this.maximum = maximum;
  }

  /**
   * Creates a double argument type with no bounds.
   *
   * @return a new double argument type
   */
  public static DoubleArgumentType doubleArg() {
    return new DoubleArgumentType(-Double.MAX_VALUE, Double.MAX_VALUE);
  }

  /**
   * Creates a double argument type with a minimum bound.
   *
   * @param min the minimum value (inclusive)
   * @return a new double argument type
   */
  public static DoubleArgumentType doubleArg(double min) {
    return new DoubleArgumentType(min, Double.MAX_VALUE);
  }

  /**
   * Creates a double argument type with both bounds.
   *
   * @param min the minimum value (inclusive)
   * @param max the maximum value (inclusive)
   * @return a new double argument type
   */
  public static DoubleArgumentType doubleArg(double min, double max) {
    return new DoubleArgumentType(min, max);
  }

  /**
   * Returns the minimum value.
   *
   * @return the minimum bound
   */
  public double getMinimum() {
    return minimum;
  }

  /**
   * Returns the maximum value.
   *
   * @return the maximum bound
   */
  public double getMaximum() {
    return maximum;
  }

  @Override
  public Double parse(StringReader reader) throws CommandException {
    int start = reader.getCursor();
    double value = reader.readDouble();
    if (value < minimum) {
      reader.setCursor(start);
      throw CommandException.argument("double",
          "Value must be at least " + minimum + ", found " + value);
    }
    if (value > maximum) {
      reader.setCursor(start);
      throw CommandException.argument("double",
          "Value must be at most " + maximum + ", found " + value);
    }
    return value;
  }

  @Override
  public CompletableFuture<Suggestions> listSuggestions(CommandContext context, SuggestionsBuilder builder) {
    // Suggest common values within bounds
    if (minimum <= 0.0 && maximum >= 0.0) {
      builder.suggest("0.0");
    }
    if (minimum <= 1.0 && maximum >= 1.0) {
      builder.suggest("1.0");
    }
    if (minimum <= 0.5 && maximum >= 0.5) {
      builder.suggest("0.5");
    }
    return builder.buildFuture();
  }

  @Override
  public Class<Double> getResultType() {
    return Double.class;
  }

  @Override
  public String getTypeName() {
    if (minimum == -Double.MAX_VALUE && maximum == Double.MAX_VALUE) {
      return "double";
    } else if (maximum == Double.MAX_VALUE) {
      return "double (>= " + minimum + ")";
    } else if (minimum == -Double.MAX_VALUE) {
      return "double (<= " + maximum + ")";
    } else {
      return "double (" + minimum + ".." + maximum + ")";
    }
  }

  @Override
  public String[] getExamples() {
    return new String[]{"0.0", "1.5", "3.14", "-2.7"};
  }
}

