package dev.polv.taleapi.command;

import dev.polv.taleapi.command.argument.StringArgumentType;
import dev.polv.taleapi.command.suggestion.Suggestions;
import dev.polv.taleapi.testutil.TestPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CommandRegistry")
class CommandRegistryTest {

  private CommandRegistry registry;
  private TestPlayer player;

  @BeforeEach
  void setUp() {
    registry = new CommandRegistry();
    player = new TestPlayer("TestPlayer");
  }

  @Nested
  @DisplayName("Registration")
  class RegistrationTests {

    @Test
    @DisplayName("should register a command")
    void shouldRegisterCommand() {
      Command command = Command.builder("test")
          .executes(ctx -> CommandResult.SUCCESS)
          .build();

      registry.register(command);

      assertTrue(registry.hasCommand("test"));
      assertEquals(1, registry.size());
    }

    @Test
    @DisplayName("should register command with aliases")
    void shouldRegisterWithAliases() {
      Command command = Command.builder("teleport")
          .aliases("tp", "warp")
          .executes(ctx -> CommandResult.SUCCESS)
          .build();

      registry.register(command);

      assertTrue(registry.hasCommand("teleport"));
      assertTrue(registry.hasCommand("tp"));
      assertTrue(registry.hasCommand("warp"));
    }

    @Test
    @DisplayName("should throw when registering duplicate command")
    void shouldThrowOnDuplicateCommand() {
      Command cmd1 = Command.builder("test")
          .executes(ctx -> CommandResult.SUCCESS)
          .build();
      Command cmd2 = Command.builder("test")
          .executes(ctx -> CommandResult.SUCCESS)
          .build();

      registry.register(cmd1);

      assertThrows(IllegalArgumentException.class, () -> registry.register(cmd2));
    }

    @Test
    @DisplayName("should throw when alias conflicts with existing command")
    void shouldThrowOnAliasConflict() {
      Command cmd1 = Command.builder("teleport")
          .executes(ctx -> CommandResult.SUCCESS)
          .build();
      Command cmd2 = Command.builder("warp")
          .aliases("teleport")
          .executes(ctx -> CommandResult.SUCCESS)
          .build();

      registry.register(cmd1);

      assertThrows(IllegalArgumentException.class, () -> registry.register(cmd2));
    }

    @Test
    @DisplayName("should unregister a command")
    void shouldUnregisterCommand() {
      Command command = Command.builder("test")
          .aliases("t")
          .executes(ctx -> CommandResult.SUCCESS)
          .build();

      registry.register(command);
      assertTrue(registry.unregister("test"));

      assertFalse(registry.hasCommand("test"));
      assertFalse(registry.hasCommand("t"));
      assertEquals(0, registry.size());
    }

    @Test
    @DisplayName("should return false when unregistering non-existent command")
    void shouldReturnFalseForNonExistentUnregister() {
      assertFalse(registry.unregister("nonexistent"));
    }
  }

  @Nested
  @DisplayName("Retrieval")
  class RetrievalTests {

    @Test
    @DisplayName("should get command by name")
    void shouldGetByName() {
      Command command = Command.builder("test")
          .executes(ctx -> CommandResult.SUCCESS)
          .build();

      registry.register(command);

      Optional<Command> retrieved = registry.getCommand("test");
      assertTrue(retrieved.isPresent());
      assertEquals("test", retrieved.get().getName());
    }

    @Test
    @DisplayName("should get command by alias")
    void shouldGetByAlias() {
      Command command = Command.builder("teleport")
          .aliases("tp")
          .executes(ctx -> CommandResult.SUCCESS)
          .build();

      registry.register(command);

      Optional<Command> retrieved = registry.getCommand("tp");
      assertTrue(retrieved.isPresent());
      assertEquals("teleport", retrieved.get().getName());
    }

    @Test
    @DisplayName("should be case-insensitive")
    void shouldBeCaseInsensitive() {
      Command command = Command.builder("Test")
          .executes(ctx -> CommandResult.SUCCESS)
          .build();

      registry.register(command);

      assertTrue(registry.hasCommand("TEST"));
      assertTrue(registry.hasCommand("test"));
      assertTrue(registry.hasCommand("Test"));
    }

    @Test
    @DisplayName("should return empty for non-existent command")
    void shouldReturnEmptyForNonExistent() {
      Optional<Command> command = registry.getCommand("nonexistent");
      assertTrue(command.isEmpty());
    }

    @Test
    @DisplayName("should get all commands")
    void shouldGetAllCommands() {
      registry.register(Command.builder("cmd1").executes(ctx -> CommandResult.SUCCESS).build());
      registry.register(Command.builder("cmd2").executes(ctx -> CommandResult.SUCCESS).build());
      registry.register(Command.builder("cmd3").executes(ctx -> CommandResult.SUCCESS).build());

      assertEquals(3, registry.getCommands().size());
      assertEquals(3, registry.getCommandNames().size());
    }

    @Test
    @DisplayName("should get available commands for sender")
    void shouldGetAvailableCommands() {
      registry.register(Command.builder("public")
          .executes(ctx -> CommandResult.SUCCESS)
          .build());
      registry.register(Command.builder("admin")
          .permission("server.admin")
          .executes(ctx -> CommandResult.SUCCESS)
          .build());

      var available = registry.getAvailableCommands(player);

      assertEquals(1, available.size());
      assertEquals("public", available.get(0).getName());
    }
  }

  @Nested
  @DisplayName("Dispatch")
  class DispatchTests {

    @Test
    @DisplayName("should dispatch command by name")
    void shouldDispatchByName() {
      Command command = Command.builder("hello")
          .executes(ctx -> {
            ctx.getSender().sendMessage("Hello!");
            return CommandResult.SUCCESS;
          })
          .build();

      registry.register(command);
      CommandResult result = registry.dispatch(player, "hello");

      assertEquals(CommandResult.SUCCESS, result);
      assertEquals("Hello!", player.getLastMessage());
    }

    @Test
    @DisplayName("should dispatch command by alias")
    void shouldDispatchByAlias() {
      Command command = Command.builder("teleport")
          .aliases("tp")
          .executes(ctx -> {
            ctx.getSender().sendMessage("Teleporting!");
            return CommandResult.SUCCESS;
          })
          .build();

      registry.register(command);
      CommandResult result = registry.dispatch(player, "tp");

      assertEquals(CommandResult.SUCCESS, result);
      assertEquals("Teleporting!", player.getLastMessage());
    }

    @Test
    @DisplayName("should strip leading slash")
    void shouldStripLeadingSlash() {
      Command command = Command.builder("test")
          .executes(ctx -> CommandResult.SUCCESS)
          .build();

      registry.register(command);
      CommandResult result = registry.dispatch(player, "/test");

      assertEquals(CommandResult.SUCCESS, result);
    }

    @Test
    @DisplayName("should throw for unknown command")
    void shouldThrowForUnknownCommand() {
      assertThrows(CommandException.class, () -> registry.dispatch(player, "unknown"));
    }

    @Test
    @DisplayName("should throw for empty input")
    void shouldThrowForEmptyInput() {
      assertThrows(CommandException.class, () -> registry.dispatch(player, ""));
    }

    @Test
    @DisplayName("should dispatch with arguments")
    void shouldDispatchWithArguments() {
      Command command = Command.builder("greet")
          .then(Command.argument("name", StringArgumentType.word())
              .executes(ctx -> {
                String name = ctx.getArgument("name", String.class);
                ctx.getSender().sendMessage("Hello, " + name + "!");
                return CommandResult.SUCCESS;
              }))
          .build();

      registry.register(command);
      registry.dispatch(player, "greet World");

      assertEquals("Hello, World!", player.getLastMessage());
    }
  }

  @Nested
  @DisplayName("Suggestions")
  class SuggestionsTests {

    @Test
    @DisplayName("should suggest command names")
    void shouldSuggestCommandNames() {
      registry.register(Command.builder("gamemode").executes(ctx -> CommandResult.SUCCESS).build());
      registry.register(Command.builder("give").executes(ctx -> CommandResult.SUCCESS).build());
      registry.register(Command.builder("teleport").executes(ctx -> CommandResult.SUCCESS).build());

      Suggestions suggestions = registry.getSuggestions(player, "g").join();

      assertEquals(2, suggestions.getList().size());
      assertTrue(suggestions.getList().stream()
          .allMatch(s -> s.getText().startsWith("g")));
    }

    @Test
    @DisplayName("should suggest command aliases")
    void shouldSuggestAliases() {
      registry.register(Command.builder("teleport")
          .aliases("tp", "warp")
          .executes(ctx -> CommandResult.SUCCESS)
          .build());

      Suggestions suggestions = registry.getSuggestions(player, "t").join();

      assertTrue(suggestions.getList().stream()
          .anyMatch(s -> s.getText().equals("teleport")));
      assertTrue(suggestions.getList().stream()
          .anyMatch(s -> s.getText().equals("tp")));
    }

    @Test
    @DisplayName("should delegate suggestions to command")
    void shouldDelegateSuggestionsToCommand() {
      registry.register(Command.builder("gamemode")
          .then(Command.literal("survival"))
          .then(Command.literal("creative"))
          .build());

      Suggestions suggestions = registry.getSuggestions(player, "gamemode ").join();

      assertEquals(2, suggestions.getList().size());
    }

    @Test
    @DisplayName("should not suggest commands without permission")
    void shouldNotSuggestWithoutPermission() {
      registry.register(Command.builder("public")
          .executes(ctx -> CommandResult.SUCCESS)
          .build());
      registry.register(Command.builder("admin")
          .permission("server.admin")
          .executes(ctx -> CommandResult.SUCCESS)
          .build());

      Suggestions suggestions = registry.getSuggestions(player, "").join();

      assertEquals(1, suggestions.getList().size());
      assertEquals("public", suggestions.getList().get(0).getText());
    }

    @Test
    @DisplayName("should strip leading slash for suggestions")
    void shouldStripSlashForSuggestions() {
      registry.register(Command.builder("test").executes(ctx -> CommandResult.SUCCESS).build());

      Suggestions suggestions = registry.getSuggestions(player, "/te").join();

      assertFalse(suggestions.isEmpty());
      assertEquals("test", suggestions.getList().get(0).getText());
    }
  }

  @Nested
  @DisplayName("Clear")
  class ClearTests {

    @Test
    @DisplayName("should clear all commands")
    void shouldClearAllCommands() {
      registry.register(Command.builder("cmd1").executes(ctx -> CommandResult.SUCCESS).build());
      registry.register(Command.builder("cmd2").executes(ctx -> CommandResult.SUCCESS).build());

      registry.clear();

      assertEquals(0, registry.size());
      assertFalse(registry.hasCommand("cmd1"));
      assertFalse(registry.hasCommand("cmd2"));
    }
  }
}

