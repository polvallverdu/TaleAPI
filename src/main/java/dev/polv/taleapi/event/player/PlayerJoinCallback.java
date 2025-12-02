package dev.polv.taleapi.event.player;

import dev.polv.taleapi.entity.TalePlayer;
import dev.polv.taleapi.event.Event;
import dev.polv.taleapi.event.EventResult;

import java.util.concurrent.CompletableFuture;

/**
 * Called when a player joins the server.
 * <p>
 * This event is cancellable and fully async. If cancelled,
 * the player will be prevented from joining (kicked).
 * </p>
 * <p>
 * Handlers return a {@link CompletableFuture} of {@link EventResult}.
 * Use {@link EventResult#pass()}, {@link EventResult#cancel()}, etc. for
 * synchronous handlers, or return a future directly for async operations.
 * </p>
 *
 * <h2>Example Usage</h2>
 * 
 * <pre>{@code
 * // Synchronous handler
 * PlayerJoinCallback.EVENT.register(player -> {
 *   player.sendMessage("Welcome to the server, " + player.getName() + "!");
 *   return EventResult.pass();
 * });
 *
 * // Async handler - database ban check
 * PlayerJoinCallback.EVENT.register(EventPriority.HIGHEST, player -> database.isPlayerBanned(player.getUUID())
 *     .thenApply(banned -> banned ? EventResult.CANCEL : EventResult.PASS));
 *
 * // Async handler with blocking operation
 * PlayerJoinCallback.EVENT.register(player -> CompletableFuture.supplyAsync(() -> {
 *   boolean banned = blockingBanCheck(player.getUUID());
 *   return banned ? EventResult.CANCEL : EventResult.PASS;
 * }, dbExecutor));
 * }</pre>
 *
 * <h2>Firing the Event</h2>
 * 
 * <pre>{@code
 * PlayerJoinCallback.EVENT.invoker().onPlayerJoin(player)
 *     .thenAccept(result -> {
 *       if (result.isCancelled()) {
 *         player.kick("You are not allowed to join.");
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
          callback -> callback.onPlayerJoin(player)),
      player -> EventResult.pass());

  /**
   * Called when a player joins the server.
   *
   * @param player the player who is joining
   * @return a future containing the event result - {@link EventResult#CANCEL} to
   *         prevent the join.
   *         Use {@link EventResult#pass()}, {@link EventResult#cancel()}, etc.
   *         for sync responses.
   */
  CompletableFuture<EventResult> onPlayerJoin(TalePlayer player);
}
