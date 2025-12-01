package dev.polv.taleapi.permission;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PermissionNode")
class PermissionNodeTest {

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethodTests {

        @Test
        @DisplayName("allow(key) creates ALLOW node")
        void allowCreatesAllowNode() {
            PermissionNode node = PermissionNode.allow("test.permission");
            
            assertEquals("test.permission", node.getKey());
            assertEquals(Tristate.ALLOW, node.getState());
            assertNull(node.getPayload());
            assertTrue(node.getContext().isEmpty());
        }

        @Test
        @DisplayName("deny(key) creates DENY node")
        void denyCreatesDenyNode() {
            PermissionNode node = PermissionNode.deny("test.permission");
            
            assertEquals("test.permission", node.getKey());
            assertEquals(Tristate.DENY, node.getState());
            assertNull(node.getPayload());
        }

        @Test
        @DisplayName("allow(key, payload) creates ALLOW node with payload")
        void allowWithPayloadCreatesNodeWithPayload() {
            PermissionNode node = PermissionNode.allow("plots.limit", 5);
            
            assertEquals("plots.limit", node.getKey());
            assertEquals(Tristate.ALLOW, node.getState());
            assertEquals(5, node.getPayload());
            assertTrue(node.hasPayload());
        }

        @Test
        @DisplayName("deny(key, payload) creates DENY node with payload")
        void denyWithPayloadCreatesNodeWithPayload() {
            PermissionNode node = PermissionNode.deny("cmd.teleport", "banned");
            
            assertEquals(Tristate.DENY, node.getState());
            assertEquals("banned", node.getPayload());
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("builder creates node with all properties")
        void builderCreatesNodeWithAllProperties() {
            ContextSet context = ContextSet.of(ContextKey.WORLD, "nether");
            
            PermissionNode node = PermissionNode.builder("test.permission")
                .allow()
                .payload(42)
                .context(context)
                .build();
            
            assertEquals("test.permission", node.getKey());
            assertEquals(Tristate.ALLOW, node.getState());
            assertEquals(42, node.getPayload());
            assertEquals(context, node.getContext());
        }

        @Test
        @DisplayName("builder defaults to UNDEFINED state")
        void builderDefaultsToUndefined() {
            PermissionNode node = PermissionNode.builder("test").build();
            assertEquals(Tristate.UNDEFINED, node.getState());
        }

        @Test
        @DisplayName("builder allow() sets state to ALLOW")
        void builderAllowSetsStateToAllow() {
            PermissionNode node = PermissionNode.builder("test").allow().build();
            assertEquals(Tristate.ALLOW, node.getState());
        }

        @Test
        @DisplayName("builder deny() sets state to DENY")
        void builderDenySetsStateToDeny() {
            PermissionNode node = PermissionNode.builder("test").deny().build();
            assertEquals(Tristate.DENY, node.getState());
        }

        @Test
        @DisplayName("builder state() sets explicit state")
        void builderStateSetsExplicitState() {
            PermissionNode node = PermissionNode.builder("test")
                .state(Tristate.DENY)
                .build();
            assertEquals(Tristate.DENY, node.getState());
        }
    }

    @Nested
    @DisplayName("Context matching")
    class ContextMatchingTests {

        @Test
        @DisplayName("node without context applies everywhere")
        void nodeWithoutContextAppliesEverywhere() {
            PermissionNode node = PermissionNode.allow("test");
            
            assertTrue(node.appliesInContext(ContextSet.EMPTY));
            assertTrue(node.appliesInContext(ContextSet.of(ContextKey.WORLD, "nether")));
        }

        @Test
        @DisplayName("node with context applies only when context matches")
        void nodeWithContextAppliesWhenMatches() {
            PermissionNode node = PermissionNode.builder("test")
                .allow()
                .context(ContextSet.of(ContextKey.WORLD, "nether"))
                .build();
            
            assertTrue(node.appliesInContext(ContextSet.of(ContextKey.WORLD, "nether")));
            assertFalse(node.appliesInContext(ContextSet.of(ContextKey.WORLD, "overworld")));
            assertFalse(node.appliesInContext(ContextSet.EMPTY));
        }

        @Test
        @DisplayName("node applies when current context has more keys")
        void nodeAppliesWhenCurrentHasMoreKeys() {
            PermissionNode node = PermissionNode.builder("test")
                .allow()
                .context(ContextSet.of(ContextKey.WORLD, "nether"))
                .build();
            
            ContextSet current = ContextSet.builder()
                .add(ContextKey.WORLD, "nether")
                .add(ContextKey.GAMEMODE, "survival")
                .build();
            
            assertTrue(node.appliesInContext(current));
        }
    }

    @Nested
    @DisplayName("toResult()")
    class ToResultTests {

        @Test
        @DisplayName("converts to PermissionResult with state and payload")
        void convertsToPermissionResult() {
            PermissionNode node = PermissionNode.allow("test", 42);
            PermissionResult result = node.toResult();
            
            assertEquals(Tristate.ALLOW, result.getState());
            assertEquals(42, result.asInt(0));
        }

        @Test
        @DisplayName("converts to PermissionResult without payload")
        void convertsToPermissionResultWithoutPayload() {
            PermissionNode node = PermissionNode.deny("test");
            PermissionResult result = node.toResult();
            
            assertEquals(Tristate.DENY, result.getState());
            assertFalse(result.hasPayload());
        }
    }

    @Nested
    @DisplayName("Immutable copies")
    class ImmutableCopyTests {

        @Test
        @DisplayName("withState creates copy with new state")
        void withStateCreatesNewCopy() {
            PermissionNode original = PermissionNode.allow("test", 42);
            PermissionNode modified = original.withState(Tristate.DENY);
            
            assertEquals(Tristate.ALLOW, original.getState());
            assertEquals(Tristate.DENY, modified.getState());
            assertEquals(42, modified.getPayload());
        }

        @Test
        @DisplayName("withPayload creates copy with new payload")
        void withPayloadCreatesNewCopy() {
            PermissionNode original = PermissionNode.allow("test", 42);
            PermissionNode modified = original.withPayload(99);
            
            assertEquals(42, original.getPayload());
            assertEquals(99, modified.getPayload());
            assertEquals(Tristate.ALLOW, modified.getState());
        }

        @Test
        @DisplayName("withContext creates copy with new context")
        void withContextCreatesNewCopy() {
            PermissionNode original = PermissionNode.allow("test");
            ContextSet newContext = ContextSet.of(ContextKey.WORLD, "nether");
            PermissionNode modified = original.withContext(newContext);
            
            assertTrue(original.getContext().isEmpty());
            assertEquals(newContext, modified.getContext());
        }
    }

    @Nested
    @DisplayName("equals() and hashCode()")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("equal nodes are equal")
        void equalNodesAreEqual() {
            PermissionNode a = PermissionNode.allow("test", 42);
            PermissionNode b = PermissionNode.allow("test", 42);
            
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("different keys are not equal")
        void differentKeysNotEqual() {
            PermissionNode a = PermissionNode.allow("test.a");
            PermissionNode b = PermissionNode.allow("test.b");
            
            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("different states are not equal")
        void differentStatesNotEqual() {
            PermissionNode a = PermissionNode.allow("test");
            PermissionNode b = PermissionNode.deny("test");
            
            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("different payloads are not equal")
        void differentPayloadsNotEqual() {
            PermissionNode a = PermissionNode.allow("test", 1);
            PermissionNode b = PermissionNode.allow("test", 2);
            
            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("different contexts are not equal")
        void differentContextsNotEqual() {
            PermissionNode a = PermissionNode.builder("test")
                .allow()
                .context(ContextSet.of(ContextKey.WORLD, "nether"))
                .build();
            PermissionNode b = PermissionNode.builder("test")
                .allow()
                .context(ContextSet.of(ContextKey.WORLD, "overworld"))
                .build();
            
            assertNotEquals(a, b);
        }
    }

    @Test
    @DisplayName("toString() includes all properties")
    void toStringIncludesAllProperties() {
        PermissionNode node = PermissionNode.builder("test")
            .allow()
            .payload(42)
            .context(ContextSet.of(ContextKey.WORLD, "nether"))
            .build();
        
        String str = node.toString();
        assertTrue(str.contains("test"));
        assertTrue(str.contains("ALLOW"));
        assertTrue(str.contains("42"));
        assertTrue(str.contains("nether"));
    }
}

