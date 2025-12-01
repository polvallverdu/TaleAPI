package dev.polv.taleapi.command.argument;

import dev.polv.taleapi.command.CommandContext;
import dev.polv.taleapi.command.CommandException;
import dev.polv.taleapi.command.suggestion.Suggestions;
import dev.polv.taleapi.command.suggestion.SuggestionsBuilder;

import java.util.concurrent.CompletableFuture;

/**
 * Defines how to parse and suggest a command argument.
 * <p>
 * Each argument type knows how to:
 * </p>
 * <ul>
 *   <li>Parse a string input into a typed value</li>
 *   <li>Provide tab-completion suggestions</li>
 *   <li>Describe the expected format for error messages</li>
 * </ul>
 *
 * <h2>Built-in Types</h2>
 * <ul>
 *   <li>{@link StringArgumentType} - String arguments (word, greedy, quoted)</li>
 *   <li>{@link IntegerArgumentType} - Integer arguments with optional bounds</li>
 *   <li>{@link DoubleArgumentType} - Double arguments with optional bounds</li>
 *   <li>{@link BooleanArgumentType} - Boolean arguments (true/false)</li>
 * </ul>
 *
 * @param <T> the type this argument parses to
 */
public interface ArgumentType<T> {

  /**
   * Parses the argument from the input string.
   *
   * @param reader the string reader positioned at the argument
   * @return the parsed value
   * @throws CommandException if the argument cannot be parsed
   */
  T parse(StringReader reader) throws CommandException;

  /**
   * Provides suggestions for this argument.
   * <p>
   * Default implementation returns no suggestions.
   * </p>
   *
   * @param context the current command context (may have partial arguments)
   * @param builder the suggestions builder
   * @return a future that completes with suggestions
   */
  default CompletableFuture<Suggestions> listSuggestions(CommandContext context, SuggestionsBuilder builder) {
    return builder.buildFuture();
  }

  /**
   * Returns example values for this argument type.
   * <p>
   * Used for documentation and error messages.
   * </p>
   *
   * @return an array of example values
   */
  default String[] getExamples() {
    return new String[0];
  }

  /**
   * Returns the Java type this argument parses to.
   *
   * @return the result type class
   */
  Class<T> getResultType();

  /**
   * Returns a human-readable name for this argument type.
   * <p>
   * Used in error messages and help text.
   * </p>
   *
   * @return the type name (e.g., "integer", "string", "player")
   */
  default String getTypeName() {
    return getResultType().getSimpleName().toLowerCase();
  }
}

