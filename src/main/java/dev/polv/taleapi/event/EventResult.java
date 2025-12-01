package dev.polv.taleapi.event;

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
}
