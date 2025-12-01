package dev.polv.taleapi.command.suggestion;

import dev.polv.taleapi.command.CommandContext;

import java.util.concurrent.CompletableFuture;

/**
 * Functional interface for providing custom suggestions for an argument.
 * <p>
 * Implement this to provide dynamic suggestions based on the command context.
 * </p>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Custom player name suggestions
 * SuggestionProvider playerSuggestions = (context, builder) -> {
 *     for (TalePlayer player : getOnlinePlayers()) {
 *         builder.suggest(player.getDisplayName());
 *     }
 *     return builder.buildFuture();
 * };
 *
 * Command.literal("tp")
 *     .then(Command.argument("player", StringArgumentType.word())
 *         .suggests(playerSuggestions))
 *     .build();
 * }</pre>
 */
@FunctionalInterface
public interface SuggestionProvider {

  /**
   * Provides suggestions for the argument.
   *
   * @param context the current command context
   * @param builder the suggestions builder
   * @return a future that completes with suggestions
   */
  CompletableFuture<Suggestions> getSuggestions(CommandContext context, SuggestionsBuilder builder);

  /**
   * Returns a suggestion provider that always returns empty suggestions.
   *
   * @return an empty suggestion provider
   */
  static SuggestionProvider empty() {
    return (context, builder) -> builder.buildFuture();
  }

  /**
   * Returns a suggestion provider from a list of static suggestions.
   *
   * @param suggestions the suggestions to provide
   * @return a static suggestion provider
   */
  static SuggestionProvider of(String... suggestions) {
    return (context, builder) -> {
      for (String suggestion : suggestions) {
        builder.suggest(suggestion);
      }
      return builder.buildFuture();
    };
  }

  /**
   * Returns a suggestion provider from a dynamic collection.
   *
   * @param suggestions supplier for suggestions
   * @return a dynamic suggestion provider
   */
  static SuggestionProvider of(Iterable<String> suggestions) {
    return (context, builder) -> {
      for (String suggestion : suggestions) {
        builder.suggest(suggestion);
      }
      return builder.buildFuture();
    };
  }
}

