package dev.polv.taleapi.permission;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PermissionResult")
class PermissionResultTest {

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethodTests {

        @Test
        @DisplayName("ALLOWED is pre-built")
        void allowedIsPreBuilt() {
            assertSame(PermissionResult.ALLOWED, PermissionResult.of(Tristate.ALLOW));
            assertTrue(PermissionResult.ALLOWED.isAllowed());
            assertFalse(PermissionResult.ALLOWED.hasPayload());
        }

        @Test
        @DisplayName("DENIED is pre-built")
        void deniedIsPreBuilt() {
            assertSame(PermissionResult.DENIED, PermissionResult.of(Tristate.DENY));
            assertTrue(PermissionResult.DENIED.isDenied());
            assertFalse(PermissionResult.DENIED.hasPayload());
        }

        @Test
        @DisplayName("UNDEFINED is pre-built")
        void undefinedIsPreBuilt() {
            assertSame(PermissionResult.UNDEFINED, PermissionResult.of(Tristate.UNDEFINED));
            assertTrue(PermissionResult.UNDEFINED.isUndefined());
            assertFalse(PermissionResult.UNDEFINED.hasPayload());
        }

        @Test
        @DisplayName("of(state, payload) creates result with payload")
        void ofWithPayloadCreatesResult() {
            PermissionResult result = PermissionResult.of(Tristate.ALLOW, 42);
            
            assertTrue(result.isAllowed());
            assertTrue(result.hasPayload());
            assertEquals(42, result.asInt(0));
        }

        @Test
        @DisplayName("allow(payload) creates ALLOW with payload")
        void allowWithPayload() {
            PermissionResult result = PermissionResult.allow(10);
            
            assertTrue(result.isAllowed());
            assertEquals(10, result.asInt(0));
        }

        @Test
        @DisplayName("deny(payload) creates DENY with payload")
        void denyWithPayload() {
            PermissionResult result = PermissionResult.deny("reason");
            
            assertTrue(result.isDenied());
            assertEquals("reason", result.asString());
        }
    }

    @Nested
    @DisplayName("State checks")
    class StateCheckTests {

        @Test
        @DisplayName("isAllowed() returns true only for ALLOW")
        void isAllowedOnlyForAllow() {
            assertTrue(PermissionResult.ALLOWED.isAllowed());
            assertFalse(PermissionResult.DENIED.isAllowed());
            assertFalse(PermissionResult.UNDEFINED.isAllowed());
        }

        @Test
        @DisplayName("isDenied() returns true only for DENY")
        void isDeniedOnlyForDeny() {
            assertFalse(PermissionResult.ALLOWED.isDenied());
            assertTrue(PermissionResult.DENIED.isDenied());
            assertFalse(PermissionResult.UNDEFINED.isDenied());
        }

        @Test
        @DisplayName("isUndefined() returns true only for UNDEFINED")
        void isUndefinedOnlyForUndefined() {
            assertFalse(PermissionResult.ALLOWED.isUndefined());
            assertFalse(PermissionResult.DENIED.isUndefined());
            assertTrue(PermissionResult.UNDEFINED.isUndefined());
        }

        @Test
        @DisplayName("getState() returns correct state")
        void getStateReturnsCorrectState() {
            assertEquals(Tristate.ALLOW, PermissionResult.ALLOWED.getState());
            assertEquals(Tristate.DENY, PermissionResult.DENIED.getState());
            assertEquals(Tristate.UNDEFINED, PermissionResult.UNDEFINED.getState());
        }
    }

    @Nested
    @DisplayName("asInt()")
    class AsIntTests {

        @Test
        @DisplayName("returns integer payload")
        void returnsIntegerPayload() {
            PermissionResult result = PermissionResult.allow(42);
            assertEquals(42, result.asInt(0));
        }

        @Test
        @DisplayName("converts Number types")
        void convertsNumberTypes() {
            assertEquals(42, PermissionResult.allow(42L).asInt(0));
            assertEquals(42, PermissionResult.allow(42.9).asInt(0));
        }

        @Test
        @DisplayName("parses string numbers")
        void parsesStringNumbers() {
            assertEquals(42, PermissionResult.allow("42").asInt(0));
        }

        @Test
        @DisplayName("returns default for invalid string")
        void returnsDefaultForInvalidString() {
            assertEquals(99, PermissionResult.allow("not a number").asInt(99));
        }

        @Test
        @DisplayName("returns default for no payload")
        void returnsDefaultForNoPayload() {
            assertEquals(99, PermissionResult.ALLOWED.asInt(99));
        }

        @Test
        @DisplayName("asOptionalInt returns OptionalInt")
        void asOptionalIntReturnsOptionalInt() {
            assertEquals(OptionalInt.of(42), PermissionResult.allow(42).asOptionalInt());
            assertEquals(OptionalInt.empty(), PermissionResult.ALLOWED.asOptionalInt());
        }
    }

    @Nested
    @DisplayName("asLong()")
    class AsLongTests {

        @Test
        @DisplayName("returns long payload")
        void returnsLongPayload() {
            PermissionResult result = PermissionResult.allow(Long.MAX_VALUE);
            assertEquals(Long.MAX_VALUE, result.asLong(0));
        }

        @Test
        @DisplayName("converts Number types")
        void convertsNumberTypes() {
            assertEquals(42L, PermissionResult.allow(42).asLong(0));
            assertEquals(42L, PermissionResult.allow(42.9).asLong(0));
        }

        @Test
        @DisplayName("parses string numbers")
        void parsesStringNumbers() {
            assertEquals(42L, PermissionResult.allow("42").asLong(0));
        }

        @Test
        @DisplayName("returns default for no payload")
        void returnsDefaultForNoPayload() {
            assertEquals(99L, PermissionResult.ALLOWED.asLong(99L));
        }

        @Test
        @DisplayName("asOptionalLong returns OptionalLong")
        void asOptionalLongReturnsOptionalLong() {
            assertEquals(OptionalLong.of(42), PermissionResult.allow(42).asOptionalLong());
            assertEquals(OptionalLong.empty(), PermissionResult.ALLOWED.asOptionalLong());
        }
    }

    @Nested
    @DisplayName("asDouble()")
    class AsDoubleTests {

        @Test
        @DisplayName("returns double payload")
        void returnsDoublePayload() {
            PermissionResult result = PermissionResult.allow(3.14);
            assertEquals(3.14, result.asDouble(0), 0.001);
        }

        @Test
        @DisplayName("converts Number types")
        void convertsNumberTypes() {
            assertEquals(42.0, PermissionResult.allow(42).asDouble(0), 0.001);
            assertEquals(42.0, PermissionResult.allow(42L).asDouble(0), 0.001);
        }

        @Test
        @DisplayName("parses string numbers")
        void parsesStringNumbers() {
            assertEquals(3.14, PermissionResult.allow("3.14").asDouble(0), 0.001);
        }

        @Test
        @DisplayName("returns default for no payload")
        void returnsDefaultForNoPayload() {
            assertEquals(99.9, PermissionResult.ALLOWED.asDouble(99.9), 0.001);
        }

        @Test
        @DisplayName("asOptionalDouble returns OptionalDouble")
        void asOptionalDoubleReturnsOptionalDouble() {
            OptionalDouble result = PermissionResult.allow(3.14).asOptionalDouble();
            assertTrue(result.isPresent());
            assertEquals(3.14, result.getAsDouble(), 0.001);
            assertEquals(OptionalDouble.empty(), PermissionResult.ALLOWED.asOptionalDouble());
        }
    }

    @Nested
    @DisplayName("asString()")
    class AsStringTests {

        @Test
        @DisplayName("returns string payload")
        void returnsStringPayload() {
            PermissionResult result = PermissionResult.allow("hello");
            assertEquals("hello", result.asString());
        }

        @Test
        @DisplayName("converts other types to string")
        void convertsOtherTypesToString() {
            assertEquals("42", PermissionResult.allow(42).asString());
            assertEquals("true", PermissionResult.allow(true).asString());
        }

        @Test
        @DisplayName("returns null for no payload")
        void returnsNullForNoPayload() {
            assertNull(PermissionResult.ALLOWED.asString());
        }

        @Test
        @DisplayName("returns default for no payload")
        void returnsDefaultForNoPayload() {
            assertEquals("default", PermissionResult.ALLOWED.asString("default"));
        }
    }

    @Nested
    @DisplayName("asBoolean()")
    class AsBooleanTests {

        @Test
        @DisplayName("returns boolean payload")
        void returnsBooleanPayload() {
            assertTrue(PermissionResult.allow(true).asBoolean(false));
            assertFalse(PermissionResult.allow(false).asBoolean(true));
        }

        @Test
        @DisplayName("parses string boolean")
        void parsesStringBoolean() {
            assertTrue(PermissionResult.allow("true").asBoolean(false));
            assertFalse(PermissionResult.allow("false").asBoolean(true));
        }

        @Test
        @DisplayName("returns default for no payload")
        void returnsDefaultForNoPayload() {
            assertTrue(PermissionResult.ALLOWED.asBoolean(true));
            assertFalse(PermissionResult.ALLOWED.asBoolean(false));
        }
    }

    @Nested
    @DisplayName("as(Class)")
    class AsClassTests {

        @Test
        @DisplayName("returns typed value when correct type")
        void returnsTypedValueWhenCorrectType() {
            PermissionResult result = PermissionResult.allow("hello");
            Optional<String> value = result.as(String.class);
            
            assertTrue(value.isPresent());
            assertEquals("hello", value.get());
        }

        @Test
        @DisplayName("returns empty when wrong type")
        void returnsEmptyWhenWrongType() {
            PermissionResult result = PermissionResult.allow("hello");
            Optional<Integer> value = result.as(Integer.class);
            
            assertTrue(value.isEmpty());
        }

        @Test
        @DisplayName("returns empty when no payload")
        void returnsEmptyWhenNoPayload() {
            Optional<String> value = PermissionResult.ALLOWED.as(String.class);
            assertTrue(value.isEmpty());
        }
    }

    @Nested
    @DisplayName("equals() and hashCode()")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("equal results are equal")
        void equalResultsAreEqual() {
            PermissionResult a = PermissionResult.of(Tristate.ALLOW, 42);
            PermissionResult b = PermissionResult.of(Tristate.ALLOW, 42);
            
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("different states are not equal")
        void differentStatesNotEqual() {
            PermissionResult a = PermissionResult.of(Tristate.ALLOW, 42);
            PermissionResult b = PermissionResult.of(Tristate.DENY, 42);
            
            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("different payloads are not equal")
        void differentPayloadsNotEqual() {
            PermissionResult a = PermissionResult.of(Tristate.ALLOW, 42);
            PermissionResult b = PermissionResult.of(Tristate.ALLOW, 99);
            
            assertNotEquals(a, b);
        }
    }
}

