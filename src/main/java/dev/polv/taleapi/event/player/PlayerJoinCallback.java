package dev.polv.taleapi.event.player;

import dev.polv.taleapi.entity.TalePlayer;
import dev.polv.taleapi.event.Event;

import java.util.concurrent.CompletableFuture;

/**
 * Called when a player joins the server.
 * <p>
 * This event is cancellable and fully async. If cancelled,
 * the player will be prevented from joining (kicked).
 * </p>
 * <p>
 * Handlers return a {@link CompletableFuture} of {@link PlayerJoinResult}.
 * Use {@link PlayerJoinResult#pass()}, {@link PlayerJoinResult#cancel()}, etc. for
 * synchronous handlers, or return a future directly for async operations.
 * </p>
 * <p>
 * When cancelling, you can optionally provide a custom kick message using
 * {@link PlayerJoinResult#cancel(String)}.
 * </p>
 *
 * <h2>Example Usage</h2>
 * 
 * <pre>{@code
 * // Synchronous handler
 * PlayerJoinCallback.EVENT.register(player -> {
 *   player.sendMessage("Welcome to the server, " + player.getName() + "!");
 *   return PlayerJoinResult.pass();
 * });
 *
 * // Async handler - database ban check with custom kick message
 * PlayerJoinCallback.EVENT.register(EventPriority.HIGHEST, player -> database.isPlayerBanned(player.getUUID())
 *     .thenApply(banned -> banned
 *         ? PlayerJoinResult.cancelled("You are banned from this server!")
 *         : PlayerJoinResult.passed()));
 *
 * // Async handler with blocking operation
 * PlayerJoinCallback.EVENT.register(player -> CompletableFuture.supplyAsync(() -> {
 *   boolean banned = blockingBanCheck(player.getUUID());
 *   return banned
 *       ? PlayerJoinResult.cancelled("Access denied.")
 *       : PlayerJoinResult.passed();
 * }, dbExecutor));
 * }</pre>
 *
 * <h2>Firing the Event</h2>
 * 
 * <pre>{@code
 * PlayerJoinCallback.EVENT.invoker().onPlayerJoin(player)
 *     .thenAccept(result -> {
 *       if (result.isCancelled()) {
 *         String message = result.getKickMessage().orElse("You are not allowed to join.");
 *         player.kick(message);
 *       }
 *     });
 * }</pre>
 */
@FunctionalInterface
public interface PlayerJoinCallback {

  /**
   * The event instance. Use this to register listeners and fire the event.
   */
  Event<PlayerJoinCallback> EVENT = Event.create(
      callbacks -> player -> Event.invokeAsync(
          callbacks.iterator(),
          callback -> callback.onPlayerJoin(player),
          PlayerJoinResult::shouldStop,
          PlayerJoinResult.passed()),
      player -> PlayerJoinResult.pass());

  /**
   * Called when a player joins the server.
   *
   * @param player the player who is joining
   * @return a future containing the event result - use {@link PlayerJoinResult#cancelled()}
   *         or {@link PlayerJoinResult#cancelled(String)} to prevent the join.
   *         Use {@link PlayerJoinResult#pass()}, {@link PlayerJoinResult#cancel()}, etc.
   *         for sync responses.
   */
  CompletableFuture<PlayerJoinResult> onPlayerJoin(TalePlayer player);
}
