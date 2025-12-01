package dev.polv.taleapi.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Event System")
class EventTest {

  @FunctionalInterface
  interface TestCallback {
    EventResult onTest(String value);
  }

  private Event<TestCallback> createTestEvent() {
    return Event.create(
        callbacks -> value -> {
          for (TestCallback callback : callbacks) {
            EventResult result = callback.onTest(value);
            if (result.shouldStop()) {
              return result;
            }
          }
          return EventResult.PASS;
        },
        value -> EventResult.PASS);
  }

  @Nested
  @DisplayName("Basic Registration")
  class BasicRegistration {

    @Test
    @DisplayName("should start with zero listeners")
    void shouldStartEmpty() {
      Event<TestCallback> event = createTestEvent();
      assertEquals(0, event.listenerCount());
    }

    @Test
    @DisplayName("should register a listener")
    void shouldRegisterListener() {
      Event<TestCallback> event = createTestEvent();
      event.register(value -> EventResult.PASS);
      assertEquals(1, event.listenerCount());
    }

    @Test
    @DisplayName("should register multiple listeners")
    void shouldRegisterMultipleListeners() {
      Event<TestCallback> event = createTestEvent();
      event.register(value -> EventResult.PASS);
      event.register(value -> EventResult.PASS);
      event.register(value -> EventResult.PASS);
      assertEquals(3, event.listenerCount());
    }

    @Test
    @DisplayName("should unregister a listener")
    void shouldUnregisterListener() {
      Event<TestCallback> event = createTestEvent();
      TestCallback listener = value -> EventResult.PASS;
      event.register(listener);
      assertEquals(1, event.listenerCount());

      boolean removed = event.unregister(listener);
      assertTrue(removed);
      assertEquals(0, event.listenerCount());
    }

    @Test
    @DisplayName("should clear all listeners")
    void shouldClearListeners() {
      Event<TestCallback> event = createTestEvent();
      event.register(EventPriority.LOW, value -> EventResult.PASS);
      event.register(EventPriority.NORMAL, value -> EventResult.PASS);
      event.register(EventPriority.HIGH, value -> EventResult.PASS);
      assertEquals(3, event.listenerCount());

      event.clearListeners();
      assertEquals(0, event.listenerCount());
    }
  }

  @Nested
  @DisplayName("Invoker Execution")
  class InvokerExecution {

    @Test
    @DisplayName("should invoke listener and receive value")
    void shouldInvokeListener() {
      Event<TestCallback> event = createTestEvent();
      List<String> received = new ArrayList<>();

      event.register(value -> {
        received.add(value);
        return EventResult.PASS;
      });

      event.invoker().onTest("hello");
      assertEquals(List.of("hello"), received);
    }

    @Test
    @DisplayName("should invoke all listeners in order")
    void shouldInvokeAllListeners() {
      Event<TestCallback> event = createTestEvent();
      List<Integer> order = new ArrayList<>();

      event.register(value -> {
        order.add(1);
        return EventResult.PASS;
      });
      event.register(value -> {
        order.add(2);
        return EventResult.PASS;
      });
      event.register(value -> {
        order.add(3);
        return EventResult.PASS;
      });

      event.invoker().onTest("test");
      assertEquals(List.of(1, 2, 3), order);
    }

    @Test
    @DisplayName("should return PASS when no listeners registered")
    void shouldReturnPassWhenEmpty() {
      Event<TestCallback> event = createTestEvent();
      EventResult result = event.invoker().onTest("test");
      assertEquals(EventResult.PASS, result);
    }
  }

  @Nested
  @DisplayName("Cancellation")
  class Cancellation {

    @Test
    @DisplayName("should stop on CANCEL result")
    void shouldStopOnCancel() {
      Event<TestCallback> event = createTestEvent();
      List<Integer> order = new ArrayList<>();

      event.register(value -> {
        order.add(1);
        return EventResult.PASS;
      });
      event.register(value -> {
        order.add(2);
        return EventResult.CANCEL;
      });
      event.register(value -> {
        order.add(3);
        return EventResult.PASS;
      });

      EventResult result = event.invoker().onTest("test");

      assertEquals(EventResult.CANCEL, result);
      assertEquals(List.of(1, 2), order); // Third listener not called
    }

    @Test
    @DisplayName("should stop on SUCCESS result")
    void shouldStopOnSuccess() {
      Event<TestCallback> event = createTestEvent();
      List<Integer> order = new ArrayList<>();

      event.register(value -> {
        order.add(1);
        return EventResult.SUCCESS;
      });
      event.register(value -> {
        order.add(2);
        return EventResult.PASS;
      });

      EventResult result = event.invoker().onTest("test");

      assertEquals(EventResult.SUCCESS, result);
      assertEquals(List.of(1), order);
    }
  }

  @Nested
  @DisplayName("Priority Ordering")
  class PriorityOrdering {

    @Test
    @DisplayName("should execute in priority order (HIGHEST to LOWEST)")
    void shouldExecuteInPriorityOrder() {
      Event<TestCallback> event = createTestEvent();
      List<String> order = new ArrayList<>();

      // Register in random order
      event.register(EventPriority.HIGH, value -> {
        order.add("HIGH");
        return EventResult.PASS;
      });
      event.register(EventPriority.LOWEST, value -> {
        order.add("LOWEST");
        return EventResult.PASS;
      });
      event.register(EventPriority.NORMAL, value -> {
        order.add("NORMAL");
        return EventResult.PASS;
      });
      event.register(EventPriority.HIGHEST, value -> {
        order.add("HIGHEST");
        return EventResult.PASS;
      });
      event.register(EventPriority.LOW, value -> {
        order.add("LOW");
        return EventResult.PASS;
      });

      event.invoker().onTest("test");

      assertEquals(List.of("HIGHEST", "HIGH", "NORMAL", "LOW", "LOWEST"), order);
    }

    @Test
    @DisplayName("should maintain insertion order within same priority")
    void shouldMaintainInsertionOrderWithinPriority() {
      Event<TestCallback> event = createTestEvent();
      List<Integer> order = new ArrayList<>();

      event.register(EventPriority.NORMAL, value -> {
        order.add(1);
        return EventResult.PASS;
      });
      event.register(EventPriority.NORMAL, value -> {
        order.add(2);
        return EventResult.PASS;
      });
      event.register(EventPriority.NORMAL, value -> {
        order.add(3);
        return EventResult.PASS;
      });

      event.invoker().onTest("test");

      assertEquals(List.of(1, 2, 3), order);
    }

    @Test
    @DisplayName("high priority listener can cancel before low priority runs")
    void highPriorityCanCancel() {
      Event<TestCallback> event = createTestEvent();
      AtomicInteger lowestCalled = new AtomicInteger(0);
      AtomicInteger highestCalled = new AtomicInteger(0);

      event.register(EventPriority.LOWEST, value -> {
        lowestCalled.incrementAndGet();
        return EventResult.PASS;
      });
      event.register(EventPriority.HIGHEST, value -> {
        highestCalled.incrementAndGet();
        return EventResult.CANCEL;
      });

      EventResult result = event.invoker().onTest("test");

      assertEquals(EventResult.CANCEL, result);
      assertEquals(0, lowestCalled.get()); // Not called because HIGHEST cancelled first
      assertEquals(1, highestCalled.get());
    }
  }

  @Nested
  @DisplayName("Async Invocation")
  class AsyncInvocation {

    @Test
    @DisplayName("should invoke asynchronously")
    void shouldInvokeAsync() throws Exception {
      Event<TestCallback> event = createTestEvent();
      AtomicInteger counter = new AtomicInteger(0);

      event.register(value -> {
        counter.incrementAndGet();
        return EventResult.PASS;
      });

      var executor = Executors.newSingleThreadExecutor();
      try {
        CompletableFuture<EventResult> future = event.invokeAsync(
            executor,
            invoker -> invoker.onTest("async-test"));

        EventResult result = future.get();
        assertEquals(EventResult.PASS, result);
        assertEquals(1, counter.get());
      } finally {
        executor.shutdown();
      }
    }
  }

  @Nested
  @DisplayName("EventResult")
  class EventResultTest {

    @Test
    @DisplayName("PASS should not stop processing")
    void passShouldNotStop() {
      assertFalse(EventResult.PASS.shouldStop());
      assertFalse(EventResult.PASS.isCancelled());
    }

    @Test
    @DisplayName("SUCCESS should stop processing but not be cancelled")
    void successShouldStop() {
      assertTrue(EventResult.SUCCESS.shouldStop());
      assertFalse(EventResult.SUCCESS.isCancelled());
    }

    @Test
    @DisplayName("CANCEL should stop processing and be cancelled")
    void cancelShouldStopAndBeCancelled() {
      assertTrue(EventResult.CANCEL.shouldStop());
      assertTrue(EventResult.CANCEL.isCancelled());
    }
  }
}
