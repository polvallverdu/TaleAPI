package dev.polv.taleapi.command.suggestion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Suggestions")
class SuggestionTest {

  @Nested
  @DisplayName("Suggestion")
  class SuggestionTests {

    @Test
    @DisplayName("should create suggestion with text only")
    void shouldCreateWithTextOnly() {
      Suggestion suggestion = new Suggestion(StringRange.at(0), "test");

      assertEquals("test", suggestion.getText());
      assertNull(suggestion.getTooltip());
    }

    @Test
    @DisplayName("should create suggestion with tooltip")
    void shouldCreateWithTooltip() {
      Suggestion suggestion = new Suggestion(
          StringRange.at(0), "test", "A test suggestion");

      assertEquals("test", suggestion.getText());
      assertEquals("A test suggestion", suggestion.getTooltip());
    }

    @Test
    @DisplayName("should apply suggestion to input")
    void shouldApplyToInput() {
      Suggestion suggestion = new Suggestion(
          StringRange.between(6, 9), "world");

      String result = suggestion.apply("hello wor");

      assertEquals("hello world", result);
    }

    @Test
    @DisplayName("should apply suggestion replacing entire input")
    void shouldApplyReplacingAll() {
      Suggestion suggestion = new Suggestion(
          StringRange.between(0, 3), "hello");

      String result = suggestion.apply("hel");

      assertEquals("hello", result);
    }

    @Test
    @DisplayName("should compare suggestions by text")
    void shouldCompareByText() {
      Suggestion a = new Suggestion(StringRange.at(0), "apple");
      Suggestion b = new Suggestion(StringRange.at(0), "banana");
      Suggestion c = new Suggestion(StringRange.at(0), "Apple");

      assertTrue(a.compareTo(b) < 0);
      assertTrue(b.compareTo(a) > 0);
      assertEquals(0, a.compareTo(c)); // case insensitive
    }

    @Test
    @DisplayName("should implement equals and hashCode")
    void shouldImplementEqualsAndHashCode() {
      Suggestion a = new Suggestion(StringRange.at(0), "test", "tip");
      Suggestion b = new Suggestion(StringRange.at(0), "test", "tip");
      Suggestion c = new Suggestion(StringRange.at(0), "test", "different");

      assertEquals(a, b);
      assertEquals(a.hashCode(), b.hashCode());
      assertNotEquals(a, c);
    }
  }

  @Nested
  @DisplayName("StringRange")
  class StringRangeTests {

    @Test
    @DisplayName("should create range at position")
    void shouldCreateAtPosition() {
      StringRange range = StringRange.at(5);

      assertEquals(5, range.getStart());
      assertEquals(5, range.getEnd());
      assertTrue(range.isEmpty());
    }

    @Test
    @DisplayName("should create range between positions")
    void shouldCreateBetween() {
      StringRange range = StringRange.between(3, 7);

      assertEquals(3, range.getStart());
      assertEquals(7, range.getEnd());
      assertEquals(4, range.getLength());
      assertFalse(range.isEmpty());
    }

    @Test
    @DisplayName("should extract substring")
    void shouldExtractSubstring() {
      StringRange range = StringRange.between(0, 5);
      String result = range.get("hello world");

      assertEquals("hello", result);
    }

    @Test
    @DisplayName("should encompass two ranges")
    void shouldEncompassRanges() {
      StringRange a = StringRange.between(2, 5);
      StringRange b = StringRange.between(7, 10);
      StringRange result = StringRange.encompassing(a, b);

      assertEquals(2, result.getStart());
      assertEquals(10, result.getEnd());
    }
  }

  @Nested
  @DisplayName("Suggestions collection")
  class SuggestionsCollectionTests {

    @Test
    @DisplayName("should create empty suggestions")
    void shouldCreateEmpty() {
      Suggestions suggestions = Suggestions.empty();

      assertTrue(suggestions.isEmpty());
      assertTrue(suggestions.getList().isEmpty());
    }

    @Test
    @DisplayName("should merge multiple suggestions")
    void shouldMerge() {
      Suggestions s1 = new Suggestions(StringRange.at(0),
          List.of(new Suggestion(StringRange.at(0), "apple")));
      Suggestions s2 = new Suggestions(StringRange.at(0),
          List.of(new Suggestion(StringRange.at(0), "banana")));
      Suggestions s3 = new Suggestions(StringRange.at(0),
          List.of(new Suggestion(StringRange.at(0), "cherry")));

      Suggestions merged = Suggestions.merge("", Arrays.asList(s1, s2, s3));

      assertEquals(3, merged.getList().size());
    }

    @Test
    @DisplayName("should deduplicate suggestions when merging")
    void shouldDeduplicateWhenMerging() {
      Suggestions s1 = new Suggestions(StringRange.at(0),
          List.of(new Suggestion(StringRange.at(0), "test")));
      Suggestions s2 = new Suggestions(StringRange.at(0),
          List.of(new Suggestion(StringRange.at(0), "test")));

      Suggestions merged = Suggestions.merge("", Arrays.asList(s1, s2));

      assertEquals(1, merged.getList().size());
    }

    @Test
    @DisplayName("should sort suggestions when merging")
    void shouldSortWhenMerging() {
      Suggestions s1 = new Suggestions(StringRange.at(0),
          List.of(new Suggestion(StringRange.at(0), "zebra")));
      Suggestions s2 = new Suggestions(StringRange.at(0),
          List.of(new Suggestion(StringRange.at(0), "apple")));
      Suggestions s3 = new Suggestions(StringRange.at(0),
          List.of(new Suggestion(StringRange.at(0), "mango")));

      Suggestions merged = Suggestions.merge("", Arrays.asList(s1, s2, s3));

      assertEquals("apple", merged.getList().get(0).getText());
      assertEquals("mango", merged.getList().get(1).getText());
      assertEquals("zebra", merged.getList().get(2).getText());
    }
  }

  @Nested
  @DisplayName("SuggestionsBuilder")
  class SuggestionsBuilderTests {

    @Test
    @DisplayName("should build suggestions")
    void shouldBuildSuggestions() {
      SuggestionsBuilder builder = new SuggestionsBuilder("te", 0);
      builder.suggest("test");
      builder.suggest("temp");

      Suggestions suggestions = builder.build();

      assertEquals(2, suggestions.getList().size());
    }

    @Test
    @DisplayName("should filter by remaining prefix")
    void shouldFilterByPrefix() {
      SuggestionsBuilder builder = new SuggestionsBuilder("te", 0);
      builder.suggest("test");
      builder.suggest("temp");
      builder.suggest("hello"); // Should be filtered out

      Suggestions suggestions = builder.build();

      assertEquals(2, suggestions.getList().size());
      assertTrue(suggestions.getList().stream()
          .allMatch(s -> s.getText().startsWith("te")));
    }

    @Test
    @DisplayName("should be case-insensitive")
    void shouldBeCaseInsensitive() {
      SuggestionsBuilder builder = new SuggestionsBuilder("TE", 0);
      builder.suggest("test");
      builder.suggest("TEMP");

      Suggestions suggestions = builder.build();

      assertEquals(2, suggestions.getList().size());
    }

    @Test
    @DisplayName("should add tooltip to suggestion")
    void shouldAddTooltip() {
      SuggestionsBuilder builder = new SuggestionsBuilder("t", 0);
      builder.suggest("test", "A test suggestion");

      Suggestions suggestions = builder.build();

      assertEquals("A test suggestion", suggestions.getList().get(0).getTooltip());
    }

    @Test
    @DisplayName("should suggest integers")
    void shouldSuggestIntegers() {
      SuggestionsBuilder builder = new SuggestionsBuilder("", 0);
      builder.suggest(42);

      Suggestions suggestions = builder.build();

      assertEquals("42", suggestions.getList().get(0).getText());
    }

    @Test
    @DisplayName("should build as CompletableFuture")
    void shouldBuildAsFuture() {
      SuggestionsBuilder builder = new SuggestionsBuilder("t", 0);
      builder.suggest("test");

      Suggestions suggestions = builder.buildFuture().join();

      assertFalse(suggestions.isEmpty());
    }

    @Test
    @DisplayName("should restart at new position")
    void shouldRestartAtNewPosition() {
      SuggestionsBuilder original = new SuggestionsBuilder("hello world", 0);
      SuggestionsBuilder restarted = original.restart(6);

      assertEquals("hello world", restarted.getInput());
      assertEquals(6, restarted.getStart());
      assertEquals("world", restarted.getRemaining());
    }
  }

  @Nested
  @DisplayName("SuggestionProvider")
  class SuggestionProviderTests {

    @Test
    @DisplayName("should provide empty suggestions")
    void shouldProvideEmpty() {
      SuggestionProvider provider = SuggestionProvider.empty();
      SuggestionsBuilder builder = new SuggestionsBuilder("", 0);

      Suggestions suggestions = provider.getSuggestions(null, builder).join();

      assertTrue(suggestions.isEmpty());
    }

    @Test
    @DisplayName("should provide static suggestions")
    void shouldProvideStatic() {
      SuggestionProvider provider = SuggestionProvider.of("a", "b", "c");
      SuggestionsBuilder builder = new SuggestionsBuilder("", 0);

      Suggestions suggestions = provider.getSuggestions(null, builder).join();

      assertEquals(3, suggestions.getList().size());
    }

    @Test
    @DisplayName("should provide from iterable")
    void shouldProvideFromIterable() {
      List<String> items = List.of("one", "two", "three");
      SuggestionProvider provider = SuggestionProvider.of(items);
      SuggestionsBuilder builder = new SuggestionsBuilder("t", 0);

      Suggestions suggestions = provider.getSuggestions(null, builder).join();

      assertEquals(2, suggestions.getList().size());
      assertTrue(suggestions.getList().stream()
          .anyMatch(s -> s.getText().equals("two")));
      assertTrue(suggestions.getList().stream()
          .anyMatch(s -> s.getText().equals("three")));
    }
  }
}

