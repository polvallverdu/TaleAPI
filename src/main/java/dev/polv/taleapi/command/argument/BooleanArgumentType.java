package dev.polv.taleapi.command.argument;

import dev.polv.taleapi.command.CommandContext;
import dev.polv.taleapi.command.CommandException;
import dev.polv.taleapi.command.suggestion.Suggestions;
import dev.polv.taleapi.command.suggestion.SuggestionsBuilder;

import java.util.concurrent.CompletableFuture;

/**
 * Argument type for boolean values.
 * <p>
 * Accepts "true" or "false" (case-insensitive).
 * </p>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * Command.argument("enabled", BooleanArgumentType.bool())
 * }</pre>
 */
public class BooleanArgumentType implements ArgumentType<Boolean> {

  private static final BooleanArgumentType INSTANCE = new BooleanArgumentType();

  private BooleanArgumentType() {
  }

  /**
   * Returns the boolean argument type instance.
   *
   * @return the boolean argument type
   */
  public static BooleanArgumentType bool() {
    return INSTANCE;
  }

  @Override
  public Boolean parse(StringReader reader) throws CommandException {
    return reader.readBoolean();
  }

  @Override
  public CompletableFuture<Suggestions> listSuggestions(CommandContext context, SuggestionsBuilder builder) {
    String remaining = builder.getRemaining().toLowerCase();
    if ("true".startsWith(remaining)) {
      builder.suggest("true");
    }
    if ("false".startsWith(remaining)) {
      builder.suggest("false");
    }
    return builder.buildFuture();
  }

  @Override
  public Class<Boolean> getResultType() {
    return Boolean.class;
  }

  @Override
  public String getTypeName() {
    return "boolean";
  }

  @Override
  public String[] getExamples() {
    return new String[]{"true", "false"};
  }
}

