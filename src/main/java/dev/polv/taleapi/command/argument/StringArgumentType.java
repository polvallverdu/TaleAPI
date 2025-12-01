package dev.polv.taleapi.command.argument;

import dev.polv.taleapi.command.CommandException;

/**
 * Argument type for string values.
 * <p>
 * Provides three modes:
 * <ul>
 *   <li>{@link #word()} - Single word (no spaces, stops at whitespace)</li>
 *   <li>{@link #string()} - Quoted string or single word</li>
 *   <li>{@link #greedyString()} - Consumes all remaining input</li>
 * </ul>
 * </p>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Single word argument
 * Command.argument("name", StringArgumentType.word())
 *
 * // Quoted or single word
 * Command.argument("message", StringArgumentType.string())
 *
 * // All remaining input
 * Command.argument("reason", StringArgumentType.greedyString())
 * }</pre>
 */
public class StringArgumentType implements ArgumentType<String> {

  private final StringType type;

  private StringArgumentType(StringType type) {
    this.type = type;
  }

  /**
   * Creates a word argument type (single word, no spaces).
   *
   * @return a new word argument type
   */
  public static StringArgumentType word() {
    return new StringArgumentType(StringType.WORD);
  }

  /**
   * Creates a string argument type (quoted or single word).
   *
   * @return a new string argument type
   */
  public static StringArgumentType string() {
    return new StringArgumentType(StringType.QUOTABLE);
  }

  /**
   * Creates a greedy string argument type (all remaining input).
   *
   * @return a new greedy string argument type
   */
  public static StringArgumentType greedyString() {
    return new StringArgumentType(StringType.GREEDY);
  }

  /**
   * Returns the string type mode.
   *
   * @return the string type
   */
  public StringType getType() {
    return type;
  }

  @Override
  public String parse(StringReader reader) throws CommandException {
    return switch (type) {
      case WORD -> reader.readUnquotedString();
      case QUOTABLE -> reader.readString();
      case GREEDY -> {
        String remaining = reader.getRemaining();
        reader.setCursor(reader.getTotalLength());
        yield remaining;
      }
    };
  }

  @Override
  public Class<String> getResultType() {
    return String.class;
  }

  @Override
  public String getTypeName() {
    return switch (type) {
      case WORD -> "word";
      case QUOTABLE -> "string";
      case GREEDY -> "text";
    };
  }

  @Override
  public String[] getExamples() {
    return switch (type) {
      case WORD -> new String[]{"word", "hello", "test123"};
      case QUOTABLE -> new String[]{"word", "\"quoted string\"", "'single quoted'"};
      case GREEDY -> new String[]{"any text here", "multiple words allowed"};
    };
  }

  /**
   * The type of string parsing.
   */
  public enum StringType {
    /**
     * Single word (stops at whitespace).
     */
    WORD,

    /**
     * Can be quoted or a single word.
     */
    QUOTABLE,

    /**
     * Consumes all remaining input.
     */
    GREEDY
  }
}

