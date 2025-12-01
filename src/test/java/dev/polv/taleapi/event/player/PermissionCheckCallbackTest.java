package dev.polv.taleapi.event.player;

import dev.polv.taleapi.event.EventPriority;
import dev.polv.taleapi.permission.ContextSet;
import dev.polv.taleapi.permission.PermissionResult;
import dev.polv.taleapi.permission.Tristate;
import dev.polv.taleapi.testutil.TestPlayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PermissionCheckCallback")
class PermissionCheckCallbackTest {

    private TestPlayer player;

    @BeforeEach
    void setUp() {
        player = new TestPlayer("TestUser");
    }

    @AfterEach
    void tearDown() {
        PermissionCheckCallback.EVENT.clearListeners();
    }

    @Nested
    @DisplayName("CheckResult factory methods")
    class CheckResultFactoryTests {

        @Test
        @DisplayName("unmodified() creates non-modifying result")
        void unmodifiedCreatesNonModifying() {
            PermissionCheckCallback.CheckResult result = PermissionCheckCallback.CheckResult.unmodified();
            
            assertFalse(result.isModified());
        }

        @Test
        @DisplayName("allow() creates ALLOW result")
        void allowCreatesAllowResult() {
            PermissionCheckCallback.CheckResult result = PermissionCheckCallback.CheckResult.allow();
            
            assertTrue(result.isModified());
            assertTrue(result.getResult().isAllowed());
        }

        @Test
        @DisplayName("allow(payload) creates ALLOW with payload")
        void allowWithPayloadCreatesAllowWithPayload() {
            PermissionCheckCallback.CheckResult result = PermissionCheckCallback.CheckResult.allow(42);
            
            assertTrue(result.isModified());
            assertTrue(result.getResult().isAllowed());
            assertEquals(42, result.getResult().asInt(0));
        }

        @Test
        @DisplayName("deny() creates DENY result")
        void denyCreatesDenyResult() {
            PermissionCheckCallback.CheckResult result = PermissionCheckCallback.CheckResult.deny();
            
            assertTrue(result.isModified());
            assertTrue(result.getResult().isDenied());
        }

        @Test
        @DisplayName("deny(payload) creates DENY with payload")
        void denyWithPayloadCreatesDenyWithPayload() {
            PermissionCheckCallback.CheckResult result = PermissionCheckCallback.CheckResult.deny("reason");
            
            assertTrue(result.isModified());
            assertTrue(result.getResult().isDenied());
            assertEquals("reason", result.getResult().asString());
        }

        @Test
        @DisplayName("override() creates modified result with custom PermissionResult")
        void overrideCreatesModifiedResult() {
            PermissionResult permResult = PermissionResult.of(Tristate.ALLOW, 100);
            PermissionCheckCallback.CheckResult result = PermissionCheckCallback.CheckResult.override(permResult);
            
            assertTrue(result.isModified());
            assertEquals(permResult, result.getResult());
        }

        @Test
        @DisplayName("of() wraps existing result as non-modified")
        void ofWrapsAsNonModified() {
            PermissionResult permResult = PermissionResult.ALLOWED;
            PermissionCheckCallback.CheckResult result = PermissionCheckCallback.CheckResult.of(permResult);
            
            assertFalse(result.isModified());
            assertEquals(permResult, result.getResult());
        }
    }

    @Nested
    @DisplayName("Event invocation")
    class EventInvocationTests {

        @Test
        @DisplayName("listener receives all parameters")
        void listenerReceivesAllParameters() {
            List<Object[]> receivedParams = new ArrayList<>();
            
            PermissionCheckCallback.EVENT.register((p, key, context, result) -> {
                receivedParams.add(new Object[]{p, key, context, result});
                return PermissionCheckCallback.CheckResult.unmodified();
            });
            
            PermissionResult initialResult = PermissionResult.ALLOWED;
            ContextSet context = ContextSet.of(dev.polv.taleapi.permission.ContextKey.WORLD, "test");
            
            PermissionCheckCallback.EVENT.invoker().onPermissionCheck(
                player, "test.permission", context, initialResult);
            
            assertEquals(1, receivedParams.size());
            assertEquals(player, receivedParams.get(0)[0]);
            assertEquals("test.permission", receivedParams.get(0)[1]);
            assertEquals(context, receivedParams.get(0)[2]);
            assertEquals(initialResult, receivedParams.get(0)[3]);
        }

        @Test
        @DisplayName("unmodified result passes through original")
        void unmodifiedPassesThrough() {
            PermissionCheckCallback.EVENT.register((p, key, context, result) -> 
                PermissionCheckCallback.CheckResult.unmodified()
            );
            
            PermissionCheckCallback.CheckResult result = PermissionCheckCallback.EVENT.invoker()
                .onPermissionCheck(player, "test", ContextSet.EMPTY, PermissionResult.ALLOWED);
            
            assertFalse(result.isModified());
            assertTrue(result.getResult().isAllowed());
        }

        @Test
        @DisplayName("modified result replaces original")
        void modifiedReplacesOriginal() {
            PermissionCheckCallback.EVENT.register((p, key, context, result) -> 
                PermissionCheckCallback.CheckResult.deny()
            );
            
            PermissionCheckCallback.CheckResult result = PermissionCheckCallback.EVENT.invoker()
                .onPermissionCheck(player, "test", ContextSet.EMPTY, PermissionResult.ALLOWED);
            
            assertTrue(result.isModified());
            assertTrue(result.getResult().isDenied());
        }

        @Test
        @DisplayName("later listener sees previous listener's modification")
        void laterSeesEarlierModification() {
            // First listener sets to ALLOW with payload 10
            PermissionCheckCallback.EVENT.register((p, key, context, result) -> 
                PermissionCheckCallback.CheckResult.allow(10)
            );
            
            // Second listener sees the modified result
            PermissionCheckCallback.EVENT.register((p, key, context, result) -> {
                assertEquals(10, result.asInt(0));
                // Double the payload
                return PermissionCheckCallback.CheckResult.allow(result.asInt(0) * 2);
            });
            
            PermissionCheckCallback.CheckResult result = PermissionCheckCallback.EVENT.invoker()
                .onPermissionCheck(player, "test", ContextSet.EMPTY, PermissionResult.UNDEFINED);
            
            assertEquals(20, result.getResult().asInt(0));
        }

        @Test
        @DisplayName("conditional modification only when needed")
        void conditionalModification() {
            PermissionCheckCallback.EVENT.register((p, key, context, result) -> {
                // Only modify teleport permissions during a match
                if (key.startsWith("cmd.teleport")) {
                    return PermissionCheckCallback.CheckResult.deny();
                }
                return PermissionCheckCallback.CheckResult.unmodified();
            });
            
            // Teleport should be denied
            PermissionCheckCallback.CheckResult teleportResult = PermissionCheckCallback.EVENT.invoker()
                .onPermissionCheck(player, "cmd.teleport.home", ContextSet.EMPTY, PermissionResult.ALLOWED);
            assertTrue(teleportResult.getResult().isDenied());
            
            // Other permissions pass through
            PermissionCheckCallback.CheckResult otherResult = PermissionCheckCallback.EVENT.invoker()
                .onPermissionCheck(player, "cmd.help", ContextSet.EMPTY, PermissionResult.ALLOWED);
            assertTrue(otherResult.getResult().isAllowed());
        }
    }

    @Nested
    @DisplayName("Priority handling")
    class PriorityTests {

        @Test
        @DisplayName("higher priority runs first")
        void higherPriorityRunsFirst() {
            List<String> order = new ArrayList<>();
            
            PermissionCheckCallback.EVENT.register(EventPriority.LOW, (p, key, context, result) -> {
                order.add("LOW");
                return PermissionCheckCallback.CheckResult.unmodified();
            });
            
            PermissionCheckCallback.EVENT.register(EventPriority.HIGH, (p, key, context, result) -> {
                order.add("HIGH");
                return PermissionCheckCallback.CheckResult.unmodified();
            });
            
            PermissionCheckCallback.EVENT.invoker()
                .onPermissionCheck(player, "test", ContextSet.EMPTY, PermissionResult.UNDEFINED);
            
            assertEquals(List.of("HIGH", "LOW"), order);
        }

        @Test
        @DisplayName("high priority can set value before low priority processes")
        void highPriorityCanSetBeforeLowProcesses() {
            // HIGH priority sets the value
            PermissionCheckCallback.EVENT.register(EventPriority.HIGH, (p, key, context, result) -> 
                PermissionCheckCallback.CheckResult.allow(100)
            );
            
            // LOW priority reads the value set by HIGH
            PermissionCheckCallback.EVENT.register(EventPriority.LOW, (p, key, context, result) -> {
                assertEquals(100, result.asInt(0));
                return PermissionCheckCallback.CheckResult.unmodified();
            });
            
            PermissionCheckCallback.EVENT.invoker()
                .onPermissionCheck(player, "test", ContextSet.EMPTY, PermissionResult.UNDEFINED);
        }
    }

    @Nested
    @DisplayName("No listeners")
    class NoListenersTests {

        @Test
        @DisplayName("returns original result when no listeners")
        void returnsOriginalWhenNoListeners() {
            PermissionCheckCallback.CheckResult result = PermissionCheckCallback.EVENT.invoker()
                .onPermissionCheck(player, "test", ContextSet.EMPTY, PermissionResult.ALLOWED);
            
            assertEquals(PermissionResult.ALLOWED, result.getResult());
        }
    }

    @Nested
    @DisplayName("Use case: Minigame permission override")
    class MinigameUseCaseTests {

        @Test
        @DisplayName("deny teleport during match")
        void denyTeleportDuringMatch() {
            // Simulate a minigame plugin that denies teleport during matches
            boolean inMatch = true;
            
            PermissionCheckCallback.EVENT.register((p, key, context, result) -> {
                if (inMatch && key.startsWith("cmd.teleport")) {
                    return PermissionCheckCallback.CheckResult.deny("Cannot teleport during match");
                }
                return PermissionCheckCallback.CheckResult.unmodified();
            });
            
            PermissionCheckCallback.CheckResult result = PermissionCheckCallback.EVENT.invoker()
                .onPermissionCheck(player, "cmd.teleport.spawn", ContextSet.EMPTY, PermissionResult.ALLOWED);
            
            assertTrue(result.getResult().isDenied());
            assertEquals("Cannot teleport during match", result.getResult().asString());
        }

        @Test
        @DisplayName("grant temporary fly during event")
        void grantTemporaryFlyDuringEvent() {
            // Simulate an event plugin that grants fly
            boolean hasEventFly = true;
            
            PermissionCheckCallback.EVENT.register((p, key, context, result) -> {
                if (hasEventFly && key.equals("ability.fly")) {
                    return PermissionCheckCallback.CheckResult.allow();
                }
                return PermissionCheckCallback.CheckResult.unmodified();
            });
            
            // Even if underlying permission is DENY, event grants it
            PermissionCheckCallback.CheckResult result = PermissionCheckCallback.EVENT.invoker()
                .onPermissionCheck(player, "ability.fly", ContextSet.EMPTY, PermissionResult.DENIED);
            
            assertTrue(result.getResult().isAllowed());
        }
    }
}

