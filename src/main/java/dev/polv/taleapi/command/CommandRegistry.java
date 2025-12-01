package dev.polv.taleapi.command;

import dev.polv.taleapi.command.suggestion.Suggestions;
import dev.polv.taleapi.event.EventResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Central registry for all commands.
 * <p>
 * Commands are registered through the {@link CommandRegisterCallback} event
 * and dispatched through this registry.
 * </p>
 *
 * <h2>Example Usage</h2>
 * 
 * <pre>{@code
 * // Register during the command registration event
 * CommandRegisterCallback.EVENT.register(registry -> {
 *   registry.register(Command.builder("hello")
 *       .executes(ctx -> {
 *         ctx.getSender().sendMessage("Hello, World!");
 *         return CommandResult.SUCCESS;
 *       })
 *       .build());
 * });
 *
 * // Execute a command
 * CommandResult result = registry.dispatch(sender, "hello");
 *
 * // Get suggestions
 * Suggestions suggestions = registry.getSuggestions(sender, "hel").join();
 * }</pre>
 */
public final class CommandRegistry {

  private final Map<String, Command> commands;
  private final Map<String, Command> aliasMap;
  private final ReentrantReadWriteLock lock;

  /**
   * Creates a new empty command registry.
   */
  public CommandRegistry() {
    this.commands = new HashMap<>();
    this.aliasMap = new HashMap<>();
    this.lock = new ReentrantReadWriteLock();
  }

  /**
   * Registers a command.
   *
   * @param command the command to register
   * @throws IllegalArgumentException if a command with the same name or alias is
   *                                  already registered
   */
  public void register(Command command) {
    Objects.requireNonNull(command, "command");

    lock.writeLock().lock();
    try {
      String name = command.getName().toLowerCase();
      if (commands.containsKey(name) || aliasMap.containsKey(name)) {
        throw new IllegalArgumentException("Command already registered: " + name);
      }

      for (String alias : command.getAliases()) {
        String lowerAlias = alias.toLowerCase();
        if (commands.containsKey(lowerAlias) || aliasMap.containsKey(lowerAlias)) {
          throw new IllegalArgumentException("Alias conflicts with existing command: " + alias);
        }
      }

      commands.put(name, command);
      for (String alias : command.getAliases()) {
        aliasMap.put(alias.toLowerCase(), command);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Unregisters a command by name.
   *
   * @param name the command name
   * @return {@code true} if the command was found and removed
   */
  public boolean unregister(String name) {
    Objects.requireNonNull(name, "name");

    lock.writeLock().lock();
    try {
      Command command = commands.remove(name.toLowerCase());
      if (command == null) {
        return false;
      }

      for (String alias : command.getAliases()) {
        aliasMap.remove(alias.toLowerCase());
      }
      return true;
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Gets a command by name or alias.
   *
   * @param name the command name or alias
   * @return an Optional containing the command, or empty if not found
   */
  public Optional<Command> getCommand(String name) {
    Objects.requireNonNull(name, "name");

    lock.readLock().lock();
    try {
      String lower = name.toLowerCase();
      Command command = commands.get(lower);
      if (command == null) {
        command = aliasMap.get(lower);
      }
      return Optional.ofNullable(command);
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Returns all registered commands.
   *
   * @return an unmodifiable collection of commands
   */
  public Collection<Command> getCommands() {
    lock.readLock().lock();
    try {
      return Collections.unmodifiableCollection(new ArrayList<>(commands.values()));
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Returns all registered command names.
   *
   * @return an unmodifiable set of command names
   */
  public Collection<String> getCommandNames() {
    lock.readLock().lock();
    try {
      return Collections.unmodifiableSet(commands.keySet());
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Checks if a command is registered.
   *
   * @param name the command name or alias
   * @return {@code true} if the command exists
   */
  public boolean hasCommand(String name) {
    return getCommand(name).isPresent();
  }

  /**
   * Dispatches a command from the given input.
   * <p>
   * This method fires the {@link CommandExecuteCallback} event before executing
   * the command. If the event is cancelled, the command will not be executed
   * and this method returns {@link CommandResult#FAILURE}.
   * </p>
   *
   * @param sender the command sender
   * @param input  the full command input (with or without leading slash)
   * @return the command result
   * @throws CommandException if parsing or execution fails
   */
  public CommandResult dispatch(CommandSender sender, String input) throws CommandException {
    Objects.requireNonNull(sender, "sender");
    Objects.requireNonNull(input, "input");

    // Strip leading slash if present
    String normalizedInput = input.startsWith("/") ? input.substring(1) : input;
    if (normalizedInput.isEmpty()) {
      throw CommandException.syntax("Empty command");
    }

    // Extract command name
    String[] parts = normalizedInput.split("\\s+", 2);
    String commandName = parts[0].toLowerCase();

    Command command = getCommand(commandName)
        .orElseThrow(() -> CommandException.syntax("Unknown command: " + commandName));

    // Fire the CommandExecuteCallback event
    EventResult eventResult = CommandExecuteCallback.EVENT.invoker()
        .onCommandExecute(sender, command, normalizedInput);

    if (eventResult.isCancelled()) {
      return CommandResult.FAILURE;
    }

    return command.execute(sender, normalizedInput);
  }

  /**
   * Gets autocompletion suggestions for the given input.
   *
   * @param sender the command sender
   * @param input  the partial input
   * @return a future completing with suggestions
   */
  public CompletableFuture<Suggestions> getSuggestions(CommandSender sender, String input) {
    Objects.requireNonNull(sender, "sender");
    Objects.requireNonNull(input, "input");

    // Strip leading slash if present
    String normalizedInput = input.startsWith("/") ? input.substring(1) : input;

    // Check if we're still typing the command name
    String[] parts = normalizedInput.split("\\s+", 2);
    String partial = parts[0].toLowerCase();

    if (parts.length == 1 && !normalizedInput.endsWith(" ")) {
      // Suggest command names
      dev.polv.taleapi.command.suggestion.SuggestionsBuilder builder = new dev.polv.taleapi.command.suggestion.SuggestionsBuilder(
          normalizedInput, 0);

      lock.readLock().lock();
      try {
        for (Command command : commands.values()) {
          if (command.canUse(sender)) {
            if (command.getName().toLowerCase().startsWith(partial)) {
              builder.suggest(command.getName());
            }
            for (String alias : command.getAliases()) {
              if (alias.toLowerCase().startsWith(partial)) {
                builder.suggest(alias);
              }
            }
          }
        }
      } finally {
        lock.readLock().unlock();
      }

      return builder.buildFuture();
    }

    // Delegate to the specific command
    Optional<Command> command = getCommand(partial);
    if (command.isEmpty()) {
      return CompletableFuture.completedFuture(Suggestions.empty());
    }

    return command.get().getSuggestions(sender, normalizedInput);
  }

  /**
   * Returns the number of registered commands.
   *
   * @return the command count
   */
  public int size() {
    lock.readLock().lock();
    try {
      return commands.size();
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Removes all registered commands.
   */
  public void clear() {
    lock.writeLock().lock();
    try {
      commands.clear();
      aliasMap.clear();
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Returns a list of available commands for the sender.
   *
   * @param sender the command sender
   * @return a list of commands the sender can use
   */
  public List<Command> getAvailableCommands(CommandSender sender) {
    lock.readLock().lock();
    try {
      List<Command> available = new ArrayList<>();
      for (Command command : commands.values()) {
        if (command.canUse(sender)) {
          available.add(command);
        }
      }
      return Collections.unmodifiableList(available);
    } finally {
      lock.readLock().unlock();
    }
  }
}
