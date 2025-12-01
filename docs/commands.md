# TaleAPI Commands

TaleAPI provides a powerful command API inspired by Brigadier, featuring a fluent builder pattern, typed arguments, autocompletion, and permission-based access control.

## Quick Start

### Registering Commands

Commands are registered through the `CommandRegisterCallback` event:

```java
CommandRegisterCallback.EVENT.register(registry -> {
    registry.register(Command.builder("hello")
        .description("Say hello!")
        .executes(ctx -> {
            ctx.getSender().sendMessage("Hello, " + ctx.getSender().getName() + "!");
            return CommandResult.SUCCESS;
        })
        .build());
});
```

### Executing Commands

```java
// Through the registry
CommandResult result = registry.dispatch(sender, "hello");
CommandResult result = registry.dispatch(sender, "/hello"); // Slash is optional

// Directly on a command
Command command = registry.getCommand("hello").orElseThrow();
CommandResult result = command.execute(sender, "hello");
```

## Command Structure

Commands are structured as a tree of **literal nodes** (fixed text) and **argument nodes** (typed parameters):

```
gamemode (literal - root)
├── survival (literal) → executes
├── creative (literal) → executes
└── <mode> (argument: integer)
    └── <player> (argument: string) → executes
```

### Literals

Literals are fixed text that must be matched exactly (case-insensitive):

```java
Command.builder("gamemode")
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
    .build();
```

Usage: `/gamemode survival` or `/gamemode creative`

### Arguments

Arguments parse user input into typed values:

```java
Command.builder("give")
    .then(Command.argument("player", StringArgumentType.word())
        .then(Command.argument("item", StringArgumentType.word())
            .then(Command.argument("amount", IntegerArgumentType.integer(1, 64))
                .executes(ctx -> {
                    String player = ctx.getArgument("player", String.class);
                    String item = ctx.getArgument("item", String.class);
                    int amount = ctx.getArgument("amount", Integer.class);
                    ctx.getSender().sendMessage("Giving " + amount + "x " + item + " to " + player);
                    return CommandResult.SUCCESS;
                }))))
    .build();
```

Usage: `/give Steve diamond 64`

## Argument Types

### StringArgumentType

| Mode   | Method           | Description                       | Example Input                    |
| ------ | ---------------- | --------------------------------- | -------------------------------- |
| Word   | `word()`         | Single word (stops at whitespace) | `hello`                          |
| String | `string()`       | Quoted string or single word      | `"hello world"`                  |
| Greedy | `greedyString()` | All remaining input               | `hello world this is everything` |

```java
// Single word
Command.argument("name", StringArgumentType.word())

// Quoted or single word
Command.argument("message", StringArgumentType.string())

// All remaining input (must be last argument)
Command.argument("reason", StringArgumentType.greedyString())
```

### IntegerArgumentType

```java
// Any integer
Command.argument("count", IntegerArgumentType.integer())

// Integer >= 0
Command.argument("amount", IntegerArgumentType.integer(0))

// Integer between 1 and 100 (inclusive)
Command.argument("level", IntegerArgumentType.integer(1, 100))
```

### DoubleArgumentType

```java
// Any double
Command.argument("value", DoubleArgumentType.doubleArg())

// Double >= 0.0
Command.argument("amount", DoubleArgumentType.doubleArg(0.0))

// Double between 0.0 and 1.0
Command.argument("percent", DoubleArgumentType.doubleArg(0.0, 1.0))
```

### FloatArgumentType

```java
// Any float
Command.argument("speed", FloatArgumentType.floatArg())

// Float with bounds
Command.argument("scale", FloatArgumentType.floatArg(0.1f, 10.0f))
```

### LongArgumentType

```java
// Any long
Command.argument("id", LongArgumentType.longArg())

// Long >= 0
Command.argument("timestamp", LongArgumentType.longArg(0))
```

### BooleanArgumentType

```java
// Accepts "true" or "false" (case-insensitive)
Command.argument("enabled", BooleanArgumentType.bool())
```

## Permissions

### Command-Level Permissions

```java
Command.builder("ban")
    .permission("server.admin.ban")
    .then(Command.argument("player", StringArgumentType.word())
        .executes(ctx -> {
            // Only runs if sender has "server.admin.ban" permission
            return CommandResult.SUCCESS;
        }))
    .build();
```

### Subcommand Permissions

```java
Command.builder("server")
    .then(Command.literal("reload")
        .requires("server.reload")
        .executes(ctx -> CommandResult.SUCCESS))
    .then(Command.literal("stop")
        .requires("server.stop")
        .executes(ctx -> CommandResult.SUCCESS))
    .build();
```

### Custom Requirements

```java
Command.builder("fly")
    .requires(sender -> sender.isPlayer() && sender.asPlayer().isOp())
    .executes(ctx -> CommandResult.SUCCESS)
    .build();
```

### Permission Format

Permissions use a dot-separated hierarchy:

| Permission          | Grants Access To                 |
| ------------------- | -------------------------------- |
| `server.admin.kick` | Exact permission                 |
| `server.admin.*`    | All `server.admin.X` permissions |
| `server.*`          | All `server.X.Y` permissions     |
| `*`                 | All permissions                  |

```java
// Player permission checks
player.hasPermission("server.admin.kick");  // Exact check
player.hasPermission("server.admin.ban");   // Granted by "server.admin.*"
```

## Autocompletion / Suggestions

### Built-in Suggestions

Argument types provide default suggestions:

- `BooleanArgumentType` suggests `true` and `false`
- `IntegerArgumentType` suggests common values like `0`, `1`, `10`

### Custom Suggestions

Use `SuggestionProvider` for dynamic suggestions:

```java
Command.builder("tp")
    .then(Command.argument("player", StringArgumentType.word())
        .suggests(SuggestionProvider.of("Steve", "Alex", "Notch"))
        .executes(ctx -> CommandResult.SUCCESS))
    .build();
```

### Dynamic Suggestions

```java
Command.builder("teleport")
    .then(Command.argument("player", StringArgumentType.word())
        .suggests((context, builder) -> {
            // Add online players
            for (TalePlayer player : getOnlinePlayers()) {
                builder.suggest(player.getDisplayName());
            }
            return builder.buildFuture();
        })
        .executes(ctx -> CommandResult.SUCCESS))
    .build();
```

### Suggestions with Tooltips

```java
.suggests((context, builder) -> {
    builder.suggest("creative", "Unlimited resources, flight, no damage");
    builder.suggest("survival", "Standard gameplay with health and hunger");
    builder.suggest("adventure", "Explore but cannot break blocks");
    return builder.buildFuture();
})
```

### Getting Suggestions

```java
// From registry
CompletableFuture<Suggestions> future = registry.getSuggestions(sender, "gamemode c");
Suggestions suggestions = future.join();

for (Suggestion suggestion : suggestions.getList()) {
    System.out.println(suggestion.getText());  // "creative"
}

// From a specific command
CompletableFuture<Suggestions> future = command.getSuggestions(sender, "gamemode s");
```

## Command Senders

Commands can be executed by different types of senders. The `CommandSender` interface is the base for all command sources:

### Players

Players implement both `TalePlayer` and `CommandSender`:

```java
.executes(ctx -> {
    CommandSender sender = ctx.getSender();

    if (sender.isPlayer()) {
        TalePlayer player = sender.asPlayer();
        player.teleport(someLocation);
    } else {
        sender.sendMessage("This command can only be used by players!");
        return CommandResult.FAILURE;
    }

    return CommandResult.SUCCESS;
})
```

### Console

The console (or server) can also execute commands. Console senders typically have all permissions:

```java
// Check sender type
if (!sender.isPlayer()) {
    // This is the console
    System.out.println("Command executed from console");
}

// Console always has permissions
sender.hasPermission("any.permission"); // true for console
```

### CommandSender Methods

| Method                  | Description                                 |
| ----------------------- | ------------------------------------------- |
| `sendMessage(String)`   | Send a message to the sender                |
| `hasPermission(String)` | Check if sender has permission              |
| `getName()`             | Get sender name ("Console" or player name)  |
| `isPlayer()`            | Check if sender is a player                 |
| `asPlayer()`            | Get as TalePlayer (or null if not a player) |

## Command Context

The `CommandContext` provides access to the sender, parsed arguments, and raw input:

```java
.executes(ctx -> {
    // Get the command sender
    CommandSender sender = ctx.getSender();

    // Check if sender is a player
    if (sender.isPlayer()) {
        TalePlayer player = sender.asPlayer();
    }

    // Get parsed arguments
    String name = ctx.getArgument("name", String.class);
    int count = ctx.getArgument("count", Integer.class);

    // Optional arguments
    Optional<String> reason = ctx.getOptionalArgument("reason", String.class);

    // Check if argument exists
    if (ctx.hasArgument("target")) {
        // ...
    }

    // Get raw input
    String rawInput = ctx.getRawInput();

    // Get the command being executed
    Command command = ctx.getCommand();

    return CommandResult.SUCCESS;
})
```

## Command Results

| Result    | Meaning                       | Use Case                               |
| --------- | ----------------------------- | -------------------------------------- |
| `SUCCESS` | Command executed successfully | Normal completion                      |
| `FAILURE` | Command failed to execute     | Logic failure (player not found, etc.) |
| `PASS`    | Command was not handled       | Pass to next handler                   |

```java
.executes(ctx -> {
    String playerName = ctx.getArgument("player", String.class);
    TalePlayer target = findPlayer(playerName);

    if (target == null) {
        ctx.getSender().sendMessage("Player not found: " + playerName);
        return CommandResult.FAILURE;
    }

    // Do something with target...
    return CommandResult.SUCCESS;
})
```

## Aliases

Register alternative names for commands:

```java
Command.builder("teleport")
    .aliases("tp", "warp", "goto")
    .executes(ctx -> CommandResult.SUCCESS)
    .build();
```

All of these work: `/teleport`, `/tp`, `/warp`, `/goto`

## Error Handling

### CommandException Types

| Type         | Description                 |
| ------------ | --------------------------- |
| `SYNTAX`     | Invalid command syntax      |
| `PERMISSION` | Missing required permission |
| `ARGUMENT`   | Invalid argument value      |
| `EXECUTION`  | General execution failure   |

### Throwing Exceptions

```java
.executes(ctx -> {
    String mode = ctx.getArgument("mode", String.class);

    if (!isValidMode(mode)) {
        throw CommandException.argument("mode", "Invalid game mode: " + mode);
    }

    if (!ctx.getSender().hasPermission("server.gamemode." + mode)) {
        throw CommandException.permission("server.gamemode." + mode);
    }

    return CommandResult.SUCCESS;
})
```

### Handling Exceptions

```java
try {
    registry.dispatch(sender, input);
} catch (CommandException e) {
    switch (e.getType()) {
        case SYNTAX -> sender.sendMessage("Usage: " + command.getUsage());
        case PERMISSION -> sender.sendMessage("You don't have permission!");
        case ARGUMENT -> sender.sendMessage("Invalid argument: " + e.getMessage());
        case EXECUTION -> sender.sendMessage("Command failed: " + e.getMessage());
    }
}
```

## Command Execution Event

The `CommandExecuteCallback` event fires whenever a command is about to be executed. This allows you to:

- Cancel command execution
- Log command usage
- Implement command cooldowns
- Block commands in certain contexts (e.g., during a minigame)

### Basic Usage

```java
// Log all command usage
CommandExecuteCallback.EVENT.register((sender, command, input) -> {
    System.out.println(sender.getName() + " executed: /" + input);
    return EventResult.PASS;
});
```

### Cancelling Commands

```java
// Block all commands during a match
CommandExecuteCallback.EVENT.register(EventPriority.HIGHEST, (sender, command, input) -> {
    if (sender.isPlayer() && isInMatch(sender.asPlayer())) {
        if (!command.getName().equals("leave")) {
            sender.sendMessage("Commands are disabled during the match!");
            return EventResult.CANCEL;
        }
    }
    return EventResult.PASS;
});
```

### Command Cooldowns

```java
private Map<String, Map<String, Long>> cooldowns = new HashMap<>();

CommandExecuteCallback.EVENT.register((sender, command, input) -> {
    if (!sender.isPlayer()) return EventResult.PASS;

    String playerId = sender.asPlayer().getUniqueId();
    String cmdName = command.getName();

    long now = System.currentTimeMillis();
    long lastUse = cooldowns
        .computeIfAbsent(playerId, k -> new HashMap<>())
        .getOrDefault(cmdName, 0L);

    if (now - lastUse < 5000) { // 5 second cooldown
        sender.sendMessage("Please wait before using this command again!");
        return EventResult.CANCEL;
    }

    cooldowns.get(playerId).put(cmdName, now);
    return EventResult.PASS;
});
```

### Event Parameters

| Parameter | Description                                                   |
| --------- | ------------------------------------------------------------- |
| `sender`  | The `CommandSender` executing the command (player or console) |
| `command` | The `Command` being executed                                  |
| `input`   | The full command input string (without leading slash)         |

### Return Values

| Result                | Effect                                          |
| --------------------- | ----------------------------------------------- |
| `EventResult.PASS`    | Continue to next listener, then execute command |
| `EventResult.SUCCESS` | Stop listeners, execute the command             |
| `EventResult.CANCEL`  | Stop listeners, do NOT execute the command      |

## CommandRegistry API

### Registration

```java
registry.register(command);                    // Register a command
registry.unregister("commandName");            // Unregister by name
registry.clear();                              // Remove all commands
```

### Retrieval

```java
Optional<Command> cmd = registry.getCommand("teleport");  // By name or alias
boolean exists = registry.hasCommand("tp");               // Check existence
Collection<Command> all = registry.getCommands();         // All commands
Collection<String> names = registry.getCommandNames();    // All command names
int count = registry.size();                              // Number of commands
```

### Execution & Suggestions

```java
CommandResult result = registry.dispatch(sender, "give Steve diamond 64");
CompletableFuture<Suggestions> suggestions = registry.getSuggestions(sender, "give S");
```

### Available Commands

```java
// Get commands the sender has permission to use
List<Command> available = registry.getAvailableCommands(sender);
```

## Complete Example

```java
CommandRegisterCallback.EVENT.register(registry -> {
    // Simple command
    registry.register(Command.builder("ping")
        .description("Check server latency")
        .executes(ctx -> {
            ctx.getSender().sendMessage("Pong!");
            return CommandResult.SUCCESS;
        })
        .build());

    // Command with arguments and suggestions
    registry.register(Command.builder("msg")
        .aliases("tell", "whisper", "w")
        .description("Send a private message")
        .permission("chat.msg")
        .then(Command.argument("player", StringArgumentType.word())
            .suggests((ctx, builder) -> {
                for (TalePlayer p : getOnlinePlayers()) {
                    builder.suggest(p.getDisplayName());
                }
                return builder.buildFuture();
            })
            .then(Command.argument("message", StringArgumentType.greedyString())
                .executes(ctx -> {
                    String target = ctx.getArgument("player", String.class);
                    String message = ctx.getArgument("message", String.class);
                    TalePlayer recipient = findPlayer(target);

                    if (recipient == null) {
                        ctx.getSender().sendMessage("Player not found!");
                        return CommandResult.FAILURE;
                    }

                    recipient.sendMessage("[" + ctx.getSender().getName() + "] " + message);
                    ctx.getSender().sendMessage("[You -> " + target + "] " + message);
                    return CommandResult.SUCCESS;
                })))
        .build());

    // Command with subcommands and different permissions
    registry.register(Command.builder("server")
        .description("Server management commands")
        .then(Command.literal("status")
            .executes(ctx -> {
                ctx.getSender().sendMessage("Server is running!");
                return CommandResult.SUCCESS;
            }))
        .then(Command.literal("reload")
            .requires("server.admin.reload")
            .executes(ctx -> {
                ctx.getSender().sendMessage("Reloading configuration...");
                return CommandResult.SUCCESS;
            }))
        .then(Command.literal("stop")
            .requires("server.admin.stop")
            .then(Command.argument("delay", IntegerArgumentType.integer(0, 60))
                .executes(ctx -> {
                    int delay = ctx.getArgument("delay", Integer.class);
                    ctx.getSender().sendMessage("Server stopping in " + delay + " seconds...");
                    return CommandResult.SUCCESS;
                }))
            .executes(ctx -> {
                ctx.getSender().sendMessage("Server stopping now!");
                return CommandResult.SUCCESS;
            }))
        .build());
});
```

## API Reference

### Command.Builder Methods

| Method                               | Description                          |
| ------------------------------------ | ------------------------------------ |
| `description(String)`                | Set command description              |
| `aliases(String...)`                 | Add command aliases                  |
| `permission(String)`                 | Set required permission              |
| `requires(Predicate<CommandSender>)` | Set custom requirement               |
| `then(CommandNode)`                  | Add child node (literal or argument) |
| `executes(CommandExecutor)`          | Set executor for this node           |
| `build()`                            | Build the command                    |

### CommandNode Methods

| Method                                   | Description                                |
| ---------------------------------------- | ------------------------------------------ |
| `Command.literal(String)`                | Create a literal node                      |
| `Command.argument(String, ArgumentType)` | Create an argument node                    |
| `then(CommandNode)`                      | Add child node                             |
| `executes(CommandExecutor)`              | Set executor                               |
| `requires(String)`                       | Set permission requirement                 |
| `requires(Predicate<CommandSender>)`     | Set custom requirement                     |
| `suggests(SuggestionProvider)`           | Set custom suggestions (ArgumentNode only) |

### CommandContext Methods

| Method                             | Description                             |
| ---------------------------------- | --------------------------------------- |
| `getSender()`                      | Get the command sender                  |
| `getRawInput()`                    | Get the raw command input               |
| `getCommand()`                     | Get the executing command               |
| `getArgument(name, Class)`         | Get parsed argument (throws if missing) |
| `getOptionalArgument(name, Class)` | Get argument as Optional                |
| `hasArgument(name)`                | Check if argument exists                |
| `getArguments()`                   | Get all arguments as Map                |

### CommandSender Methods

| Method                  | Description                 |
| ----------------------- | --------------------------- |
| `sendMessage(String)`   | Send a message              |
| `hasPermission(String)` | Check permission            |
| `getName()`             | Get sender name             |
| `isPlayer()`            | Check if sender is a player |
| `asPlayer()`            | Get as TalePlayer (or null) |
