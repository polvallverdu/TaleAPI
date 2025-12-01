package dev.polv.taleapi.event;

/**
 * Defines the priority order for event listener execution.
 * <p>
 * Listeners are executed from {@link #HIGHEST} to {@link #LOWEST}.
 * Higher priority listeners run first, giving them first access to handle
 * events.
 * </p>
 *
 * <p>
 * Execution order: HIGHEST → HIGH → NORMAL → LOW → LOWEST
 * </p>
 */
public enum EventPriority {
  /**
   * Executed last. Use for listeners that run after all others.
   */
  LOWEST,

  /**
   * Executed after NORMAL.
   */
  LOW,

  /**
   * Default priority. Use for most listeners.
   */
  NORMAL,

  /**
   * Executed after HIGHEST.
   */
  HIGH,

  /**
   * Executed first. Use for listeners that need priority access
   * to handle the event before others.
   */
  HIGHEST
}
