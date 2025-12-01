package dev.polv.taleapi.command;

import dev.polv.taleapi.event.Event;
import dev.polv.taleapi.event.EventResult;

/**
 * Called when a command is about to be executed.
 * <p>
 * This event fires before the command is dispatched, allowing listeners to:
 * <ul>
 *   <li>Cancel the command execution entirely</li>
 *   <li>Log command usage</li>
 *   <li>Implement command cooldowns</li>
 *   <li>Block specific commands in certain contexts (e.g., during a minigame)</li>
 * </ul>
 * </p>
 * <p>
 * This event is cancellable. If cancelled, the command will not be executed
 * and no error message will be sent to the sender (you should send your own).
 * </p>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Log all command usage
 * CommandExecuteCallback.EVENT.register((sender, command, input) -> {
 *     System.out.println(sender.getName() + " executed: /" + input);
 *     return EventResult.PASS;
 * });
 *
 * // Block commands during a match
 * CommandExecuteCallback.EVENT.register(EventPriority.HIGHEST, (sender, command, input) -> {
 *     if (isInMatch(sender) && !command.getName().equals("leave")) {
 *         sender.sendMessage("Commands are disabled during the match!");
 *         return EventResult.CANCEL;
 *     }
 *     return EventResult.PASS;
 * });
 *
 * // Implement command cooldowns
 * CommandExecuteCallback.EVENT.register((sender, command, input) -> {
 *     if (isOnCooldown(sender, command)) {
 *         sender.sendMessage("Please wait before using this command again!");
 *         return EventResult.CANCEL;
 *     }
 *     setCooldown(sender, command);
 *     return EventResult.PASS;
 * });
 * }</pre>
 */
@FunctionalInterface
public interface CommandExecuteCallback {

  /**
   * The event instance. Use this to register listeners and fire the event.
   */
  Event<CommandExecuteCallback> EVENT = Event.create(
      callbacks -> (sender, command, input) -> {
        for (CommandExecuteCallback callback : callbacks) {
          EventResult result = callback.onCommandExecute(sender, command, input);
          if (result.shouldStop()) {
            return result;
          }
        }
        return EventResult.PASS;
      },
      (sender, command, input) -> EventResult.PASS // Empty invoker - no listeners, just pass
  );

  /**
   * Called when a command is about to be executed.
   *
   * @param sender  the entity executing the command (player or console)
   * @param command the command being executed
   * @param input   the full command input string (without leading slash)
   * @return the event result - {@link EventResult#CANCEL} to prevent execution
   */
  EventResult onCommandExecute(CommandSender sender, Command command, String input);
}

