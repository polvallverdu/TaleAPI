package dev.polv.taleapi.command.argument;

import dev.polv.taleapi.command.CommandException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ArgumentTypes")
class ArgumentTypeTest {

  @Nested
  @DisplayName("StringArgumentType")
  class StringArgumentTypeTests {

    @Test
    @DisplayName("word() should parse single word")
    void wordShouldParseSingleWord() {
      StringArgumentType type = StringArgumentType.word();
      StringReader reader = new StringReader("hello world");

      String result = type.parse(reader);

      assertEquals("hello", result);
      assertEquals(" world", reader.getRemaining());
    }

    @Test
    @DisplayName("word() should stop at whitespace")
    void wordShouldStopAtWhitespace() {
      StringArgumentType type = StringArgumentType.word();
      StringReader reader = new StringReader("test123 rest");

      String result = type.parse(reader);

      assertEquals("test123", result);
    }

    @Test
    @DisplayName("string() should parse quoted string")
    void stringShouldParseQuotedString() throws CommandException {
      StringArgumentType type = StringArgumentType.string();
      StringReader reader = new StringReader("\"hello world\" rest");

      String result = type.parse(reader);

      assertEquals("hello world", result);
    }

    @Test
    @DisplayName("string() should parse single quoted string")
    void stringShouldParseSingleQuotedString() throws CommandException {
      StringArgumentType type = StringArgumentType.string();
      StringReader reader = new StringReader("'hello world' rest");

      String result = type.parse(reader);

      assertEquals("hello world", result);
    }

    @Test
    @DisplayName("string() should handle escape sequences")
    void stringShouldHandleEscapeSequences() throws CommandException {
      StringArgumentType type = StringArgumentType.string();
      StringReader reader = new StringReader("\"hello \\\"world\\\"\"");

      String result = type.parse(reader);

      assertEquals("hello \"world\"", result);
    }

    @Test
    @DisplayName("greedyString() should consume all remaining")
    void greedyShouldConsumeAll() {
      StringArgumentType type = StringArgumentType.greedyString();
      StringReader reader = new StringReader("hello world this is a test");

      String result = type.parse(reader);

      assertEquals("hello world this is a test", result);
      assertFalse(reader.canRead());
    }
  }

  @Nested
  @DisplayName("IntegerArgumentType")
  class IntegerArgumentTypeTests {

    @Test
    @DisplayName("should parse positive integer")
    void shouldParsePositiveInteger() {
      IntegerArgumentType type = IntegerArgumentType.integer();
      StringReader reader = new StringReader("42 rest");

      int result = type.parse(reader);

      assertEquals(42, result);
    }

    @Test
    @DisplayName("should parse negative integer")
    void shouldParseNegativeInteger() {
      IntegerArgumentType type = IntegerArgumentType.integer();
      StringReader reader = new StringReader("-123");

      int result = type.parse(reader);

      assertEquals(-123, result);
    }

    @Test
    @DisplayName("should enforce minimum bound")
    void shouldEnforceMinimum() {
      IntegerArgumentType type = IntegerArgumentType.integer(0);
      StringReader reader = new StringReader("-5");

      assertThrows(CommandException.class, () -> type.parse(reader));
    }

    @Test
    @DisplayName("should enforce maximum bound")
    void shouldEnforceMaximum() {
      IntegerArgumentType type = IntegerArgumentType.integer(0, 10);
      StringReader reader = new StringReader("15");

      assertThrows(CommandException.class, () -> type.parse(reader));
    }

    @Test
    @DisplayName("should accept value at bounds")
    void shouldAcceptAtBounds() {
      IntegerArgumentType type = IntegerArgumentType.integer(1, 10);

      assertEquals(1, type.parse(new StringReader("1")));
      assertEquals(10, type.parse(new StringReader("10")));
    }

    @Test
    @DisplayName("should throw for invalid number")
    void shouldThrowForInvalidNumber() {
      IntegerArgumentType type = IntegerArgumentType.integer();
      StringReader reader = new StringReader("notanumber");

      assertThrows(CommandException.class, () -> type.parse(reader));
    }
  }

  @Nested
  @DisplayName("DoubleArgumentType")
  class DoubleArgumentTypeTests {

    @Test
    @DisplayName("should parse double")
    void shouldParseDouble() {
      DoubleArgumentType type = DoubleArgumentType.doubleArg();
      StringReader reader = new StringReader("3.14159");

      double result = type.parse(reader);

      assertEquals(3.14159, result, 0.00001);
    }

    @Test
    @DisplayName("should parse negative double")
    void shouldParseNegativeDouble() {
      DoubleArgumentType type = DoubleArgumentType.doubleArg();
      StringReader reader = new StringReader("-2.5");

      double result = type.parse(reader);

      assertEquals(-2.5, result, 0.001);
    }

    @Test
    @DisplayName("should parse integer as double")
    void shouldParseIntegerAsDouble() {
      DoubleArgumentType type = DoubleArgumentType.doubleArg();
      StringReader reader = new StringReader("42");

      double result = type.parse(reader);

      assertEquals(42.0, result, 0.001);
    }

    @Test
    @DisplayName("should parse scientific notation")
    void shouldParseScientificNotation() {
      DoubleArgumentType type = DoubleArgumentType.doubleArg();
      StringReader reader = new StringReader("1.5e10");

      double result = type.parse(reader);

      assertEquals(1.5e10, result, 1e5);
    }

    @Test
    @DisplayName("should enforce bounds")
    void shouldEnforceBounds() {
      DoubleArgumentType type = DoubleArgumentType.doubleArg(0.0, 1.0);

      assertThrows(CommandException.class, () -> type.parse(new StringReader("-0.5")));
      assertThrows(CommandException.class, () -> type.parse(new StringReader("1.5")));
    }
  }

  @Nested
  @DisplayName("BooleanArgumentType")
  class BooleanArgumentTypeTests {

    @Test
    @DisplayName("should parse true")
    void shouldParseTrue() {
      BooleanArgumentType type = BooleanArgumentType.bool();
      StringReader reader = new StringReader("true");

      assertTrue(type.parse(reader));
    }

    @Test
    @DisplayName("should parse false")
    void shouldParseFalse() {
      BooleanArgumentType type = BooleanArgumentType.bool();
      StringReader reader = new StringReader("false");

      assertFalse(type.parse(reader));
    }

    @Test
    @DisplayName("should be case-insensitive")
    void shouldBeCaseInsensitive() {
      BooleanArgumentType type = BooleanArgumentType.bool();

      assertTrue(type.parse(new StringReader("TRUE")));
      assertTrue(type.parse(new StringReader("True")));
      assertFalse(type.parse(new StringReader("FALSE")));
      assertFalse(type.parse(new StringReader("False")));
    }

    @Test
    @DisplayName("should throw for invalid boolean")
    void shouldThrowForInvalidBoolean() {
      BooleanArgumentType type = BooleanArgumentType.bool();
      StringReader reader = new StringReader("yes");

      assertThrows(CommandException.class, () -> type.parse(reader));
    }
  }

  @Nested
  @DisplayName("LongArgumentType")
  class LongArgumentTypeTests {

    @Test
    @DisplayName("should parse long")
    void shouldParseLong() {
      LongArgumentType type = LongArgumentType.longArg();
      StringReader reader = new StringReader("9223372036854775807");

      long result = type.parse(reader);

      assertEquals(Long.MAX_VALUE, result);
    }

    @Test
    @DisplayName("should enforce bounds")
    void shouldEnforceBounds() {
      LongArgumentType type = LongArgumentType.longArg(0, 100);

      assertThrows(CommandException.class, () -> type.parse(new StringReader("-1")));
      assertThrows(CommandException.class, () -> type.parse(new StringReader("101")));
    }
  }

  @Nested
  @DisplayName("FloatArgumentType")
  class FloatArgumentTypeTests {

    @Test
    @DisplayName("should parse float")
    void shouldParseFloat() {
      FloatArgumentType type = FloatArgumentType.floatArg();
      StringReader reader = new StringReader("3.14");

      float result = type.parse(reader);

      assertEquals(3.14f, result, 0.01f);
    }

    @Test
    @DisplayName("should enforce bounds")
    void shouldEnforceBounds() {
      FloatArgumentType type = FloatArgumentType.floatArg(0.0f, 1.0f);

      assertThrows(CommandException.class, () -> type.parse(new StringReader("-0.5")));
      assertThrows(CommandException.class, () -> type.parse(new StringReader("1.5")));
    }
  }

  @Nested
  @DisplayName("StringReader")
  class StringReaderTests {

    @Test
    @DisplayName("should read unquoted string")
    void shouldReadUnquotedString() {
      StringReader reader = new StringReader("hello world");

      assertEquals("hello", reader.readUnquotedString());
      assertEquals(" world", reader.getRemaining());
    }

    @Test
    @DisplayName("should skip whitespace")
    void shouldSkipWhitespace() {
      StringReader reader = new StringReader("   hello");
      reader.skipWhitespace();

      assertEquals("hello", reader.getRemaining());
    }

    @Test
    @DisplayName("should track cursor position")
    void shouldTrackCursor() {
      StringReader reader = new StringReader("hello world");

      assertEquals(0, reader.getCursor());
      reader.readUnquotedString();
      assertEquals(5, reader.getCursor());
    }

    @Test
    @DisplayName("should read remaining")
    void shouldReadRemaining() {
      StringReader reader = new StringReader("hello world");
      reader.skip();
      reader.skip();

      assertEquals("llo world", reader.readRemaining());
      assertFalse(reader.canRead());
    }

    @Test
    @DisplayName("should peek without advancing")
    void shouldPeekWithoutAdvancing() {
      StringReader reader = new StringReader("hello");

      assertEquals('h', reader.peek());
      assertEquals('h', reader.peek());
      assertEquals(0, reader.getCursor());
    }

    @Test
    @DisplayName("should read and advance")
    void shouldReadAndAdvance() {
      StringReader reader = new StringReader("hello");

      assertEquals('h', reader.read());
      assertEquals('e', reader.read());
      assertEquals(2, reader.getCursor());
    }
  }
}

