package dev.polv.taleapi.permission;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ContextSet")
class ContextSetTest {

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("builds empty context set")
        void buildsEmptyContextSet() {
            ContextSet context = ContextSet.builder().build();
            
            assertTrue(context.isEmpty());
            assertEquals(0, context.size());
        }

        @Test
        @DisplayName("builds context set with typed key")
        void buildsWithTypedKey() {
            ContextSet context = ContextSet.builder()
                .add(ContextKey.WORLD, "nether")
                .build();
            
            assertFalse(context.isEmpty());
            assertEquals(1, context.size());
            assertEquals("nether", context.get(ContextKey.WORLD).orElse(null));
        }

        @Test
        @DisplayName("builds context set with string key")
        void buildsWithStringKey() {
            ContextSet context = ContextSet.builder()
                .add("custom", "value")
                .build();
            
            assertTrue(context.keys().contains("custom"));
        }

        @Test
        @DisplayName("builds context set with multiple keys")
        void buildsWithMultipleKeys() {
            ContextSet context = ContextSet.builder()
                .add(ContextKey.WORLD, "overworld")
                .add(ContextKey.GAMEMODE, "survival")
                .add(ContextKey.SERVER, "lobby")
                .build();
            
            assertEquals(3, context.size());
            assertEquals("overworld", context.get(ContextKey.WORLD).orElse(null));
            assertEquals("survival", context.get(ContextKey.GAMEMODE).orElse(null));
            assertEquals("lobby", context.get(ContextKey.SERVER).orElse(null));
        }

        @Test
        @DisplayName("addAll copies from another ContextSet")
        void addAllCopiesFromAnother() {
            ContextSet original = ContextSet.builder()
                .add(ContextKey.WORLD, "nether")
                .build();
            
            ContextSet extended = ContextSet.builder()
                .addAll(original)
                .add(ContextKey.GAMEMODE, "creative")
                .build();
            
            assertEquals(2, extended.size());
            assertEquals("nether", extended.get(ContextKey.WORLD).orElse(null));
            assertEquals("creative", extended.get(ContextKey.GAMEMODE).orElse(null));
        }
    }

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethodTests {

        @Test
        @DisplayName("EMPTY is empty")
        void emptyIsEmpty() {
            assertTrue(ContextSet.EMPTY.isEmpty());
        }

        @Test
        @DisplayName("of(key, value) creates single-entry set")
        void ofCreatesSingleEntry() {
            ContextSet context = ContextSet.of(ContextKey.WORLD, "nether");
            
            assertEquals(1, context.size());
            assertEquals("nether", context.get(ContextKey.WORLD).orElse(null));
        }

        @Test
        @DisplayName("of(k1, v1, k2, v2) creates two-entry set")
        void ofCreatesTwoEntries() {
            ContextSet context = ContextSet.of(
                ContextKey.WORLD, "nether",
                ContextKey.GAMEMODE, "survival"
            );
            
            assertEquals(2, context.size());
            assertEquals("nether", context.get(ContextKey.WORLD).orElse(null));
            assertEquals("survival", context.get(ContextKey.GAMEMODE).orElse(null));
        }
    }

    @Nested
    @DisplayName("contains()")
    class ContainsTests {

        @Test
        @DisplayName("returns true for existing key")
        void returnsTrueForExistingKey() {
            ContextSet context = ContextSet.of(ContextKey.WORLD, "nether");
            
            assertTrue(context.contains(ContextKey.WORLD));
        }

        @Test
        @DisplayName("returns false for missing key")
        void returnsFalseForMissingKey() {
            ContextSet context = ContextSet.of(ContextKey.WORLD, "nether");
            
            assertFalse(context.contains(ContextKey.GAMEMODE));
        }

        @Test
        @DisplayName("returns true for matching key-value pair")
        void returnsTrueForMatchingPair() {
            ContextSet context = ContextSet.of(ContextKey.WORLD, "nether");
            
            assertTrue(context.contains(ContextKey.WORLD, "nether"));
        }

        @Test
        @DisplayName("returns false for non-matching value")
        void returnsFalseForNonMatchingValue() {
            ContextSet context = ContextSet.of(ContextKey.WORLD, "nether");
            
            assertFalse(context.contains(ContextKey.WORLD, "overworld"));
        }
    }

    @Nested
    @DisplayName("matches()")
    class MatchesTests {

        @Test
        @DisplayName("empty context matches everything")
        void emptyMatchesEverything() {
            ContextSet empty = ContextSet.EMPTY;
            ContextSet target = ContextSet.builder()
                .add(ContextKey.WORLD, "nether")
                .add(ContextKey.GAMEMODE, "survival")
                .build();
            
            assertTrue(empty.matches(target));
        }

        @Test
        @DisplayName("matches when all keys match")
        void matchesWhenAllKeysMatch() {
            ContextSet required = ContextSet.of(ContextKey.WORLD, "nether");
            ContextSet actual = ContextSet.builder()
                .add(ContextKey.WORLD, "nether")
                .add(ContextKey.GAMEMODE, "survival")
                .build();
            
            assertTrue(required.matches(actual));
        }

        @Test
        @DisplayName("doesn't match when value differs")
        void doesntMatchWhenValueDiffers() {
            ContextSet required = ContextSet.of(ContextKey.WORLD, "nether");
            ContextSet actual = ContextSet.of(ContextKey.WORLD, "overworld");
            
            assertFalse(required.matches(actual));
        }

        @Test
        @DisplayName("doesn't match when key is missing")
        void doesntMatchWhenKeyMissing() {
            ContextSet required = ContextSet.of(ContextKey.WORLD, "nether");
            ContextSet actual = ContextSet.of(ContextKey.GAMEMODE, "survival");
            
            assertFalse(required.matches(actual));
        }

        @Test
        @DisplayName("matches when multiple keys match")
        void matchesWhenMultipleKeysMatch() {
            ContextSet required = ContextSet.of(
                ContextKey.WORLD, "nether",
                ContextKey.GAMEMODE, "survival"
            );
            ContextSet actual = ContextSet.builder()
                .add(ContextKey.WORLD, "nether")
                .add(ContextKey.GAMEMODE, "survival")
                .add(ContextKey.SERVER, "main")
                .build();
            
            assertTrue(required.matches(actual));
        }
    }

    @Nested
    @DisplayName("equals() and hashCode()")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("equal contexts are equal")
        void equalContextsAreEqual() {
            ContextSet a = ContextSet.of(ContextKey.WORLD, "nether");
            ContextSet b = ContextSet.of(ContextKey.WORLD, "nether");
            
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("different contexts are not equal")
        void differentContextsNotEqual() {
            ContextSet a = ContextSet.of(ContextKey.WORLD, "nether");
            ContextSet b = ContextSet.of(ContextKey.WORLD, "overworld");
            
            assertNotEquals(a, b);
        }
    }
}

