package dev.polv.taleapi.command;

/**
 * Functional interface for command execution logic.
 * <p>
 * Implementations receive a {@link CommandContext} containing all parsed
 * arguments and the command sender, and return a {@link CommandResult}
 * indicating the outcome.
 * </p>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * CommandExecutor executor = ctx -> {
 *     String name = ctx.getArgument("name", String.class);
 *     ctx.getSender().sendMessage("Hello, " + name + "!");
 *     return CommandResult.SUCCESS;
 * };
 * }</pre>
 */
@FunctionalInterface
public interface CommandExecutor {

  /**
   * Executes the command with the given context.
   *
   * @param context the command context containing sender and arguments
   * @return the result of the command execution
   * @throws CommandException if the command fails due to invalid arguments or state
   */
  CommandResult execute(CommandContext context) throws CommandException;
}

