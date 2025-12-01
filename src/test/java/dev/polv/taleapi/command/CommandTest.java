package dev.polv.taleapi.command;

import dev.polv.taleapi.command.argument.BooleanArgumentType;
import dev.polv.taleapi.command.argument.DoubleArgumentType;
import dev.polv.taleapi.command.argument.IntegerArgumentType;
import dev.polv.taleapi.command.argument.StringArgumentType;
import dev.polv.taleapi.command.suggestion.Suggestions;
import dev.polv.taleapi.testutil.TestCommandSender;
import dev.polv.taleapi.testutil.TestPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Command")
class CommandTest {

  private TestPlayer player;
  private TestCommandSender console;

  @BeforeEach
  void setUp() {
    player = new TestPlayer("TestPlayer");
    console = new TestCommandSender();
  }

  @Nested
  @DisplayName("Builder")
  class BuilderTests {

    @Test
    @DisplayName("should create a simple command")
    void shouldCreateSimpleCommand() {
      Command command = Command.builder("test")
          .description("A test command")
          .executes(ctx -> CommandResult.SUCCESS)
          .build();

      assertEquals("test", command.getName());
      assertEquals("A test command", command.getDescription());
      assertNotNull(command.getRootNode());
    }

    @Test
    @DisplayName("should create command with aliases")
    void shouldCreateCommandWithAliases() {
      Command command = Command.builder("teleport")
          .aliases("tp", "warp")
          .executes(ctx -> CommandResult.SUCCESS)
          .build();

      assertEquals("teleport", command.getName());
      assertEquals(2, command.getAliases().size());
      assertTrue(command.getAliases().contains("tp"));
      assertTrue(command.getAliases().contains("warp"));
    }

    @Test
    @DisplayName("should create command with permission")
    void shouldCreateCommandWithPermission() {
      Command command = Command.builder("admin")
          .permission("server.admin")
          .executes(ctx -> CommandResult.SUCCESS)
          .build();

      assertEquals("server.admin", command.getPermission());
    }

    @Test
    @DisplayName("should create command with subcommands")
    void shouldCreateCommandWithSubcommands() {
      Command command = Command.builder("gamemode")
          .then(Command.literal("survival"))
          .then(Command.literal("creative"))
          .then(Command.literal("adventure"))
          .build();

      assertEquals(3, command.getRootNode().getChildren().size());
    }

    @Test
    @DisplayName("should create command with arguments")
    void shouldCreateCommandWithArguments() {
      Command command = Command.builder("give")
          .then(Command.argument("player", StringArgumentType.word())
              .then(Command.argument("item", StringArgumentType.word())
                  .then(Command.argument("amount", IntegerArgumentType.integer(1, 64)))))
          .build();

      assertFalse(command.getRootNode().getChildren().isEmpty());
    }
  }

  @Nested
  @DisplayName("Execution")
  class ExecutionTests {

    @Test
    @DisplayName("should execute simple command")
    void shouldExecuteSimpleCommand() {
      Command command = Command.builder("hello")
          .executes(ctx -> {
            ctx.getSender().sendMessage("Hello!");
            return CommandResult.SUCCESS;
          })
          .build();

      CommandResult result = command.execute(player, "hello");

      assertEquals(CommandResult.SUCCESS, result);
      assertEquals("Hello!", player.getLastMessage());
    }

    @Test
    @DisplayName("should execute command with literal subcommand")
    void shouldExecuteWithLiteralSubcommand() {
      Command command = Command.builder("gamemode")
          .then(Command.literal("survival")
              .executes(ctx -> {
                ctx.getSender().sendMessage("Survival mode");
                return CommandResult.SUCCESS;
              }))
          .then(Command.literal("creative")
              .executes(ctx -> {
                ctx.getSender().sendMessage("Creative mode");
                return CommandResult.SUCCESS;
              }))
          .build();

      command.execute(player, "gamemode survival");
      assertEquals("Survival mode", player.getLastMessage());

      command.execute(player, "gamemode creative");
      assertEquals("Creative mode", player.getLastMessage());
    }

    @Test
    @DisplayName("should execute command with arguments")
    void shouldExecuteWithArguments() {
      AtomicReference<String> capturedName = new AtomicReference<>();
      AtomicReference<Integer> capturedAmount = new AtomicReference<>();

      Command command = Command.builder("give")
          .then(Command.argument("player", StringArgumentType.word())
              .then(Command.argument("amount", IntegerArgumentType.integer())
                  .executes(ctx -> {
                    capturedName.set(ctx.getArgument("player", String.class));
                    capturedAmount.set(ctx.getArgument("amount", Integer.class));
                    return CommandResult.SUCCESS;
                  })))
          .build();

      command.execute(player, "give Steve 64");

      assertEquals("Steve", capturedName.get());
      assertEquals(64, capturedAmount.get());
    }

    @Test
    @DisplayName("should pass context with sender")
    void shouldPassContextWithSender() {
      AtomicReference<CommandSender> capturedSender = new AtomicReference<>();

      Command command = Command.builder("test")
          .executes(ctx -> {
            capturedSender.set(ctx.getSender());
            return CommandResult.SUCCESS;
          })
          .build();

      command.execute(player, "test");

      assertSame(player, capturedSender.get());
    }

    @Test
    @DisplayName("should throw on incomplete command")
    void shouldThrowOnIncompleteCommand() {
      Command command = Command.builder("give")
          .then(Command.argument("player", StringArgumentType.word())
              .executes(ctx -> CommandResult.SUCCESS))
          .build();

      assertThrows(CommandException.class, () -> command.execute(player, "give"));
    }

    @Test
    @DisplayName("should execute with multiple argument types")
    void shouldExecuteWithMultipleArgumentTypes() {
      AtomicReference<String> name = new AtomicReference<>();
      AtomicReference<Integer> count = new AtomicReference<>();
      AtomicReference<Double> multiplier = new AtomicReference<>();
      AtomicReference<Boolean> enabled = new AtomicReference<>();

      Command command = Command.builder("config")
          .then(Command.argument("name", StringArgumentType.word())
              .then(Command.argument("count", IntegerArgumentType.integer())
                  .then(Command.argument("multiplier", DoubleArgumentType.doubleArg())
                      .then(Command.argument("enabled", BooleanArgumentType.bool())
                          .executes(ctx -> {
                            name.set(ctx.getArgument("name", String.class));
                            count.set(ctx.getArgument("count", Integer.class));
                            multiplier.set(ctx.getArgument("multiplier", Double.class));
                            enabled.set(ctx.getArgument("enabled", Boolean.class));
                            return CommandResult.SUCCESS;
                          })))))
          .build();

      command.execute(player, "config test 42 3.14 true");

      assertEquals("test", name.get());
      assertEquals(42, count.get());
      assertEquals(3.14, multiplier.get(), 0.001);
      assertTrue(enabled.get());
    }

    @Test
    @DisplayName("should support greedy string argument")
    void shouldSupportGreedyStringArgument() {
      AtomicReference<String> message = new AtomicReference<>();

      Command command = Command.builder("say")
          .then(Command.argument("message", StringArgumentType.greedyString())
              .executes(ctx -> {
                message.set(ctx.getArgument("message", String.class));
                return CommandResult.SUCCESS;
              }))
          .build();

      command.execute(player, "say Hello world! This is a test.");

      assertEquals("Hello world! This is a test.", message.get());
    }
  }

  @Nested
  @DisplayName("Permission")
  class PermissionTests {

    @Test
    @DisplayName("should allow command when player has permission")
    void shouldAllowWithPermission() {
      Command command = Command.builder("admin")
          .permission("server.admin")
          .executes(ctx -> CommandResult.SUCCESS)
          .build();

      player.addPermission("server.admin");
      CommandResult result = command.execute(player, "admin");

      assertEquals(CommandResult.SUCCESS, result);
    }

    @Test
    @DisplayName("should deny command when player lacks permission")
    void shouldDenyWithoutPermission() {
      Command command = Command.builder("admin")
          .permission("server.admin")
          .executes(ctx -> CommandResult.SUCCESS)
          .build();

      assertThrows(CommandException.class, () -> command.execute(player, "admin"));
    }

    @Test
    @DisplayName("should allow command with wildcard permission")
    void shouldAllowWithWildcardPermission() {
      Command command = Command.builder("admin")
          .permission("server.admin.kick")
          .executes(ctx -> CommandResult.SUCCESS)
          .build();

      player.addPermission("server.admin.*");
      CommandResult result = command.execute(player, "admin");

      assertEquals(CommandResult.SUCCESS, result);
    }

    @Test
    @DisplayName("should allow operators to bypass permissions")
    void shouldAllowOperatorsBypassPermissions() {
      Command command = Command.builder("admin")
          .permission("server.admin")
          .executes(ctx -> CommandResult.SUCCESS)
          .build();

      player.setOp(true);
      CommandResult result = command.execute(player, "admin");

      assertEquals(CommandResult.SUCCESS, result);
    }

    @Test
    @DisplayName("should allow console to execute commands")
    void shouldAllowConsole() {
      Command command = Command.builder("admin")
          .permission("server.admin")
          .executes(ctx -> CommandResult.SUCCESS)
          .build();

      CommandResult result = command.execute(console, "admin");

      assertEquals(CommandResult.SUCCESS, result);
    }

    @Test
    @DisplayName("should support subcommand-level permissions")
    void shouldSupportSubcommandPermissions() {
      Command command = Command.builder("server")
          .then(Command.literal("reload")
              .requires("server.reload")
              .executes(ctx -> CommandResult.SUCCESS))
          .then(Command.literal("stop")
              .requires("server.stop")
              .executes(ctx -> CommandResult.SUCCESS))
          .build();

      player.addPermission("server.reload");

      // Should succeed
      CommandResult result = command.execute(player, "server reload");
      assertEquals(CommandResult.SUCCESS, result);

      // Should fail - no permission for stop
      assertThrows(CommandException.class, () -> command.execute(player, "server stop"));
    }
  }

  @Nested
  @DisplayName("Suggestions")
  class SuggestionTests {

    @Test
    @DisplayName("should suggest command name")
    void shouldSuggestCommandName() {
      Command command = Command.builder("gamemode")
          .executes(ctx -> CommandResult.SUCCESS)
          .build();

      Suggestions suggestions = command.getSuggestions(player, "game").join();

      assertFalse(suggestions.isEmpty());
      assertTrue(suggestions.getList().stream()
          .anyMatch(s -> s.getText().equals("gamemode")));
    }

    @Test
    @DisplayName("should suggest literal subcommands")
    void shouldSuggestLiteralSubcommands() {
      Command command = Command.builder("gamemode")
          .then(Command.literal("survival"))
          .then(Command.literal("creative"))
          .then(Command.literal("adventure"))
          .build();

      Suggestions suggestions = command.getSuggestions(player, "gamemode ").join();

      assertEquals(3, suggestions.getList().size());
    }

    @Test
    @DisplayName("should filter suggestions by prefix")
    void shouldFilterSuggestionsByPrefix() {
      Command command = Command.builder("gamemode")
          .then(Command.literal("survival"))
          .then(Command.literal("creative"))
          .then(Command.literal("spectator"))
          .build();

      Suggestions suggestions = command.getSuggestions(player, "gamemode s").join();

      assertEquals(2, suggestions.getList().size());
      assertTrue(suggestions.getList().stream()
          .allMatch(s -> s.getText().toLowerCase().startsWith("s")));
    }

    @Test
    @DisplayName("should suggest boolean values")
    void shouldSuggestBooleanValues() {
      Command command = Command.builder("toggle")
          .then(Command.argument("enabled", BooleanArgumentType.bool()))
          .build();

      Suggestions suggestions = command.getSuggestions(player, "toggle ").join();

      assertEquals(2, suggestions.getList().size());
      assertTrue(suggestions.getList().stream()
          .anyMatch(s -> s.getText().equals("true")));
      assertTrue(suggestions.getList().stream()
          .anyMatch(s -> s.getText().equals("false")));
    }

    @Test
    @DisplayName("should not suggest commands without permission")
    void shouldNotSuggestWithoutPermission() {
      Command command = Command.builder("admin")
          .permission("server.admin")
          .then(Command.literal("kick"))
          .build();

      Suggestions suggestions = command.getSuggestions(player, "admin ").join();

      assertTrue(suggestions.isEmpty());
    }
  }

  @Nested
  @DisplayName("Error Handling")
  class ErrorHandlingTests {

    @Test
    @DisplayName("should throw syntax error for unknown command")
    void shouldThrowForUnknownCommand() {
      Command command = Command.builder("test")
          .executes(ctx -> CommandResult.SUCCESS)
          .build();

      CommandException ex = assertThrows(CommandException.class,
          () -> command.execute(player, "unknown"));

      assertEquals(CommandException.Type.SYNTAX, ex.getType());
    }

    @Test
    @DisplayName("should throw argument error for invalid integer")
    void shouldThrowForInvalidInteger() {
      Command command = Command.builder("test")
          .then(Command.argument("num", IntegerArgumentType.integer())
              .executes(ctx -> CommandResult.SUCCESS))
          .build();

      assertThrows(CommandException.class,
          () -> command.execute(player, "test notanumber"));
    }

    @Test
    @DisplayName("should throw argument error for out-of-bounds integer")
    void shouldThrowForOutOfBoundsInteger() {
      Command command = Command.builder("test")
          .then(Command.argument("num", IntegerArgumentType.integer(1, 10))
              .executes(ctx -> CommandResult.SUCCESS))
          .build();

      assertThrows(CommandException.class,
          () -> command.execute(player, "test 100"));
    }

    @Test
    @DisplayName("should propagate executor exceptions")
    void shouldPropagateExecutorExceptions() {
      Command command = Command.builder("test")
          .executes(ctx -> {
            throw new CommandException("Custom error");
          })
          .build();

      CommandException ex = assertThrows(CommandException.class,
          () -> command.execute(player, "test"));

      assertEquals("Custom error", ex.getMessage());
    }
  }
}

