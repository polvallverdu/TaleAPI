package dev.polv.taleapi.event;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/**
 * A type-safe event holder that manages listener registration and invocation.
 * <p>
 * Each event type should have its own static {@code Event<T>} instance.
 * Listeners are functional interfaces that get combined into a single invoker.
 * </p>
 *
 * <h2>Example Usage</h2>
 * 
 * <pre>{@code
 * // Define an event callback interface
 * @FunctionalInterface
 * public interface MyCallback {
 *   EventResult onSomething(String data);
 *
 *   Event<MyCallback> EVENT = Event.create(callbacks -> data -> {
 *     for (var callback : callbacks) {
 *       EventResult result = callback.onSomething(data);
 *       if (result.shouldStop())
 *         return result;
 *     }
 *     return EventResult.PASS;
 *   });
 * }
 *
 * // Register a listener
 * MyCallback.EVENT.register(data -> {
 *   System.out.println("Received: " + data);
 *   return EventResult.PASS;
 * });
 *
 * // Fire the event
 * EventResult result = MyCallback.EVENT.invoker().onSomething("hello");
 * }</pre>
 *
 * @param <T> the callback functional interface type
 */
public final class Event<T> {

  private final Function<List<T>, T> invokerFactory;
  private final EnumMap<EventPriority, List<T>> listeners;
  private final ReentrantReadWriteLock lock;
  private volatile T invoker;
  private final T emptyInvoker;

  /**
   * Creates a new Event with the given invoker factory.
   *
   * @param invokerFactory function that combines multiple listeners into one
   *                       invoker
   * @param emptyInvoker   the invoker to use when no listeners are registered
   */
  Event(Function<List<T>, T> invokerFactory, T emptyInvoker) {
    this.invokerFactory = Objects.requireNonNull(invokerFactory, "invokerFactory");
    this.emptyInvoker = Objects.requireNonNull(emptyInvoker, "emptyInvoker");
    this.listeners = new EnumMap<>(EventPriority.class);
    this.lock = new ReentrantReadWriteLock();

    for (EventPriority priority : EventPriority.values()) {
      listeners.put(priority, new ArrayList<>());
    }

    this.invoker = emptyInvoker;
  }

  /**
   * Creates a new Event with priority support.
   *
   * @param invokerFactory function that combines a list of listeners into a
   *                       single invoker
   * @param emptyInvoker   the invoker to return when no listeners are registered
   * @param <T>            the callback type
   * @return a new Event instance
   */
  public static <T> Event<T> create(Function<List<T>, T> invokerFactory, T emptyInvoker) {
    return new Event<>(invokerFactory, emptyInvoker);
  }

  /**
   * Registers a listener with {@link EventPriority#NORMAL} priority.
   *
   * @param listener the listener to register
   * @throws NullPointerException if listener is null
   */
  public void register(T listener) {
    register(EventPriority.NORMAL, listener);
  }

  /**
   * Registers a listener with the specified priority.
   *
   * @param priority the execution priority
   * @param listener the listener to register
   * @throws NullPointerException if priority or listener is null
   */
  public void register(EventPriority priority, T listener) {
    Objects.requireNonNull(priority, "priority");
    Objects.requireNonNull(listener, "listener");

    lock.writeLock().lock();
    try {
      listeners.get(priority).add(listener);
      rebuildInvoker();
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Unregisters a listener from all priority levels.
   *
   * @param listener the listener to remove
   * @return {@code true} if the listener was found and removed
   */
  public boolean unregister(T listener) {
    lock.writeLock().lock();
    try {
      boolean removed = false;
      for (List<T> list : listeners.values()) {
        removed |= list.remove(listener);
      }
      if (removed) {
        rebuildInvoker();
      }
      return removed;
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Returns the combined invoker for all registered listeners.
   * <p>
   * The invoker executes listeners in priority order (HIGHEST to LOWEST).
   * </p>
   *
   * @return the invoker that will call all registered listeners
   */
  public T invoker() {
    return invoker;
  }

  /**
   * Fires the event asynchronously using the provided executor.
   * <p>
   * This is a convenience method for async event handling. The returned
   * CompletableFuture completes with the invoker result.
   * </p>
   *
   * @param executor the executor to run the event on
   * @param action   a function that calls the invoker and returns its result
   * @param <R>      the return type of the event callback
   * @return a CompletableFuture that completes with the event result
   */
  public <R> CompletableFuture<R> invokeAsync(Executor executor, Function<T, R> action) {
    return CompletableFuture.supplyAsync(() -> action.apply(invoker()), executor);
  }

  /**
   * @return the number of registered listeners across all priorities
   */
  public int listenerCount() {
    lock.readLock().lock();
    try {
      return listeners.values().stream()
          .mapToInt(List::size)
          .sum();
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Removes all registered listeners.
   */
  public void clearListeners() {
    lock.writeLock().lock();
    try {
      for (List<T> list : listeners.values()) {
        list.clear();
      }
      invoker = emptyInvoker;
    } finally {
      lock.writeLock().unlock();
    }
  }

  private void rebuildInvoker() {
    List<T> combined = new ArrayList<>();
    EventPriority[] priorities = EventPriority.values();

    // Iterate in reverse order: HIGHEST to LOWEST
    for (int i = priorities.length - 1; i >= 0; i--) {
      combined.addAll(listeners.get(priorities[i]));
    }

    if (combined.isEmpty()) {
      invoker = emptyInvoker;
    } else {
      invoker = invokerFactory.apply(combined);
    }
  }
}
