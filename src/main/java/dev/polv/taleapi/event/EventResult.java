package dev.polv.taleapi.event;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the result of an event callback execution.
 * <p>
 * Used to control event flow and cancellation behavior.
 * </p>
 *
 * <ul>
 * <li>{@link #PASS} - Continue to the next listener, no opinion on the
 * outcome</li>
 * <li>{@link #SUCCESS} - Stop processing and indicate success</li>
 * <li>{@link #CANCEL} - Stop processing and cancel the event</li>
 * </ul>
 *
 * <h2>Async Events</h2>
 * <p>
 * For async events that return {@code CompletableFuture<EventResult>}, use
 * the convenience methods {@link #pass()}, {@link #success()}, and
 * {@link #cancel()}
 * to return completed futures for synchronous handlers.
 * </p>
 */
public enum EventResult {
  /**
   * Continue processing the event. The listener has no opinion on the outcome.
   */
  PASS,

  /**
   * Stop processing and indicate the event was handled successfully.
   * Subsequent listeners will not be called.
   */
  SUCCESS,

  /**
   * Cancel the event. Subsequent listeners will not be called,
   * and the action that triggered the event should be prevented.
   */
  CANCEL;

  // Pre-computed completed futures for efficiency
  private static final CompletableFuture<EventResult> PASS_FUTURE = CompletableFuture.completedFuture(PASS);
  private static final CompletableFuture<EventResult> SUCCESS_FUTURE = CompletableFuture.completedFuture(SUCCESS);
  private static final CompletableFuture<EventResult> CANCEL_FUTURE = CompletableFuture.completedFuture(CANCEL);

  /**
   * @return {@code true} if this result stops further event processing
   */
  public boolean shouldStop() {
    return this != PASS;
  }

  /**
   * @return {@code true} if this result indicates the event was cancelled
   */
  public boolean isCancelled() {
    return this == CANCEL;
  }

  /**
   * Returns this result wrapped in a completed {@link CompletableFuture}.
   * <p>
   * This is useful for async event callbacks that need to return a future
   * but are doing synchronous processing.
   * </p>
   *
   * @return a completed future containing this result
   */
  public CompletableFuture<EventResult> asFuture() {
    return switch (this) {
      case PASS -> PASS_FUTURE;
      case SUCCESS -> SUCCESS_FUTURE;
      case CANCEL -> CANCEL_FUTURE;
    };
  }

  /**
   * Returns a completed future with {@link #PASS}.
   * <p>
   * Convenience method for async event handlers that want to pass synchronously.
   * </p>
   *
   * @return a completed future containing PASS
   */
  public static CompletableFuture<EventResult> pass() {
    return PASS_FUTURE;
  }

  /**
   * Returns a completed future with {@link #SUCCESS}.
   * <p>
   * Convenience method for async event handlers that want to succeed
   * synchronously.
   * </p>
   *
   * @return a completed future containing SUCCESS
   */
  public static CompletableFuture<EventResult> success() {
    return SUCCESS_FUTURE;
  }

  /**
   * Returns a completed future with {@link #CANCEL}.
   * <p>
   * Convenience method for async event handlers that want to cancel
   * synchronously.
   * </p>
   *
   * @return a completed future containing CANCEL
   */
  public static CompletableFuture<EventResult> cancel() {
    return CANCEL_FUTURE;
  }
}
