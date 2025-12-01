package dev.polv.taleapi.permission;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PermissionTree")
class PermissionTreeTest {

    private PermissionTree tree;

    @BeforeEach
    void setUp() {
        tree = new PermissionTree();
    }

    @Nested
    @DisplayName("Basic operations")
    class BasicOperationsTests {

        @Test
        @DisplayName("new tree is empty")
        void newTreeIsEmpty() {
            assertTrue(tree.isEmpty());
            assertEquals(0, tree.size());
        }

        @Test
        @DisplayName("set() adds permission")
        void setAddsPermission() {
            tree.set(PermissionNode.allow("test.permission"));
            
            assertFalse(tree.isEmpty());
            assertEquals(1, tree.size());
        }

        @Test
        @DisplayName("allow() convenience method adds ALLOW permission")
        void allowConvenienceMethodAddsPermission() {
            tree.allow("test.permission");
            
            assertTrue(tree.query("test.permission").isAllowed());
        }

        @Test
        @DisplayName("deny() convenience method adds DENY permission")
        void denyConvenienceMethodAddsPermission() {
            tree.deny("test.permission");
            
            assertTrue(tree.query("test.permission").isDenied());
        }

        @Test
        @DisplayName("remove() removes permission")
        void removeRemovesPermission() {
            tree.allow("test.permission");
            assertTrue(tree.remove("test.permission"));
            
            assertTrue(tree.query("test.permission").isUndefined());
        }

        @Test
        @DisplayName("remove() returns false for non-existent key")
        void removeReturnsFalseForNonExistent() {
            assertFalse(tree.remove("nonexistent"));
        }

        @Test
        @DisplayName("clear() removes all permissions")
        void clearRemovesAllPermissions() {
            tree.allow("a");
            tree.allow("b");
            tree.allow("c");
            
            tree.clear();
            
            assertTrue(tree.isEmpty());
        }
    }

    @Nested
    @DisplayName("Query operations")
    class QueryOperationsTests {

        @Test
        @DisplayName("query returns UNDEFINED for non-existent permission")
        void queryReturnsUndefinedForNonExistent() {
            PermissionResult result = tree.query("nonexistent");
            assertTrue(result.isUndefined());
        }

        @Test
        @DisplayName("query returns correct state for set permission")
        void queryReturnsCorrectState() {
            tree.set(PermissionNode.allow("test.permission"));
            
            assertTrue(tree.query("test.permission").isAllowed());
        }

        @Test
        @DisplayName("query returns payload from permission node")
        void queryReturnsPayload() {
            tree.set(PermissionNode.allow("plots.limit", 5));
            
            PermissionResult result = tree.query("plots.limit");
            assertEquals(5, result.asInt(0));
        }

        @Test
        @DisplayName("has() returns true for ALLOW permissions")
        void hasReturnsTrueForAllow() {
            tree.allow("test.permission");
            
            assertTrue(tree.has("test.permission"));
        }

        @Test
        @DisplayName("has() returns false for DENY permissions")
        void hasReturnsFalseForDeny() {
            tree.deny("test.permission");
            
            assertFalse(tree.has("test.permission"));
        }

        @Test
        @DisplayName("has() returns false for non-existent permissions")
        void hasReturnsFalseForNonExistent() {
            assertFalse(tree.has("nonexistent"));
        }
    }

    @Nested
    @DisplayName("Wildcard support")
    class WildcardTests {

        @Test
        @DisplayName("root wildcard (*) matches everything")
        void rootWildcardMatchesEverything() {
            tree.allow("*");
            
            assertTrue(tree.has("anything"));
            assertTrue(tree.has("deeply.nested.permission"));
            assertTrue(tree.has("cmd.teleport"));
        }

        @Test
        @DisplayName("nested wildcard (cmd.*) matches all children")
        void nestedWildcardMatchesChildren() {
            tree.allow("cmd.*");
            
            assertTrue(tree.has("cmd.teleport"));
            assertTrue(tree.has("cmd.give"));
            assertTrue(tree.has("cmd.anything.here"));
            assertFalse(tree.has("other.permission"));
        }

        @Test
        @DisplayName("specific permission takes precedence over wildcard")
        void specificTakesPrecedenceOverWildcard() {
            tree.allow("cmd.*");
            tree.deny("cmd.ban");
            
            assertTrue(tree.has("cmd.teleport"));
            assertFalse(tree.has("cmd.ban"));
        }

        @Test
        @DisplayName("wildcard at any level works correctly")
        void wildcardAtAnyLevel() {
            tree.allow("server.admin.*");
            
            assertTrue(tree.has("server.admin.kick"));
            assertTrue(tree.has("server.admin.ban"));
            assertFalse(tree.has("server.user.chat"));
        }

        @Test
        @DisplayName("multiple wildcards at different levels")
        void multipleWildcardsAtDifferentLevels() {
            tree.allow("*"); // Global allow
            tree.deny("cmd.*"); // Deny all commands
            tree.allow("cmd.help"); // But allow help
            
            assertTrue(tree.has("anything")); // Global allow
            assertFalse(tree.has("cmd.ban")); // cmd.* deny
            assertTrue(tree.has("cmd.help")); // Specific allow
        }

        @Test
        @DisplayName("wildcard lookup is O(k) - doesn't traverse all nodes")
        void wildcardLookupIsEfficient() {
            // Add many permissions
            for (int i = 0; i < 1000; i++) {
                tree.allow("test.permission." + i);
            }
            tree.allow("cmd.*");
            
            // This should be instant due to wildcard matching
            long start = System.nanoTime();
            boolean result = tree.has("cmd.very.deep.nested.permission");
            long elapsed = System.nanoTime() - start;
            
            assertTrue(result);
            assertTrue(elapsed < 1_000_000); // Should complete in < 1ms
        }
    }

    @Nested
    @DisplayName("Context-aware queries")
    class ContextAwareTests {

        @Test
        @DisplayName("query with context matches contextual permission")
        void queryWithContextMatches() {
            tree.set(PermissionNode.builder("ability.fly")
                .allow()
                .context(ContextSet.of(ContextKey.WORLD, "creative"))
                .build());
            
            ContextSet creativeContext = ContextSet.of(ContextKey.WORLD, "creative");
            ContextSet survivalContext = ContextSet.of(ContextKey.WORLD, "survival");
            
            assertTrue(tree.has("ability.fly", creativeContext));
            assertFalse(tree.has("ability.fly", survivalContext));
        }

        @Test
        @DisplayName("global permission applies when no context-specific exists")
        void globalPermissionAppliesWithoutContext() {
            tree.allow("ability.jump");
            
            assertTrue(tree.has("ability.jump", ContextSet.of(ContextKey.WORLD, "any")));
        }

        @Test
        @DisplayName("context-specific permission takes precedence")
        void contextSpecificTakesPrecedence() {
            // Global allow
            tree.set(PermissionNode.allow("ability.fly"));
            // Context-specific deny
            tree.set(PermissionNode.builder("ability.fly")
                .deny()
                .context(ContextSet.of(ContextKey.WORLD, "survival"))
                .build());
            
            ContextSet survivalContext = ContextSet.of(ContextKey.WORLD, "survival");
            ContextSet creativeContext = ContextSet.of(ContextKey.WORLD, "creative");
            
            // In survival, the contextual DENY should apply
            assertFalse(tree.has("ability.fly", survivalContext));
            // In creative, the global ALLOW applies
            assertTrue(tree.has("ability.fly", creativeContext));
        }

        @Test
        @DisplayName("has() with context delegates correctly")
        void hasWithContextDelegates() {
            tree.set(PermissionNode.builder("test")
                .allow()
                .context(ContextSet.of(ContextKey.WORLD, "nether"))
                .build());
            
            assertTrue(tree.has("test", ContextSet.of(ContextKey.WORLD, "nether")));
            assertFalse(tree.has("test", ContextSet.of(ContextKey.WORLD, "overworld")));
        }
    }

    @Nested
    @DisplayName("getAllNodes()")
    class GetAllNodesTests {

        @Test
        @DisplayName("returns empty list for empty tree")
        void returnsEmptyForEmptyTree() {
            assertTrue(tree.getAllNodes().isEmpty());
        }

        @Test
        @DisplayName("returns all added nodes")
        void returnsAllNodes() {
            tree.allow("a");
            tree.allow("b.c");
            tree.deny("d.e.f");
            
            List<PermissionNode> nodes = tree.getAllNodes();
            assertEquals(3, nodes.size());
        }

        @Test
        @DisplayName("returned list is unmodifiable")
        void returnedListIsUnmodifiable() {
            tree.allow("test");
            
            List<PermissionNode> nodes = tree.getAllNodes();
            assertThrows(UnsupportedOperationException.class, () -> nodes.add(PermissionNode.allow("new")));
        }
    }

    @Nested
    @DisplayName("getAllKeys()")
    class GetAllKeysTests {

        @Test
        @DisplayName("returns all permission keys")
        void returnsAllKeys() {
            tree.allow("cmd.teleport");
            tree.allow("cmd.give");
            tree.deny("server.admin");
            
            List<String> keys = tree.getAllKeys();
            assertEquals(3, keys.size());
            assertTrue(keys.contains("cmd.teleport"));
            assertTrue(keys.contains("cmd.give"));
            assertTrue(keys.contains("server.admin"));
        }
    }

    @Nested
    @DisplayName("merge()")
    class MergeTests {

        @Test
        @DisplayName("merges another tree's nodes")
        void mergesAnotherTree() {
            tree.allow("a");
            
            PermissionTree other = new PermissionTree();
            other.allow("b");
            other.allow("c");
            
            tree.merge(other);
            
            assertEquals(3, tree.size());
            assertTrue(tree.has("a"));
            assertTrue(tree.has("b"));
            assertTrue(tree.has("c"));
        }

        @Test
        @DisplayName("merged nodes are added without replacing")
        void mergedNodesAddWithoutReplacing() {
            tree.set(PermissionNode.allow("test", 1));
            
            PermissionTree other = new PermissionTree();
            other.set(PermissionNode.allow("test", 2));
            
            tree.merge(other);
            
            // Both nodes exist (multiple nodes per key supported)
            assertEquals(2, tree.size());
        }
    }

    @Nested
    @DisplayName("flatten()")
    class FlattenTests {

        @Test
        @DisplayName("flattens multiple trees in priority order")
        void flattensMultipleTrees() {
            PermissionTree defaults = new PermissionTree();
            defaults.allow("default.perm");
            
            PermissionTree group = new PermissionTree();
            group.allow("group.perm");
            
            PermissionTree personal = new PermissionTree();
            personal.allow("personal.perm");
            
            PermissionTree flattened = PermissionTree.flatten(defaults, group, personal);
            
            assertTrue(flattened.has("default.perm"));
            assertTrue(flattened.has("group.perm"));
            assertTrue(flattened.has("personal.perm"));
        }

        @Test
        @DisplayName("handles null trees gracefully")
        void handlesNullTrees() {
            PermissionTree tree1 = new PermissionTree();
            tree1.allow("test");
            
            PermissionTree flattened = PermissionTree.flatten(tree1, null);
            
            assertTrue(flattened.has("test"));
        }
    }

    @Nested
    @DisplayName("Constructor with collection")
    class ConstructorWithCollectionTests {

        @Test
        @DisplayName("initializes from collection of nodes")
        void initializesFromCollection() {
            List<PermissionNode> nodes = List.of(
                PermissionNode.allow("a"),
                PermissionNode.deny("b"),
                PermissionNode.allow("c", 42)
            );
            
            PermissionTree tree = new PermissionTree(nodes);
            
            assertEquals(3, tree.size());
            assertTrue(tree.has("a"));
            assertFalse(tree.has("b"));
            assertEquals(42, tree.query("c").asInt(0));
        }
    }

    @Nested
    @DisplayName("Performance characteristics")
    class PerformanceTests {

        @Test
        @DisplayName("lookup time depends on key length, not tree size")
        void lookupTimeIndependentOfTreeSize() {
            // Create a tree with many permissions
            for (int i = 0; i < 10000; i++) {
                tree.allow("permission.number." + i);
            }
            
            // Lookup a specific permission
            long start = System.nanoTime();
            for (int i = 0; i < 1000; i++) {
                tree.query("permission.number.5000");
            }
            long elapsed = System.nanoTime() - start;
            
            // Average should be very fast (< 0.1ms per lookup)
            long avgNanos = elapsed / 1000;
            assertTrue(avgNanos < 100_000, "Average lookup took " + avgNanos + "ns");
        }

        @Test
        @DisplayName("wildcard provides early termination")
        void wildcardEarlyTermination() {
            tree.allow("a.*");
            // Add many siblings
            for (int i = 0; i < 10000; i++) {
                tree.allow("a." + i);
            }
            
            // Wildcard query should still be fast
            long start = System.nanoTime();
            for (int i = 0; i < 1000; i++) {
                tree.query("a.very.deep.path.that.doesnt.exist.specifically");
            }
            long elapsed = System.nanoTime() - start;
            
            long avgNanos = elapsed / 1000;
            assertTrue(avgNanos < 100_000, "Average wildcard lookup took " + avgNanos + "ns");
        }
    }
}

