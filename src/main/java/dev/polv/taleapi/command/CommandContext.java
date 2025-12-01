package dev.polv.taleapi.command;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Contains the context for a command execution.
 * <p>
 * This includes the command sender, the raw input, and all parsed arguments.
 * Arguments are accessed by their registered names and are type-safe.
 * </p>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * Command.literal("teleport")
 *     .then(Command.argument("player", StringArgumentType.word()))
 *     .then(Command.argument("x", IntegerArgumentType.integer()))
 *     .then(Command.argument("y", IntegerArgumentType.integer()))
 *     .then(Command.argument("z", IntegerArgumentType.integer()))
 *     .executes(ctx -> {
 *         String playerName = ctx.getArgument("player", String.class);
 *         int x = ctx.getArgument("x", Integer.class);
 *         int y = ctx.getArgument("y", Integer.class);
 *         int z = ctx.getArgument("z", Integer.class);
 *         ctx.getSender().sendMessage("Teleporting " + playerName + " to " + x + ", " + y + ", " + z);
 *         return CommandResult.SUCCESS;
 *     })
 *     .build();
 * }</pre>
 */
public final class CommandContext {

  private final CommandSender sender;
  private final String rawInput;
  private final Map<String, Object> arguments;
  private final Command command;

  private CommandContext(Builder builder) {
    this.sender = Objects.requireNonNull(builder.sender, "sender");
    this.rawInput = Objects.requireNonNull(builder.rawInput, "rawInput");
    this.arguments = Collections.unmodifiableMap(new HashMap<>(builder.arguments));
    this.command = builder.command;
  }

  /**
   * Returns the command sender who executed this command.
   *
   * @return the command sender
   */
  public CommandSender getSender() {
    return sender;
  }

  /**
   * Returns the raw input string that was parsed.
   *
   * @return the raw command input
   */
  public String getRawInput() {
    return rawInput;
  }

  /**
   * Returns the command that is being executed.
   *
   * @return the command, or {@code null} if not set
   */
  public Command getCommand() {
    return command;
  }

  /**
   * Gets a parsed argument by name.
   *
   * @param name  the argument name as registered in the command
   * @param clazz the expected type of the argument
   * @param <T>   the argument type
   * @return the parsed argument value
   * @throws IllegalArgumentException if no argument exists with that name
   * @throws ClassCastException       if the argument is not of the expected type
   */
  public <T> T getArgument(String name, Class<T> clazz) {
    Object value = arguments.get(name);
    if (value == null && !arguments.containsKey(name)) {
      throw new IllegalArgumentException("No argument named '" + name + "' found in context");
    }
    return clazz.cast(value);
  }

  /**
   * Gets a parsed argument by name, returning an Optional.
   *
   * @param name  the argument name as registered in the command
   * @param clazz the expected type of the argument
   * @param <T>   the argument type
   * @return an Optional containing the argument value, or empty if not present
   */
  public <T> Optional<T> getOptionalArgument(String name, Class<T> clazz) {
    Object value = arguments.get(name);
    if (value == null) {
      return Optional.empty();
    }
    return Optional.of(clazz.cast(value));
  }

  /**
   * Checks if an argument with the given name exists.
   *
   * @param name the argument name
   * @return {@code true} if the argument exists
   */
  public boolean hasArgument(String name) {
    return arguments.containsKey(name);
  }

  /**
   * Returns all parsed arguments as an unmodifiable map.
   *
   * @return the arguments map
   */
  public Map<String, Object> getArguments() {
    return arguments;
  }

  /**
   * Creates a new builder for constructing a CommandContext.
   *
   * @param sender   the command sender
   * @param rawInput the raw command input
   * @return a new builder instance
   */
  public static Builder builder(CommandSender sender, String rawInput) {
    return new Builder(sender, rawInput);
  }

  /**
   * Builder for constructing {@link CommandContext} instances.
   */
  public static final class Builder {
    private final CommandSender sender;
    private final String rawInput;
    private final Map<String, Object> arguments = new HashMap<>();
    private Command command;

    private Builder(CommandSender sender, String rawInput) {
      this.sender = sender;
      this.rawInput = rawInput;
    }

    /**
     * Adds a parsed argument to the context.
     *
     * @param name  the argument name
     * @param value the parsed value
     * @return this builder
     */
    public Builder withArgument(String name, Object value) {
      arguments.put(name, value);
      return this;
    }

    /**
     * Adds all arguments from a map to the context.
     *
     * @param args the arguments to add
     * @return this builder
     */
    public Builder withArguments(Map<String, Object> args) {
      arguments.putAll(args);
      return this;
    }

    /**
     * Sets the command being executed.
     *
     * @param command the command
     * @return this builder
     */
    public Builder withCommand(Command command) {
      this.command = command;
      return this;
    }

    /**
     * Builds the CommandContext.
     *
     * @return the constructed CommandContext
     */
    public CommandContext build() {
      return new CommandContext(this);
    }
  }
}

