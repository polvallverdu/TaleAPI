package dev.polv.taleapi.permission;

import dev.polv.taleapi.entity.TalePlayer;
import dev.polv.taleapi.event.player.PermissionCheckCallback;
import dev.polv.taleapi.testutil.TestPlayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PermissionService")
class PermissionServiceTest {

    private PermissionService service;
    private TestPermissionProvider provider;
    private TestPlayer player;

    @BeforeEach
    void setUp() {
        service = PermissionService.getInstance();
        provider = new TestPermissionProvider();
        player = new TestPlayer("TestPlayer");
        
        service.setProvider(provider);
    }

    @AfterEach
    void tearDown() {
        service.shutdown();
        PermissionCheckCallback.EVENT.clearListeners();
    }

    @Nested
    @DisplayName("Provider management")
    class ProviderManagementTests {

        @Test
        @DisplayName("setProvider registers and enables provider")
        void setProviderRegistersAndEnables() {
            TestPermissionProvider newProvider = new TestPermissionProvider();
            
            service.setProvider(newProvider);
            
            assertTrue(newProvider.enabled);
            assertEquals(newProvider, service.getProvider());
        }

        @Test
        @DisplayName("setProvider disables old provider")
        void setProviderDisablesOldProvider() {
            TestPermissionProvider oldProvider = provider;
            TestPermissionProvider newProvider = new TestPermissionProvider();
            
            service.setProvider(newProvider);
            
            assertTrue(oldProvider.disabled);
        }

        @Test
        @DisplayName("hasProvider returns true when provider set")
        void hasProviderReturnsTrueWhenSet() {
            assertTrue(service.hasProvider());
        }

        @Test
        @DisplayName("shutdown disables provider")
        void shutdownDisablesProvider() {
            service.shutdown();
            
            assertTrue(provider.disabled);
            assertNull(service.getProvider());
            assertFalse(service.hasProvider());
        }
    }

    @Nested
    @DisplayName("Query operations")
    class QueryOperationsTests {

        @Test
        @DisplayName("query delegates to provider")
        void queryDelegatesToProvider() {
            provider.setResult("test.permission", PermissionResult.ALLOWED);
            
            PermissionResult result = service.query(player, "test.permission");
            
            assertTrue(result.isAllowed());
        }

        @Test
        @DisplayName("query with context delegates to provider")
        void queryWithContextDelegatesToProvider() {
            ContextSet context = ContextSet.of(ContextKey.WORLD, "nether");
            provider.setResult("test.permission", context, PermissionResult.ALLOWED);
            
            PermissionResult result = service.query(player, "test.permission", context);
            
            assertTrue(result.isAllowed());
        }

        @Test
        @DisplayName("has() is convenience for query().isAllowed()")
        void hasIsConvenienceMethod() {
            provider.setResult("test.allow", PermissionResult.ALLOWED);
            provider.setResult("test.deny", PermissionResult.DENIED);
            
            assertTrue(service.has(player, "test.allow"));
            assertFalse(service.has(player, "test.deny"));
        }

        @Test
        @DisplayName("throws when no provider registered")
        void throwsWhenNoProvider() {
            service.shutdown();
            
            assertThrows(IllegalStateException.class, () -> service.query(player, "test"));
        }
    }

    @Nested
    @DisplayName("Event hook")
    class EventHookTests {

        @Test
        @DisplayName("PermissionCheckCallback can override result")
        void eventCanOverrideResult() {
            provider.setResult("test.permission", PermissionResult.ALLOWED);
            
            // Register a hook that denies this specific permission
            PermissionCheckCallback.EVENT.register((p, key, ctx, result) -> {
                if (key.equals("test.permission")) {
                    return PermissionCheckCallback.CheckResult.deny();
                }
                return PermissionCheckCallback.CheckResult.unmodified();
            });
            
            PermissionResult result = service.query(player, "test.permission");
            
            // The hook should have overridden to DENY
            assertTrue(result.isDenied());
        }

        @Test
        @DisplayName("unmodified result passes through")
        void unmodifiedResultPassesThrough() {
            provider.setResult("test.permission", PermissionResult.ALLOWED);
            
            // Register a hook that doesn't modify
            PermissionCheckCallback.EVENT.register((p, key, ctx, result) -> 
                PermissionCheckCallback.CheckResult.unmodified()
            );
            
            PermissionResult result = service.query(player, "test.permission");
            
            assertTrue(result.isAllowed());
        }

        @Test
        @DisplayName("hook can add payload to result")
        void hookCanAddPayload() {
            provider.setResult("test.limit", PermissionResult.ALLOWED);
            
            // Register a hook that adds a payload
            PermissionCheckCallback.EVENT.register((p, key, ctx, result) -> {
                if (key.equals("test.limit")) {
                    return PermissionCheckCallback.CheckResult.allow(100);
                }
                return PermissionCheckCallback.CheckResult.unmodified();
            });
            
            PermissionResult result = service.query(player, "test.limit");
            
            assertEquals(100, result.asInt(0));
        }

        @Test
        @DisplayName("multiple hooks are called in order")
        void multipleHooksCalledInOrder() {
            provider.setResult("test", PermissionResult.UNDEFINED);
            
            // First hook sets to ALLOW with payload 10
            PermissionCheckCallback.EVENT.register((p, key, ctx, result) -> 
                PermissionCheckCallback.CheckResult.allow(10)
            );
            
            // Second hook doubles the payload
            PermissionCheckCallback.EVENT.register((p, key, ctx, result) -> {
                int value = result.asInt(0);
                return PermissionCheckCallback.CheckResult.allow(value * 2);
            });
            
            PermissionResult result = service.query(player, "test");
            
            assertEquals(20, result.asInt(0));
        }

        @Test
        @DisplayName("hook receives correct parameters")
        void hookReceivesCorrectParameters() {
            AtomicBoolean checked = new AtomicBoolean(false);
            ContextSet context = ContextSet.of(ContextKey.WORLD, "nether");
            
            PermissionCheckCallback.EVENT.register((p, key, ctx, result) -> {
                assertEquals(player, p);
                assertEquals("test.permission", key);
                assertEquals(context, ctx);
                checked.set(true);
                return PermissionCheckCallback.CheckResult.unmodified();
            });
            
            service.query(player, "test.permission", context);
            
            assertTrue(checked.get());
        }
    }

    @Nested
    @DisplayName("Player lifecycle")
    class PlayerLifecycleTests {

        @Test
        @DisplayName("loadPlayer delegates to provider")
        void loadPlayerDelegatesToProvider() {
            service.loadPlayer(player);
            
            assertTrue(provider.loadedPlayers.contains(player.getUniqueId()));
        }

        @Test
        @DisplayName("unloadPlayer delegates to provider")
        void unloadPlayerDelegatesToProvider() {
            service.unloadPlayer(player);
            
            assertTrue(provider.unloadedPlayers.contains(player.getUniqueId()));
        }

        @Test
        @DisplayName("loadPlayer does nothing without provider")
        void loadPlayerSafeWithoutProvider() {
            service.shutdown();
            
            // Should not throw
            CompletableFuture<Void> future = service.loadPlayer(player);
            assertNotNull(future);
        }
    }

    // Test provider implementation
    private static class TestPermissionProvider implements PermissionProvider {
        boolean enabled = false;
        boolean disabled = false;
        java.util.Set<String> loadedPlayers = new java.util.HashSet<>();
        java.util.Set<String> unloadedPlayers = new java.util.HashSet<>();
        
        private final java.util.Map<String, PermissionResult> results = new java.util.HashMap<>();
        private final java.util.Map<String, java.util.Map<ContextSet, PermissionResult>> contextResults = new java.util.HashMap<>();

        void setResult(String key, PermissionResult result) {
            results.put(key, result);
        }

        void setResult(String key, ContextSet context, PermissionResult result) {
            contextResults.computeIfAbsent(key, k -> new java.util.HashMap<>()).put(context, result);
        }

        @Override
        public String getId() { return "test"; }

        @Override
        public String getName() { return "Test Provider"; }

        @Override
        public PermissionResult query(TalePlayer player, String key) {
            return results.getOrDefault(key, PermissionResult.UNDEFINED);
        }

        @Override
        public PermissionResult query(TalePlayer player, String key, ContextSet context) {
            var contextMap = contextResults.get(key);
            if (contextMap != null) {
                PermissionResult result = contextMap.get(context);
                if (result != null) return result;
            }
            return query(player, key);
        }

        @Override
        public PermissionTree getPlayerTree(TalePlayer player) { return null; }

        @Override
        public CompletableFuture<Void> setPermission(TalePlayer player, PermissionNode node) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> removePermission(TalePlayer player, String key) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public Set<String> getClientSyncedNodes(TalePlayer player) { return Set.of(); }

        @Override
        public CompletableFuture<Void> loadPlayer(TalePlayer player) {
            loadedPlayers.add(player.getUniqueId());
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void unloadPlayer(TalePlayer player) {
            unloadedPlayers.add(player.getUniqueId());
        }

        @Override
        public CompletableFuture<Void> invalidateCache(TalePlayer player) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onEnable() { enabled = true; }

        @Override
        public void onDisable() { disabled = true; }
    }
}

