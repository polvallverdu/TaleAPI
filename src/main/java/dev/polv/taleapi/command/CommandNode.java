package dev.polv.taleapi.command;

import dev.polv.taleapi.command.argument.ArgumentType;
import dev.polv.taleapi.command.suggestion.SuggestionProvider;
import dev.polv.taleapi.command.suggestion.Suggestions;
import dev.polv.taleapi.command.suggestion.SuggestionsBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/**
 * Base class for command tree nodes.
 * <p>
 * A command is structured as a tree of nodes, where each node can be:
 * </p>
 * <ul>
 *   <li>{@link LiteralNode} - A fixed text literal (like "gamemode", "give")</li>
 *   <li>{@link ArgumentNode} - A typed argument that parses input</li>
 * </ul>
 *
 * <h2>Example Tree Structure</h2>
 * <pre>{@code
 * gamemode (literal)
 * ├── survival (literal) -> executes
 * ├── creative (literal) -> executes
 * └── <mode> (argument: string)
 *     └── <player> (argument: player) -> executes
 * }</pre>
 *
 * @param <T> the self type for builder chaining
 */
public abstract class CommandNode<T extends CommandNode<T>> {

  /** The name of this command node. */
  protected final String name;

  /** The child nodes of this command node. */
  protected final List<CommandNode<?>> children;

  /** The executor that handles command execution for this node. */
  protected CommandExecutor executor;

  /** The requirement predicate that must be satisfied for this command to be available. */
  protected Predicate<CommandSender> requirement;

  /**
   * Creates a new command node with the given name.
   *
   * @param name the name of this command node
   */
  protected CommandNode(String name) {
    this.name = Objects.requireNonNull(name, "name");
    this.children = new ArrayList<>();
    this.requirement = sender -> true;
  }

  /**
   * Returns the name of this node.
   *
   * @return the node name
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the child nodes.
   *
   * @return an unmodifiable list of children
   */
  public List<CommandNode<?>> getChildren() {
    return Collections.unmodifiableList(children);
  }

  /**
   * Returns the executor for this node, if any.
   *
   * @return the executor, or null
   */
  public CommandExecutor getExecutor() {
    return executor;
  }

  /**
   * Returns the requirement predicate for this node.
   *
   * @return the requirement
   */
  public Predicate<CommandSender> getRequirement() {
    return requirement;
  }

  /**
   * Checks if the sender can use this node.
   *
   * @param sender the command sender
   * @return {@code true} if the requirement is met
   */
  public boolean canUse(CommandSender sender) {
    return requirement.test(sender);
  }

  /**
   * Adds a child node to this node.
   *
   * @param child the child to add
   * @return this node for chaining
   */
  @SuppressWarnings("unchecked")
  public T then(CommandNode<?> child) {
    children.add(Objects.requireNonNull(child, "child"));
    return (T) this;
  }

  /**
   * Sets the executor for this node.
   *
   * @param executor the executor
   * @return this node for chaining
   */
  @SuppressWarnings("unchecked")
  public T executes(CommandExecutor executor) {
    this.executor = Objects.requireNonNull(executor, "executor");
    return (T) this;
  }

  /**
   * Sets a requirement for this node based on a permission.
   *
   * @param permission the required permission
   * @return this node for chaining
   */
  @SuppressWarnings("unchecked")
  public T requires(String permission) {
    this.requirement = sender -> sender.hasPermission(permission);
    return (T) this;
  }

  /**
   * Sets a custom requirement predicate for this node.
   *
   * @param requirement the requirement predicate
   * @return this node for chaining
   */
  @SuppressWarnings("unchecked")
  public T requires(Predicate<CommandSender> requirement) {
    this.requirement = Objects.requireNonNull(requirement, "requirement");
    return (T) this;
  }

  /**
   * Checks if this node can execute a command (has an executor).
   *
   * @return {@code true} if this node has an executor
   */
  public boolean isExecutable() {
    return executor != null;
  }

  /**
   * Returns the usage string for this node.
   *
   * @return the usage representation
   */
  public abstract String getUsageText();

  /**
   * Provides suggestions for autocompletion.
   *
   * @param context the command context
   * @param builder the suggestions builder
   * @return a future with suggestions
   */
  public abstract CompletableFuture<Suggestions> listSuggestions(CommandContext context, SuggestionsBuilder builder);

  /**
   * Creates a literal command node.
   *
   * @param name the literal text
   * @return a new literal node
   */
  public static LiteralNode literal(String name) {
    return new LiteralNode(name);
  }

  /**
   * Creates an argument command node.
   *
   * @param name the argument name
   * @param type the argument type
   * @param <T>  the argument's result type
   * @return a new argument node
   */
  public static <T> ArgumentNode<T> argument(String name, ArgumentType<T> type) {
    return new ArgumentNode<>(name, type);
  }

  /**
   * A literal command node representing fixed text.
   * <p>
   * Literals match exact text (case-insensitive) and are used for
   * command names and subcommands.
   * </p>
   */
  public static class LiteralNode extends CommandNode<LiteralNode> {

    /**
     * Creates a new literal node with the given name.
     *
     * @param name the literal text this node matches
     */
    public LiteralNode(String name) {
      super(name);
    }

    @Override
    public String getUsageText() {
      return name;
    }

    @Override
    public CompletableFuture<Suggestions> listSuggestions(CommandContext context, SuggestionsBuilder builder) {
      String remaining = builder.getRemaining().toLowerCase();
      if (name.toLowerCase().startsWith(remaining)) {
        builder.suggest(name);
      }
      return builder.buildFuture();
    }

    /**
     * Checks if the given input matches this literal.
     *
     * @param input the input to check
     * @return {@code true} if the input matches
     */
    public boolean matches(String input) {
      return name.equalsIgnoreCase(input);
    }

    @Override
    public String toString() {
      return "LiteralNode{name='" + name + "'}";
    }
  }

  /**
   * An argument command node that parses typed input.
   * <p>
   * Arguments have a name used to retrieve parsed values and a type
   * that defines how to parse input.
   * </p>
   *
   * @param <V> the parsed value type
   */
  public static class ArgumentNode<V> extends CommandNode<ArgumentNode<V>> {

    /** The argument type that defines how to parse this argument. */
    private final ArgumentType<V> type;

    /** The suggestion provider for this argument, or null for default suggestions. */
    private SuggestionProvider suggestionProvider;

    /**
     * Creates a new argument node with the given name and type.
     *
     * @param name the name of this argument node
     * @param type the argument type for parsing
     */
    public ArgumentNode(String name, ArgumentType<V> type) {
      super(name);
      this.type = Objects.requireNonNull(type, "type");
    }

    /**
     * Returns the argument type.
     *
     * @return the type
     */
    public ArgumentType<V> getType() {
      return type;
    }

    /**
     * Returns the custom suggestion provider, if any.
     *
     * @return the suggestion provider, or null
     */
    public SuggestionProvider getSuggestionProvider() {
      return suggestionProvider;
    }

    /**
     * Sets a custom suggestion provider for this argument.
     *
     * @param provider the suggestion provider
     * @return this node for chaining
     */
    public ArgumentNode<V> suggests(SuggestionProvider provider) {
      this.suggestionProvider = provider;
      return this;
    }

    @Override
    public String getUsageText() {
      return "<" + name + ">";
    }

    @Override
    public CompletableFuture<Suggestions> listSuggestions(CommandContext context, SuggestionsBuilder builder) {
      if (suggestionProvider != null) {
        return suggestionProvider.getSuggestions(context, builder);
      }
      return type.listSuggestions(context, builder);
    }

    @Override
    public String toString() {
      return "ArgumentNode{name='" + name + "', type=" + type.getTypeName() + "}";
    }
  }
}

