package dev.polv.taleapi.command.argument;

import dev.polv.taleapi.command.CommandException;

/**
 * A simple string reader for parsing command arguments.
 * <p>
 * Provides methods for reading different types of values from a string,
 * tracking the current position, and handling whitespace.
 * </p>
 */
public class StringReader {

  private static final char SYNTAX_ESCAPE = '\\';
  private static final char SYNTAX_DOUBLE_QUOTE = '"';
  private static final char SYNTAX_SINGLE_QUOTE = '\'';

  private final String string;
  private int cursor;

  /**
   * Creates a new StringReader for the given input.
   *
   * @param string the input string to read
   */
  public StringReader(String string) {
    this.string = string;
    this.cursor = 0;
  }

  /**
   * Creates a new StringReader starting at a specific position.
   *
   * @param string the input string
   * @param cursor the starting position
   */
  public StringReader(String string, int cursor) {
    this.string = string;
    this.cursor = cursor;
  }

  /**
   * Returns the full input string.
   *
   * @return the input string
   */
  public String getString() {
    return string;
  }

  /**
   * Returns the current cursor position.
   *
   * @return the cursor position
   */
  public int getCursor() {
    return cursor;
  }

  /**
   * Sets the cursor position.
   *
   * @param cursor the new position
   */
  public void setCursor(int cursor) {
    this.cursor = cursor;
  }

  /**
   * Returns the number of characters remaining.
   *
   * @return remaining character count
   */
  public int getRemainingLength() {
    return string.length() - cursor;
  }

  /**
   * Returns the total length of the input string.
   *
   * @return the string length
   */
  public int getTotalLength() {
    return string.length();
  }

  /**
   * Returns the portion of the string that has been read.
   *
   * @return the read portion
   */
  public String getRead() {
    return string.substring(0, cursor);
  }

  /**
   * Returns the remaining unread portion of the string.
   *
   * @return the remaining portion
   */
  public String getRemaining() {
    return string.substring(cursor);
  }

  /**
   * Checks if there are more characters to read.
   *
   * @return {@code true} if more characters are available
   */
  public boolean canRead() {
    return cursor < string.length();
  }

  /**
   * Checks if the specified number of characters can be read.
   *
   * @param length the number of characters
   * @return {@code true} if that many characters are available
   */
  public boolean canRead(int length) {
    return cursor + length <= string.length();
  }

  /**
   * Returns the character at the current position without advancing.
   *
   * @return the current character
   */
  public char peek() {
    return string.charAt(cursor);
  }

  /**
   * Returns the character at the specified offset from current position.
   *
   * @param offset the offset from cursor
   * @return the character at that position
   */
  public char peek(int offset) {
    return string.charAt(cursor + offset);
  }

  /**
   * Reads and returns the character at the current position, advancing the cursor.
   *
   * @return the read character
   */
  public char read() {
    return string.charAt(cursor++);
  }

  /**
   * Skips the current character.
   */
  public void skip() {
    cursor++;
  }

  /**
   * Skips all whitespace characters at the current position.
   */
  public void skipWhitespace() {
    while (canRead() && Character.isWhitespace(peek())) {
      skip();
    }
  }

  /**
   * Reads an integer from the current position.
   *
   * @return the parsed integer
   * @throws CommandException if no valid integer is found
   */
  public int readInt() throws CommandException {
    int start = cursor;
    while (canRead() && isAllowedInNumber(peek())) {
      skip();
    }
    String number = string.substring(start, cursor);
    if (number.isEmpty()) {
      throw CommandException.syntax("Expected integer at position " + cursor);
    }
    try {
      return Integer.parseInt(number);
    } catch (NumberFormatException e) {
      cursor = start;
      throw CommandException.syntax("Invalid integer '" + number + "'");
    }
  }

  /**
   * Reads a long from the current position.
   *
   * @return the parsed long
   * @throws CommandException if no valid long is found
   */
  public long readLong() throws CommandException {
    int start = cursor;
    while (canRead() && isAllowedInNumber(peek())) {
      skip();
    }
    String number = string.substring(start, cursor);
    if (number.isEmpty()) {
      throw CommandException.syntax("Expected long at position " + cursor);
    }
    try {
      return Long.parseLong(number);
    } catch (NumberFormatException e) {
      cursor = start;
      throw CommandException.syntax("Invalid long '" + number + "'");
    }
  }

  /**
   * Reads a double from the current position.
   *
   * @return the parsed double
   * @throws CommandException if no valid double is found
   */
  public double readDouble() throws CommandException {
    int start = cursor;
    while (canRead() && isAllowedInDouble(peek())) {
      skip();
    }
    String number = string.substring(start, cursor);
    if (number.isEmpty()) {
      throw CommandException.syntax("Expected double at position " + cursor);
    }
    try {
      return Double.parseDouble(number);
    } catch (NumberFormatException e) {
      cursor = start;
      throw CommandException.syntax("Invalid double '" + number + "'");
    }
  }

  /**
   * Reads a float from the current position.
   *
   * @return the parsed float
   * @throws CommandException if no valid float is found
   */
  public float readFloat() throws CommandException {
    int start = cursor;
    while (canRead() && isAllowedInDouble(peek())) {
      skip();
    }
    String number = string.substring(start, cursor);
    if (number.isEmpty()) {
      throw CommandException.syntax("Expected float at position " + cursor);
    }
    try {
      return Float.parseFloat(number);
    } catch (NumberFormatException e) {
      cursor = start;
      throw CommandException.syntax("Invalid float '" + number + "'");
    }
  }

  /**
   * Reads a boolean from the current position.
   *
   * @return the parsed boolean
   * @throws CommandException if no valid boolean is found
   */
  public boolean readBoolean() throws CommandException {
    int start = cursor;
    String value = readUnquotedString();
    if (value.isEmpty()) {
      throw CommandException.syntax("Expected boolean at position " + cursor);
    }
    if (value.equalsIgnoreCase("true")) {
      return true;
    } else if (value.equalsIgnoreCase("false")) {
      return false;
    } else {
      cursor = start;
      throw CommandException.syntax("Invalid boolean '" + value + "', expected 'true' or 'false'");
    }
  }

  /**
   * Reads an unquoted string (stops at whitespace).
   *
   * @return the read string
   */
  public String readUnquotedString() {
    int start = cursor;
    while (canRead() && isAllowedInUnquotedString(peek())) {
      skip();
    }
    return string.substring(start, cursor);
  }

  /**
   * Reads a quoted string (handles escape sequences).
   *
   * @return the read string (without quotes)
   * @throws CommandException if the string is malformed
   */
  public String readQuotedString() throws CommandException {
    if (!canRead()) {
      return "";
    }
    char quote = peek();
    if (!isQuote(quote)) {
      throw CommandException.syntax("Expected quote at position " + cursor);
    }
    skip();
    return readStringUntil(quote);
  }

  /**
   * Reads a string, handling both quoted and unquoted forms.
   *
   * @return the read string
   * @throws CommandException if a quoted string is malformed
   */
  public String readString() throws CommandException {
    if (!canRead()) {
      return "";
    }
    char next = peek();
    if (isQuote(next)) {
      skip();
      return readStringUntil(next);
    }
    return readUnquotedString();
  }

  /**
   * Reads all remaining characters as a string.
   *
   * @return the remaining string
   */
  public String readRemaining() {
    String result = getRemaining();
    cursor = string.length();
    return result;
  }

  private String readStringUntil(char terminator) throws CommandException {
    StringBuilder result = new StringBuilder();
    boolean escaped = false;
    while (canRead()) {
      char c = read();
      if (escaped) {
        if (c == terminator || c == SYNTAX_ESCAPE) {
          result.append(c);
          escaped = false;
        } else {
          cursor--;
          throw CommandException.syntax("Invalid escape sequence '\\" + c + "' at position " + cursor);
        }
      } else if (c == SYNTAX_ESCAPE) {
        escaped = true;
      } else if (c == terminator) {
        return result.toString();
      } else {
        result.append(c);
      }
    }
    throw CommandException.syntax("Unclosed quoted string starting at position " + cursor);
  }

  /**
   * Checks if the given character terminates an unquoted string.
   *
   * @param c the character to check
   * @return {@code true} if the character is allowed in an unquoted string
   */
  public static boolean isAllowedInUnquotedString(char c) {
    return c >= '0' && c <= '9'
        || c >= 'A' && c <= 'Z'
        || c >= 'a' && c <= 'z'
        || c == '_' || c == '-'
        || c == '.' || c == '+';
  }

  private static boolean isAllowedInNumber(char c) {
    return c >= '0' && c <= '9' || c == '-';
  }

  private static boolean isAllowedInDouble(char c) {
    return c >= '0' && c <= '9' || c == '.' || c == '-' || c == 'e' || c == 'E';
  }

  private static boolean isQuote(char c) {
    return c == SYNTAX_DOUBLE_QUOTE || c == SYNTAX_SINGLE_QUOTE;
  }

  /**
   * Expects and consumes a specific character.
   *
   * @param c the expected character
   * @throws CommandException if the character is not found
   */
  public void expect(char c) throws CommandException {
    if (!canRead() || peek() != c) {
      throw CommandException.syntax("Expected '" + c + "' at position " + cursor);
    }
    skip();
  }

  @Override
  public String toString() {
    return "StringReader{string='" + string + "', cursor=" + cursor + '}';
  }
}

