package dev.polv.taleapi.command;

import dev.polv.taleapi.command.argument.IntegerArgumentType;
import dev.polv.taleapi.command.argument.StringArgumentType;
import dev.polv.taleapi.command.suggestion.SuggestionProvider;
import dev.polv.taleapi.event.EventPriority;
import dev.polv.taleapi.testutil.TestPlayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CommandRegisterCallback")
class CommandRegisterCallbackTest {

  private CommandRegistry registry;
  private TestPlayer player;

  @BeforeEach
  void setUp() {
    registry = new CommandRegistry();
    player = new TestPlayer("TestPlayer").setOp(true);
  }

  @AfterEach
  void cleanup() {
    CommandRegisterCallback.EVENT.clearListeners();
  }

  @Test
  @DisplayName("should register commands through event")
  void shouldRegisterCommandsThroughEvent() {
    CommandRegisterCallback.EVENT.register(reg -> {
      reg.register(Command.builder("hello")
          .executes(ctx -> CommandResult.SUCCESS)
          .build());
    });

    CommandRegisterCallback.EVENT.invoker().onRegisterCommands(registry);

    assertTrue(registry.hasCommand("hello"));
  }

  @Test
  @DisplayName("should support multiple listeners")
  void shouldSupportMultipleListeners() {
    CommandRegisterCallback.EVENT.register(reg -> {
      reg.register(Command.builder("cmd1")
          .executes(ctx -> CommandResult.SUCCESS)
          .build());
    });

    CommandRegisterCallback.EVENT.register(reg -> {
      reg.register(Command.builder("cmd2")
          .executes(ctx -> CommandResult.SUCCESS)
          .build());
    });

    CommandRegisterCallback.EVENT.invoker().onRegisterCommands(registry);

    assertTrue(registry.hasCommand("cmd1"));
    assertTrue(registry.hasCommand("cmd2"));
    assertEquals(2, registry.size());
  }

  @Test
  @DisplayName("should support priority ordering")
  void shouldSupportPriorityOrdering() {
    StringBuilder order = new StringBuilder();

    CommandRegisterCallback.EVENT.register(EventPriority.LOWEST, reg -> {
      order.append("LOWEST,");
    });

    CommandRegisterCallback.EVENT.register(EventPriority.HIGHEST, reg -> {
      order.append("HIGHEST,");
    });

    CommandRegisterCallback.EVENT.register(EventPriority.NORMAL, reg -> {
      order.append("NORMAL,");
    });

    CommandRegisterCallback.EVENT.invoker().onRegisterCommands(registry);

    assertEquals("HIGHEST,NORMAL,LOWEST,", order.toString());
  }

  @Test
  @DisplayName("should work with complete command example")
  void shouldWorkWithCompleteExample() {
    CommandRegisterCallback.EVENT.register(reg -> {
      // Simple command
      reg.register(Command.builder("ping")
          .description("Pong!")
          .executes(ctx -> {
            ctx.getSender().sendMessage("Pong!");
            return CommandResult.SUCCESS;
          })
          .build());

      // Command with arguments and suggestions
      reg.register(Command.builder("greet")
          .description("Greet a player")
          .permission("server.greet")
          .then(Command.argument("player", StringArgumentType.word())
              .suggests(SuggestionProvider.of("Steve", "Alex", "Notch"))
              .executes(ctx -> {
                String name = ctx.getArgument("player", String.class);
                ctx.getSender().sendMessage("Hello, " + name + "!");
                return CommandResult.SUCCESS;
              }))
          .build());

      // Command with subcommands
      reg.register(Command.builder("gamemode")
          .aliases("gm")
          .description("Change game mode")
          .permission("server.gamemode")
          .then(Command.literal("survival")
              .executes(ctx -> {
                ctx.getSender().sendMessage("Survival mode!");
                return CommandResult.SUCCESS;
              }))
          .then(Command.literal("creative")
              .executes(ctx -> {
                ctx.getSender().sendMessage("Creative mode!");
                return CommandResult.SUCCESS;
              }))
          .then(Command.argument("mode", IntegerArgumentType.integer(0, 3))
              .executes(ctx -> {
                int mode = ctx.getArgument("mode", Integer.class);
                ctx.getSender().sendMessage("Mode " + mode + "!");
                return CommandResult.SUCCESS;
              }))
          .build());
    });

    CommandRegisterCallback.EVENT.invoker().onRegisterCommands(registry);

    // Verify registration
    assertEquals(3, registry.size());
    assertTrue(registry.hasCommand("ping"));
    assertTrue(registry.hasCommand("greet"));
    assertTrue(registry.hasCommand("gamemode"));
    assertTrue(registry.hasCommand("gm")); // alias

    // Test execution
    registry.dispatch(player, "ping");
    assertEquals("Pong!", player.getLastMessage());

    player.clearMessages();
    registry.dispatch(player, "greet World");
    assertEquals("Hello, World!", player.getLastMessage());

    player.clearMessages();
    registry.dispatch(player, "gm creative");
    assertEquals("Creative mode!", player.getLastMessage());

    player.clearMessages();
    registry.dispatch(player, "gamemode 2");
    assertEquals("Mode 2!", player.getLastMessage());

    // Test suggestions
    var suggestions = registry.getSuggestions(player, "gamemode ").join();
    assertTrue(suggestions.getList().size() >= 3); // survival, creative, and integer suggestions
  }

  @Test
  @DisplayName("should do nothing when no listeners registered")
  void shouldDoNothingWithNoListeners() {
    // Empty event should not throw
    assertDoesNotThrow(() -> 
        CommandRegisterCallback.EVENT.invoker().onRegisterCommands(registry));

    assertEquals(0, registry.size());
  }

  @Test
  @DisplayName("should unregister listeners")
  void shouldUnregisterListeners() {
    CommandRegisterCallback listener = reg -> {
      reg.register(Command.builder("test")
          .executes(ctx -> CommandResult.SUCCESS)
          .build());
    };

    CommandRegisterCallback.EVENT.register(listener);
    assertTrue(CommandRegisterCallback.EVENT.unregister(listener));

    CommandRegisterCallback.EVENT.invoker().onRegisterCommands(registry);

    assertEquals(0, registry.size());
  }
}

