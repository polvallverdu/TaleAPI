package dev.polv.taleapi.command;

import dev.polv.taleapi.event.Event;

/**
 * Called when commands should be registered.
 * <p>
 * This event is fired during server startup to allow plugins to register
 * their commands. The provided {@link CommandRegistry} should be used
 * to register all commands.
 * </p>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * CommandRegisterCallback.EVENT.register(registry -> {
 *     // Register a simple command
 *     registry.register(Command.builder("hello")
 *         .description("Says hello to the player")
 *         .executes(ctx -> {
 *             ctx.getSender().sendMessage("Hello, " + ctx.getSender().getName() + "!");
 *             return CommandResult.SUCCESS;
 *         })
 *         .build());
 *
 *     // Register a command with arguments
 *     registry.register(Command.builder("greet")
 *         .description("Greets a player")
 *         .permission("server.greet")
 *         .then(Command.argument("player", StringArgumentType.word())
 *             .suggests(SuggestionProvider.of("Steve", "Alex"))
 *             .executes(ctx -> {
 *                 String player = ctx.getArgument("player", String.class);
 *                 ctx.getSender().sendMessage("Hello, " + player + "!");
 *                 return CommandResult.SUCCESS;
 *             }))
 *         .build());
 *
 *     // Register a command with subcommands
 *     registry.register(Command.builder("gamemode")
 *         .aliases("gm")
 *         .permission("server.gamemode")
 *         .then(Command.literal("survival").executes(ctx -> {
 *             ctx.getSender().sendMessage("Switching to survival");
 *             return CommandResult.SUCCESS;
 *         }))
 *         .then(Command.literal("creative").executes(ctx -> {
 *             ctx.getSender().sendMessage("Switching to creative");
 *             return CommandResult.SUCCESS;
 *         }))
 *         .build());
 * });
 * }</pre>
 */
@FunctionalInterface
public interface CommandRegisterCallback {

  /**
   * The event instance. Use this to register listeners.
   */
  Event<CommandRegisterCallback> EVENT = Event.create(
      callbacks -> registry -> {
        for (CommandRegisterCallback callback : callbacks) {
          callback.onRegisterCommands(registry);
        }
      },
      registry -> {} // Empty invoker - no listeners, do nothing
  );

  /**
   * Called when commands should be registered.
   *
   * @param registry the command registry to register commands with
   */
  void onRegisterCommands(CommandRegistry registry);
}

