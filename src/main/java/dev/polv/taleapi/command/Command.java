package dev.polv.taleapi.command;

import dev.polv.taleapi.command.argument.ArgumentType;
import dev.polv.taleapi.command.argument.StringReader;
import dev.polv.taleapi.command.suggestion.Suggestions;
import dev.polv.taleapi.command.suggestion.SuggestionsBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/**
 * Represents a complete command with its tree structure.
 * <p>
 * Commands are built using a fluent builder API similar to Brigadier.
 * The command tree consists of literal nodes (fixed text) and argument nodes
 * (typed parameters).
 * </p>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * Command gamemode = Command.literal("gamemode")
 *     .requires("server.gamemode")
 *     .then(Command.literal("survival")
 *         .executes(ctx -> {
 *             ctx.getSender().sendMessage("Switching to survival mode");
 *             return CommandResult.SUCCESS;
 *         }))
 *     .then(Command.literal("creative")
 *         .executes(ctx -> {
 *             ctx.getSender().sendMessage("Switching to creative mode");
 *             return CommandResult.SUCCESS;
 *         }))
 *     .then(Command.argument("mode", StringArgumentType.word())
 *         .then(Command.argument("player", StringArgumentType.word())
 *             .executes(ctx -> {
 *                 String mode = ctx.getArgument("mode", String.class);
 *                 String player = ctx.getArgument("player", String.class);
 *                 ctx.getSender().sendMessage("Setting " + player + " to " + mode);
 *                 return CommandResult.SUCCESS;
 *             })))
 *     .build();
 *
 * // Execute the command
 * CommandResult result = command.execute(sender, "gamemode creative");
 *
 * // Get suggestions
 * Suggestions suggestions = command.getSuggestions(sender, "gamemode c").join();
 * }</pre>
 */
public final class Command {

  private final String name;
  private final CommandNode.LiteralNode rootNode;
  private final String description;
  private final List<String> aliases;
  private final String permission;

  private Command(Builder builder) {
    this.name = builder.rootNode.getName();
    this.rootNode = builder.rootNode;
    this.description = builder.description;
    this.aliases = Collections.unmodifiableList(new ArrayList<>(builder.aliases));
    this.permission = builder.permission;
  }

  /**
   * Returns the command name (the root literal).
   *
   * @return the command name
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the root node of the command tree.
   *
   * @return the root node
   */
  public CommandNode.LiteralNode getRootNode() {
    return rootNode;
  }

  /**
   * Returns the command description.
   *
   * @return the description, or null if not set
   */
  public String getDescription() {
    return description;
  }

  /**
   * Returns the command aliases.
   *
   * @return an unmodifiable list of aliases
   */
  public List<String> getAliases() {
    return aliases;
  }

  /**
   * Returns the permission required to use this command.
   *
   * @return the permission, or null if no permission required
   */
  public String getPermission() {
    return permission;
  }

  /**
   * Checks if the sender has permission to use this command.
   *
   * @param sender the command sender
   * @return {@code true} if the sender can use this command
   */
  public boolean canUse(CommandSender sender) {
    if (permission != null && !sender.hasPermission(permission)) {
      return false;
    }
    return rootNode.canUse(sender);
  }

  /**
   * Executes the command with the given input.
   *
   * @param sender the command sender
   * @param input  the command input (without the leading slash)
   * @return the execution result
   * @throws CommandException if parsing or execution fails
   */
  public CommandResult execute(CommandSender sender, String input) throws CommandException {
    if (!canUse(sender)) {
      throw CommandException.permission(permission != null ? permission : "command." + name);
    }

    StringReader reader = new StringReader(input);
    reader.skipWhitespace();

    // Parse and verify the root literal
    String literal = reader.readUnquotedString();
    if (!rootNode.matches(literal) && !aliases.stream().anyMatch(a -> a.equalsIgnoreCase(literal))) {
      throw CommandException.syntax("Unknown command: " + literal);
    }

    // Parse the rest of the command
    Map<String, Object> arguments = new HashMap<>();
    CommandNode<?> currentNode = rootNode;
    CommandExecutor executor = rootNode.getExecutor();

    while (reader.canRead()) {
      reader.skipWhitespace();
      if (!reader.canRead()) {
        break;
      }

      CommandNode<?> matchedChild = null;
      int startPos = reader.getCursor();

      // Try to match a child node
      for (CommandNode<?> child : currentNode.getChildren()) {
        if (!child.canUse(sender)) {
          continue;
        }

        reader.setCursor(startPos);

        try {
          if (child instanceof CommandNode.LiteralNode literalChild) {
            String word = reader.readUnquotedString();
            if (literalChild.matches(word)) {
              matchedChild = child;
              break;
            }
          } else if (child instanceof CommandNode.ArgumentNode<?> argChild) {
            Object value = argChild.getType().parse(reader);
            arguments.put(argChild.getName(), value);
            matchedChild = child;
            break;
          }
        } catch (CommandException e) {
          // Try next child
        }
      }

      if (matchedChild == null) {
        // No matching child found
        String remaining = reader.getRemaining().trim();
        if (!remaining.isEmpty()) {
          throw CommandException.syntax("Unknown argument: " + remaining.split("\\s+")[0]);
        }
        break;
      }

      currentNode = matchedChild;
      if (currentNode.getExecutor() != null) {
        executor = currentNode.getExecutor();
      }
    }

    if (executor == null) {
      throw CommandException.syntax("Incomplete command");
    }

    // Build context and execute
    CommandContext context = CommandContext.builder(sender, input)
        .withArguments(arguments)
        .withCommand(this)
        .build();

    return executor.execute(context);
  }

  /**
   * Gets autocompletion suggestions for the given input.
   *
   * @param sender the command sender
   * @param input  the partial input
   * @return a future completing with suggestions
   */
  public CompletableFuture<Suggestions> getSuggestions(CommandSender sender, String input) {
    if (!canUse(sender)) {
      return CompletableFuture.completedFuture(Suggestions.empty());
    }

    StringReader reader = new StringReader(input);
    reader.skipWhitespace();

    // Check if we're still on the command name
    int startPos = reader.getCursor();
    String literal = reader.readUnquotedString();

    if (!reader.canRead() || (reader.canRead() && !Character.isWhitespace(reader.peek()))) {
      // Still completing the command name
      SuggestionsBuilder builder = new SuggestionsBuilder(input, startPos);
      if (name.toLowerCase().startsWith(literal.toLowerCase())) {
        builder.suggest(name);
      }
      for (String alias : aliases) {
        if (alias.toLowerCase().startsWith(literal.toLowerCase())) {
          builder.suggest(alias);
        }
      }
      return builder.buildFuture();
    }

    // Verify command name matches
    if (!rootNode.matches(literal) && !aliases.stream().anyMatch(a -> a.equalsIgnoreCase(literal))) {
      return CompletableFuture.completedFuture(Suggestions.empty());
    }

    // Parse and find current position
    Map<String, Object> arguments = new HashMap<>();
    CommandNode<?> currentNode = rootNode;

    while (reader.canRead()) {
      reader.skipWhitespace();
      if (!reader.canRead()) {
        break;
      }

      int argStart = reader.getCursor();
      CommandNode<?> matchedChild = null;

      for (CommandNode<?> child : currentNode.getChildren()) {
        if (!child.canUse(sender)) {
          continue;
        }

        reader.setCursor(argStart);

        try {
          if (child instanceof CommandNode.LiteralNode literalChild) {
            String word = reader.readUnquotedString();
            if (literalChild.matches(word) && reader.canRead() && Character.isWhitespace(reader.peek())) {
              matchedChild = child;
              break;
            }
          } else if (child instanceof CommandNode.ArgumentNode<?> argChild) {
            Object value = argChild.getType().parse(reader);
            if (reader.canRead() && Character.isWhitespace(reader.peek())) {
              arguments.put(argChild.getName(), value);
              matchedChild = child;
              break;
            }
          }
        } catch (CommandException e) {
          // Try next child
        }
      }

      if (matchedChild == null) {
        // We're at the end, provide suggestions from current node's children
        reader.setCursor(argStart);
        break;
      }

      currentNode = matchedChild;
    }

    // Get suggestions from current node's children
    CommandContext context = CommandContext.builder(sender, input)
        .withArguments(arguments)
        .withCommand(this)
        .build();

    reader.skipWhitespace();
    int suggestionStart = reader.getCursor();
    SuggestionsBuilder builder = new SuggestionsBuilder(input, suggestionStart);

    List<CompletableFuture<Suggestions>> futures = new ArrayList<>();
    for (CommandNode<?> child : currentNode.getChildren()) {
      if (child.canUse(sender)) {
        futures.add(child.listSuggestions(context, new SuggestionsBuilder(input, suggestionStart)));
      }
    }

    if (futures.isEmpty()) {
      return builder.buildFuture();
    }

    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .thenApply(v -> {
          List<Suggestions> results = new ArrayList<>();
          for (CompletableFuture<Suggestions> future : futures) {
            results.add(future.join());
          }
          return Suggestions.merge(input, results);
        });
  }

  /**
   * Returns the usage string for this command.
   *
   * @return the usage string
   */
  public String getUsage() {
    return "/" + name + " " + buildUsage(rootNode);
  }

  private String buildUsage(CommandNode<?> node) {
    StringBuilder sb = new StringBuilder();
    List<CommandNode<?>> children = node.getChildren();

    if (children.isEmpty()) {
      return "";
    }

    if (children.size() == 1) {
      CommandNode<?> child = children.get(0);
      sb.append(child.getUsageText());
      String childUsage = buildUsage(child);
      if (!childUsage.isEmpty()) {
        sb.append(" ").append(childUsage);
      }
    } else {
      // Multiple children - show as alternatives
      List<String> options = new ArrayList<>();
      for (CommandNode<?> child : children) {
        String usage = child.getUsageText();
        String childUsage = buildUsage(child);
        if (!childUsage.isEmpty()) {
          usage += " " + childUsage;
        }
        options.add(usage);
      }
      sb.append("(").append(String.join("|", options)).append(")");
    }

    return sb.toString().trim();
  }

  /**
   * Creates a literal node for building commands.
   *
   * @param name the literal text
   * @return a new literal node
   */
  public static CommandNode.LiteralNode literal(String name) {
    return new CommandNode.LiteralNode(name);
  }

  /**
   * Creates an argument node for building commands.
   *
   * @param name the argument name
   * @param type the argument type
   * @param <T>  the argument's parsed type
   * @return a new argument node
   */
  public static <T> CommandNode.ArgumentNode<T> argument(String name, ArgumentType<T> type) {
    return new CommandNode.ArgumentNode<>(name, type);
  }

  /**
   * Creates a builder for a command with the given name.
   *
   * @param name the command name (the root literal)
   * @return a new builder
   */
  public static Builder builder(String name) {
    return new Builder(name);
  }

  /**
   * Builder for constructing {@link Command} instances.
   */
  public static final class Builder {
    private final CommandNode.LiteralNode rootNode;
    private String description;
    private final List<String> aliases = new ArrayList<>();
    private String permission;

    private Builder(String name) {
      this.rootNode = new CommandNode.LiteralNode(name);
    }

    /**
     * Sets a description for this command.
     *
     * @param description the description
     * @return this builder
     */
    public Builder description(String description) {
      this.description = description;
      return this;
    }

    /**
     * Adds aliases for this command.
     *
     * @param aliases the aliases
     * @return this builder
     */
    public Builder aliases(String... aliases) {
      Collections.addAll(this.aliases, aliases);
      return this;
    }

    /**
     * Sets the permission required to use this command.
     *
     * @param permission the permission
     * @return this builder
     */
    public Builder permission(String permission) {
      this.permission = permission;
      this.rootNode.requires(permission);
      return this;
    }

    /**
     * Sets a custom requirement predicate for this command.
     *
     * @param requirement the requirement
     * @return this builder
     */
    public Builder requires(Predicate<CommandSender> requirement) {
      this.rootNode.requires(requirement);
      return this;
    }

    /**
     * Adds a child node to the root of this command.
     *
     * @param child the child node
     * @return this builder
     */
    public Builder then(CommandNode<?> child) {
      this.rootNode.then(child);
      return this;
    }

    /**
     * Sets the executor for the root node.
     *
     * @param executor the executor
     * @return this builder
     */
    public Builder executes(CommandExecutor executor) {
      this.rootNode.executes(executor);
      return this;
    }

    /**
     * Builds the command.
     *
     * @return the constructed command
     */
    public Command build() {
      return new Command(this);
    }
  }

  @Override
  public String toString() {
    return "Command{name='" + name + "', aliases=" + aliases + '}';
  }
}

