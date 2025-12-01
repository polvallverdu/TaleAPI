package dev.polv.taleapi.command;

import dev.polv.taleapi.event.EventPriority;
import dev.polv.taleapi.event.EventResult;
import dev.polv.taleapi.testutil.TestCommandSender;
import dev.polv.taleapi.testutil.TestPlayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CommandExecuteCallback")
class CommandExecuteCallbackTest {

  private CommandRegistry registry;
  private TestPlayer player;
  private TestCommandSender console;

  @BeforeEach
  void setUp() {
    registry = new CommandRegistry();
    player = new TestPlayer("TestPlayer").setOp(true);
    console = new TestCommandSender();

    // Register a test command
    registry.register(Command.builder("test")
        .executes(ctx -> {
          ctx.getSender().sendMessage("Command executed!");
          return CommandResult.SUCCESS;
        })
        .build());
  }

  @AfterEach
  void cleanup() {
    CommandExecuteCallback.EVENT.clearListeners();
  }

  @Test
  @DisplayName("should fire event before command execution")
  void shouldFireEventBeforeExecution() {
    AtomicBoolean eventFired = new AtomicBoolean(false);
    AtomicBoolean commandExecuted = new AtomicBoolean(false);

    CommandExecuteCallback.EVENT.register((sender, command, input) -> {
      eventFired.set(true);
      assertFalse(commandExecuted.get(), "Event should fire before command executes");
      return EventResult.PASS;
    });

    registry.register(Command.builder("tracked")
        .executes(ctx -> {
          commandExecuted.set(true);
          return CommandResult.SUCCESS;
        })
        .build());

    registry.dispatch(player, "tracked");

    assertTrue(eventFired.get());
    assertTrue(commandExecuted.get());
  }

  @Test
  @DisplayName("should cancel command execution when event is cancelled")
  void shouldCancelExecution() {
    AtomicBoolean commandExecuted = new AtomicBoolean(false);

    CommandExecuteCallback.EVENT.register((sender, command, input) -> {
      return EventResult.CANCEL;
    });

    registry.register(Command.builder("blocked")
        .executes(ctx -> {
          commandExecuted.set(true);
          return CommandResult.SUCCESS;
        })
        .build());

    CommandResult result = registry.dispatch(player, "blocked");

    assertFalse(commandExecuted.get());
    assertEquals(CommandResult.FAILURE, result);
  }

  @Test
  @DisplayName("should provide correct sender to event")
  void shouldProvideCorrectSender() {
    AtomicReference<CommandSender> capturedSender = new AtomicReference<>();

    CommandExecuteCallback.EVENT.register((sender, command, input) -> {
      capturedSender.set(sender);
      return EventResult.PASS;
    });

    registry.dispatch(player, "test");
    assertSame(player, capturedSender.get());

    registry.dispatch(console, "test");
    assertSame(console, capturedSender.get());
  }

  @Test
  @DisplayName("should provide correct command to event")
  void shouldProvideCorrectCommand() {
    AtomicReference<Command> capturedCommand = new AtomicReference<>();

    CommandExecuteCallback.EVENT.register((sender, command, input) -> {
      capturedCommand.set(command);
      return EventResult.PASS;
    });

    registry.dispatch(player, "test");

    assertNotNull(capturedCommand.get());
    assertEquals("test", capturedCommand.get().getName());
  }

  @Test
  @DisplayName("should provide correct input to event")
  void shouldProvideCorrectInput() {
    AtomicReference<String> capturedInput = new AtomicReference<>();

    registry.register(Command.builder("echo")
        .then(Command.argument("message", dev.polv.taleapi.command.argument.StringArgumentType.greedyString())
            .executes(ctx -> CommandResult.SUCCESS))
        .build());

    CommandExecuteCallback.EVENT.register((sender, command, input) -> {
      capturedInput.set(input);
      return EventResult.PASS;
    });

    registry.dispatch(player, "echo hello world");
    assertEquals("echo hello world", capturedInput.get());

    // With leading slash
    registry.dispatch(player, "/echo test message");
    assertEquals("echo test message", capturedInput.get()); // Slash is stripped
  }

  @Test
  @DisplayName("should respect priority order")
  void shouldRespectPriorityOrder() {
    List<String> order = new ArrayList<>();

    CommandExecuteCallback.EVENT.register(EventPriority.LOWEST, (sender, command, input) -> {
      order.add("LOWEST");
      return EventResult.PASS;
    });

    CommandExecuteCallback.EVENT.register(EventPriority.HIGHEST, (sender, command, input) -> {
      order.add("HIGHEST");
      return EventResult.PASS;
    });

    CommandExecuteCallback.EVENT.register(EventPriority.NORMAL, (sender, command, input) -> {
      order.add("NORMAL");
      return EventResult.PASS;
    });

    registry.dispatch(player, "test");

    assertEquals(List.of("HIGHEST", "NORMAL", "LOWEST"), order);
  }

  @Test
  @DisplayName("should stop processing when cancelled")
  void shouldStopProcessingWhenCancelled() {
    List<String> executed = new ArrayList<>();

    CommandExecuteCallback.EVENT.register(EventPriority.HIGHEST, (sender, command, input) -> {
      executed.add("HIGHEST");
      return EventResult.CANCEL;
    });

    CommandExecuteCallback.EVENT.register(EventPriority.NORMAL, (sender, command, input) -> {
      executed.add("NORMAL");
      return EventResult.PASS;
    });

    CommandExecuteCallback.EVENT.register(EventPriority.LOWEST, (sender, command, input) -> {
      executed.add("LOWEST");
      return EventResult.PASS;
    });

    registry.dispatch(player, "test");

    // Only HIGHEST should run since it cancelled
    assertEquals(List.of("HIGHEST"), executed);
  }

  @Test
  @DisplayName("should allow logging all commands")
  void shouldAllowLoggingAllCommands() {
    List<String> log = new ArrayList<>();

    CommandExecuteCallback.EVENT.register((sender, command, input) -> {
      log.add(sender.getName() + " executed: /" + input);
      return EventResult.PASS;
    });

    registry.dispatch(player, "test");
    registry.dispatch(console, "test");

    assertEquals(2, log.size());
    assertEquals("TestPlayer executed: /test", log.get(0));
    assertEquals("Console executed: /test", log.get(1));
  }

  @Test
  @DisplayName("should work with console sender")
  void shouldWorkWithConsoleSender() {
    AtomicBoolean eventFired = new AtomicBoolean(false);

    CommandExecuteCallback.EVENT.register((sender, command, input) -> {
      eventFired.set(true);
      assertFalse(sender.isPlayer());
      assertEquals("Console", sender.getName());
      return EventResult.PASS;
    });

    registry.dispatch(console, "test");

    assertTrue(eventFired.get());
    assertEquals("Command executed!", console.getLastMessage());
  }

  @Test
  @DisplayName("should work with player sender")
  void shouldWorkWithPlayerSender() {
    AtomicBoolean eventFired = new AtomicBoolean(false);

    CommandExecuteCallback.EVENT.register((sender, command, input) -> {
      eventFired.set(true);
      assertTrue(sender.isPlayer());
      assertNotNull(sender.asPlayer());
      assertEquals("TestPlayer", sender.getName());
      return EventResult.PASS;
    });

    registry.dispatch(player, "test");

    assertTrue(eventFired.get());
    assertEquals("Command executed!", player.getLastMessage());
  }

  @Test
  @DisplayName("should allow blocking specific commands")
  void shouldAllowBlockingSpecificCommands() {
    registry.register(Command.builder("allowed")
        .executes(ctx -> {
          ctx.getSender().sendMessage("Allowed!");
          return CommandResult.SUCCESS;
        })
        .build());

    registry.register(Command.builder("blocked")
        .executes(ctx -> {
          ctx.getSender().sendMessage("Blocked!");
          return CommandResult.SUCCESS;
        })
        .build());

    CommandExecuteCallback.EVENT.register((sender, command, input) -> {
      if (command.getName().equals("blocked")) {
        sender.sendMessage("This command is disabled!");
        return EventResult.CANCEL;
      }
      return EventResult.PASS;
    });

    registry.dispatch(player, "allowed");
    assertEquals("Allowed!", player.getLastMessage());

    player.clearMessages();
    registry.dispatch(player, "blocked");
    assertEquals("This command is disabled!", player.getLastMessage());
  }

  @Test
  @DisplayName("should unregister listener")
  void shouldUnregisterListener() {
    AtomicBoolean listenerCalled = new AtomicBoolean(false);

    CommandExecuteCallback listener = (sender, command, input) -> {
      listenerCalled.set(true);
      return EventResult.CANCEL;
    };

    CommandExecuteCallback.EVENT.register(listener);

    // Command should be blocked
    CommandResult result1 = registry.dispatch(player, "test");
    assertEquals(CommandResult.FAILURE, result1);
    assertTrue(listenerCalled.get());

    // Unregister and reset
    CommandExecuteCallback.EVENT.unregister(listener);
    listenerCalled.set(false);

    // Command should now work
    CommandResult result2 = registry.dispatch(player, "test");
    assertEquals(CommandResult.SUCCESS, result2);
    assertFalse(listenerCalled.get());
  }
}

