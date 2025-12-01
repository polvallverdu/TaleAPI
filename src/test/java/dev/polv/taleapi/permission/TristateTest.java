package dev.polv.taleapi.permission;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tristate")
class TristateTest {

    @Nested
    @DisplayName("asBoolean()")
    class AsBooleanTests {

        @Test
        @DisplayName("ALLOW returns true")
        void allowReturnsTrue() {
            assertTrue(Tristate.ALLOW.asBoolean());
        }

        @Test
        @DisplayName("DENY returns false")
        void denyReturnsFalse() {
            assertFalse(Tristate.DENY.asBoolean());
        }

        @Test
        @DisplayName("UNDEFINED returns false")
        void undefinedReturnsFalse() {
            assertFalse(Tristate.UNDEFINED.asBoolean());
        }
    }

    @Nested
    @DisplayName("asBoolean(default)")
    class AsBooleanWithDefaultTests {

        @Test
        @DisplayName("ALLOW ignores default and returns true")
        void allowIgnoresDefault() {
            assertTrue(Tristate.ALLOW.asBoolean(false));
            assertTrue(Tristate.ALLOW.asBoolean(true));
        }

        @Test
        @DisplayName("DENY ignores default and returns false")
        void denyIgnoresDefault() {
            assertFalse(Tristate.DENY.asBoolean(false));
            assertFalse(Tristate.DENY.asBoolean(true));
        }

        @Test
        @DisplayName("UNDEFINED returns the default value")
        void undefinedReturnsDefault() {
            assertTrue(Tristate.UNDEFINED.asBoolean(true));
            assertFalse(Tristate.UNDEFINED.asBoolean(false));
        }
    }

    @Nested
    @DisplayName("fromBoolean()")
    class FromBooleanTests {

        @Test
        @DisplayName("true converts to ALLOW")
        void trueConvertsToAllow() {
            assertEquals(Tristate.ALLOW, Tristate.fromBoolean(true));
        }

        @Test
        @DisplayName("false converts to DENY")
        void falseConvertsToDeny() {
            assertEquals(Tristate.DENY, Tristate.fromBoolean(false));
        }
    }

    @Nested
    @DisplayName("fromNullableBoolean()")
    class FromNullableBooleanTests {

        @Test
        @DisplayName("Boolean.TRUE converts to ALLOW")
        void trueConvertsToAllow() {
            assertEquals(Tristate.ALLOW, Tristate.fromNullableBoolean(Boolean.TRUE));
        }

        @Test
        @DisplayName("Boolean.FALSE converts to DENY")
        void falseConvertsToDeny() {
            assertEquals(Tristate.DENY, Tristate.fromNullableBoolean(Boolean.FALSE));
        }

        @Test
        @DisplayName("null converts to UNDEFINED")
        void nullConvertsToUndefined() {
            assertEquals(Tristate.UNDEFINED, Tristate.fromNullableBoolean(null));
        }
    }

    @Nested
    @DisplayName("isDefined()")
    class IsDefinedTests {

        @Test
        @DisplayName("ALLOW is defined")
        void allowIsDefined() {
            assertTrue(Tristate.ALLOW.isDefined());
        }

        @Test
        @DisplayName("DENY is defined")
        void denyIsDefined() {
            assertTrue(Tristate.DENY.isDefined());
        }

        @Test
        @DisplayName("UNDEFINED is not defined")
        void undefinedIsNotDefined() {
            assertFalse(Tristate.UNDEFINED.isDefined());
        }
    }
}

