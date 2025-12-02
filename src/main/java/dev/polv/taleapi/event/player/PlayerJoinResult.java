package dev.polv.taleapi.event.player;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Represents the result of a player join event handler.
 * <p>
 * This extends the basic event result concept to include an optional kick message
 * that can be displayed to players when their join is cancelled.
 * </p>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Simple pass - allow the player to join
 * return PlayerJoinResult.pass();
 *
 * // Cancel without a message
 * return PlayerJoinResult.cancel();
 *
 * // Cancel with a custom kick message
 * return PlayerJoinResult.cancel("You are banned from this server!");
 *
 * // Async example with custom message
 * return database.isPlayerBanned(player.getUUID())
 *     .thenApply(banned -> banned
 *         ? PlayerJoinResult.cancelled("You have been banned.")
 *         : PlayerJoinResult.passed());
 * }</pre>
 */
public final class PlayerJoinResult {

  private static final PlayerJoinResult PASS = new PlayerJoinResult(Type.PASS, null);
  private static final PlayerJoinResult SUCCESS = new PlayerJoinResult(Type.SUCCESS, null);
  private static final PlayerJoinResult CANCEL = new PlayerJoinResult(Type.CANCEL, null);

  private static final CompletableFuture<PlayerJoinResult> PASS_FUTURE = CompletableFuture.completedFuture(PASS);
  private static final CompletableFuture<PlayerJoinResult> SUCCESS_FUTURE = CompletableFuture.completedFuture(SUCCESS);
  private static final CompletableFuture<PlayerJoinResult> CANCEL_FUTURE = CompletableFuture.completedFuture(CANCEL);

  /**
   * The type of result.
   */
  public enum Type {
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
     * and the player will be kicked from the server.
     */
    CANCEL
  }

  private final Type type;
  private final String kickMessage;

  private PlayerJoinResult(Type type, String kickMessage) {
    this.type = type;
    this.kickMessage = kickMessage;
  }

  /**
   * Creates a PASS result - continue to the next listener.
   *
   * @return the passed result instance
   */
  public static PlayerJoinResult passed() {
    return PASS;
  }

  /**
   * Creates a SUCCESS result - stop processing, event handled successfully.
   *
   * @return the success result instance
   */
  public static PlayerJoinResult success() {
    return SUCCESS;
  }

  /**
   * Creates a CANCEL result without a kick message.
   *
   * @return the cancelled result instance
   */
  public static PlayerJoinResult cancelled() {
    return CANCEL;
  }

  /**
   * Creates a CANCEL result with a custom kick message.
   *
   * @param kickMessage the message to display to the player when kicked
   * @return a new cancelled result with the kick message
   */
  public static PlayerJoinResult cancelled(String kickMessage) {
    Objects.requireNonNull(kickMessage, "kickMessage");
    return new PlayerJoinResult(Type.CANCEL, kickMessage);
  }

  /**
   * Returns a completed future with a PASS result.
   * <p>
   * Convenience method for handlers that want to pass synchronously.
   * </p>
   *
   * @return a completed future containing a PASS result
   */
  public static CompletableFuture<PlayerJoinResult> pass() {
    return PASS_FUTURE;
  }

  /**
   * Returns a completed future with a CANCEL result without a kick message.
   * <p>
   * Convenience method for handlers that want to cancel synchronously.
   * </p>
   *
   * @return a completed future containing a CANCEL result
   */
  public static CompletableFuture<PlayerJoinResult> cancel() {
    return CANCEL_FUTURE;
  }

  /**
   * Returns a completed future with a CANCEL result and a custom kick message.
   * <p>
   * Convenience method for handlers that want to cancel synchronously with a message.
   * </p>
   *
   * @param kickMessage the message to display to the player when kicked
   * @return a completed future containing a CANCEL result with the kick message
   */
  public static CompletableFuture<PlayerJoinResult> cancel(String kickMessage) {
    Objects.requireNonNull(kickMessage, "kickMessage");
    return CompletableFuture.completedFuture(new PlayerJoinResult(Type.CANCEL, kickMessage));
  }

  /**
   * @return the result type
   */
  public Type getType() {
    return type;
  }

  /**
   * @return {@code true} if this result stops further event processing
   */
  public boolean shouldStop() {
    return type != Type.PASS;
  }

  /**
   * @return {@code true} if this result indicates the event was cancelled
   */
  public boolean isCancelled() {
    return type == Type.CANCEL;
  }

  /**
   * Returns the kick message, if one was specified.
   *
   * @return an Optional containing the kick message, or empty if none was specified
   */
  public Optional<String> getKickMessage() {
    return Optional.ofNullable(kickMessage);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PlayerJoinResult that = (PlayerJoinResult) o;
    return type == that.type && Objects.equals(kickMessage, that.kickMessage);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, kickMessage);
  }

  @Override
  public String toString() {
    if (kickMessage != null) {
      return "PlayerJoinResult{type=" + type + ", kickMessage='" + kickMessage + "'}";
    }
    return "PlayerJoinResult{type=" + type + "}";
  }
}
