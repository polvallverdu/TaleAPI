# TaleAPI Events

TaleAPI uses a Fabric-inspired event system that is type-safe, decentralized, and supports priorities and cancellation.

## Core Concepts

### Event Structure

Each event is defined as a **functional interface** with a static `EVENT` field that holds the event instance:

```java
@FunctionalInterface
public interface PlayerJoinCallback {

  Event<PlayerJoinCallback> EVENT = Event.create(
      callbacks -> player -> {
        for (PlayerJoinCallback callback : callbacks) {
          EventResult result = callback.onPlayerJoin(player);
          if (result.shouldStop()) {
            return result;
          }
        }
        return EventResult.PASS;
      },
      player -> EventResult.PASS
  );

  EventResult onPlayerJoin(TalePlayer player);
}
```

### Registering Listeners

Use `EVENT.register()` to add a listener:

```java
// Simple registration (NORMAL priority)
PlayerJoinCallback.EVENT.register(player -> {
  player.sendMessage("Welcome, " + player.getName() + "!");
  return EventResult.PASS;
});

// With priority
PlayerJoinCallback.EVENT.register(EventPriority.HIGHEST, player -> {
  if (isBanned(player)) {
    return EventResult.CANCEL;
  }
  return EventResult.PASS;
});
```

### Firing Events

To fire an event, call `invoker()` and invoke the callback method:

```java
EventResult result = PlayerJoinCallback.EVENT.invoker().onPlayerJoin(player);

if (result.isCancelled()) {
  // Handle cancellation - e.g., kick the player
  player.kick("You are not allowed to join.");
}
```

## Priority System

Listeners are executed in priority order from **HIGHEST to LOWEST**:

| Priority  | Execution Order  | Use Case                        |
| --------- | ---------------- | ------------------------------- |
| `HIGHEST` | 1st (runs first) | Critical checks, security, bans |
| `HIGH`    | 2nd              | Important processing            |
| `NORMAL`  | 3rd (default)    | Standard event handling         |
| `LOW`     | 4th              | After main processing           |
| `LOWEST`  | 5th (runs last)  | Final cleanup, logging          |

```java
// This runs FIRST
PlayerJoinCallback.EVENT.register(EventPriority.HIGHEST, player -> {
  System.out.println("I run first!");
  return EventResult.PASS;
});

// This runs LAST
PlayerJoinCallback.EVENT.register(EventPriority.LOWEST, player -> {
  System.out.println("I run last!");
  return EventResult.PASS;
});
```

## Event Results

For cancellable events, return an `EventResult`:

| Result    | Effect                                      |
| --------- | ------------------------------------------- |
| `PASS`    | Continue to next listener                   |
| `SUCCESS` | Stop processing, event handled successfully |
| `CANCEL`  | Stop processing, cancel the event           |

```java
PlayerJoinCallback.EVENT.register(player -> {
  if (isServerFull()) {
    return EventResult.CANCEL; // Prevent join
  }
  return EventResult.PASS; // Allow, continue to other listeners
});
```

## Async Events

Fire events asynchronously using `invokeAsync()`:

```java
Executor executor = Executors.newSingleThreadExecutor();

CompletableFuture<EventResult> future = PlayerJoinCallback.EVENT.invokeAsync(
    executor,
    invoker -> invoker.onPlayerJoin(player)
);

future.thenAccept(result -> {
  if (result.isCancelled()) {
    // Handle async cancellation
  }
});
```

## Available Events

### Player Events

| Event                | Cancellable | Description                |
| -------------------- | ----------- | -------------------------- |
| `PlayerJoinCallback` | ✅ Yes      | Called when a player joins |
| `PlayerQuitCallback` | ❌ No       | Called when a player quits |

## Creating Custom Events

### Step 1: Define the Callback Interface

```java
@FunctionalInterface
public interface BlockBreakCallback {

  Event<BlockBreakCallback> EVENT = Event.create(
      callbacks -> (player, block) -> {
        for (BlockBreakCallback callback : callbacks) {
          EventResult result = callback.onBlockBreak(player, block);
          if (result.shouldStop()) {
            return result;
          }
        }
        return EventResult.PASS;
      },
      (player, block) -> EventResult.PASS
  );

  EventResult onBlockBreak(TalePlayer player, Block block);
}
```

### Step 2: Fire the Event

```java
// In your block breaking logic
EventResult result = BlockBreakCallback.EVENT.invoker().onBlockBreak(player, block);

if (!result.isCancelled()) {
  // Proceed with breaking the block
  block.destroy();
}
```

### Step 3: Listen to the Event

```java
BlockBreakCallback.EVENT.register(EventPriority.HIGHEST, (player, block) -> {
  if (block.isProtected()) {
    player.sendMessage("This block is protected!");
    return EventResult.CANCEL;
  }
  return EventResult.PASS;
});
```

### Non-Cancellable Events

For events that don't need cancellation (like `PlayerQuitCallback`), use `void` return type:

```java
@FunctionalInterface
public interface ChatMessageCallback {

  Event<ChatMessageCallback> EVENT = Event.create(
      callbacks -> (player, message) -> {
        for (ChatMessageCallback callback : callbacks) {
          callback.onChatMessage(player, message);
        }
      },
      (player, message) -> {}
  );

  void onChatMessage(TalePlayer player, String message);
}
```

## Unregistering Listeners

Store a reference to your listener to unregister it later:

```java
PlayerJoinCallback myListener = player -> {
  player.sendMessage("Hello!");
  return EventResult.PASS;
};

// Register
PlayerJoinCallback.EVENT.register(myListener);

// Later: unregister
PlayerJoinCallback.EVENT.unregister(myListener);
```

## Best Practices

1. **Use appropriate priorities**: Don't use `HIGHEST` for everything. Reserve it for critical security checks.

2. **Return `PASS` by default**: Only return `CANCEL` or `SUCCESS` when you explicitly want to stop processing.

3. **Keep listeners fast**: Event listeners run synchronously by default. For heavy processing, use `invokeAsync()`.

4. **Document your events**: Use JavaDoc to explain when events fire, what cancellation means, and any side effects.

5. **Clean up listeners**: If your mod/plugin can be disabled, unregister your listeners to prevent memory leaks.
