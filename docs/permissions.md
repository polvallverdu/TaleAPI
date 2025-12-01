# TaleAPI Permissions

TaleAPI features a modern, extensible permission system built on the Service Provider Interface (SPI) pattern. It supports dynamic values, contextual permissions, and efficient O(k) lookups using a Radix Tree.

## Core Concepts

### Tristate (Not Just Booleans!)

Unlike traditional systems, permissions use a **Tristate**:

| State       | Meaning                                         |
| ----------- | ----------------------------------------------- |
| `ALLOW`     | Permission is explicitly granted                |
| `DENY`      | Permission is explicitly denied                 |
| `UNDEFINED` | No explicit value - inherit from parent/default |

This distinction is crucial for permission inheritance. `UNDEFINED` inherits, while `DENY` blocks inheritance.

```java
Tristate state = result.getState();
if (state == Tristate.ALLOW) {
  // Explicitly allowed
} else if (state == Tristate.DENY) {
  // Explicitly denied
} else {
  // Use default behavior
}
```

### Dynamic Values (Payloads)

Permissions can carry **payload values**, solving the "magic number" problem:

```java
// ❌ Old way: "plots.limit.5" - requires string parsing
// ✅ New way: key="plots.limit", payload=5

int maxPlots = perms.query(player, "plots.limit").asInt(1);
double multiplier = perms.query(player, "exp.multiplier").asDouble(1.0);
String rank = perms.query(player, "chat.prefix").asString("[Member]");
```

### Contextual Permissions

Permissions can have **context** - conditions for when they apply:

```java
// Permission only applies in the nether world
PermissionNode node = PermissionNode.builder("ability.fly")
    .allow()
    .context(ContextSet.of(ContextKey.WORLD, "nether"))
    .build();

// Query with current context
ContextSet playerContext = ContextSet.of(ContextKey.WORLD, player.getWorld());
PermissionResult result = perms.query(player, "ability.fly", playerContext);
```

Built-in context keys:

| Key                   | Description            |
| --------------------- | ---------------------- |
| `ContextKey.WORLD`    | World name             |
| `ContextKey.SERVER`   | Server name (networks) |
| `ContextKey.GAMEMODE` | Player's gamemode      |

## Basic Usage

### Checking Permissions

```java
// Get the permission service
PermissionService perms = PermissionService.getInstance();

// Simple boolean check
if (perms.query(player, "cmd.teleport").isAllowed()) {
  // Player can teleport
}

// Convenience method
if (perms.has(player, "cmd.teleport")) {
  // Same as above
}

// On the player directly (via PermissionHolder)
if (player.hasPermission("cmd.teleport")) {
  // Also works!
}
```

### Getting Dynamic Values

```java
// Get an integer limit
int maxHomes = perms.query(player, "homes.limit").asInt(3);

// Get a double multiplier
double expMultiplier = perms.query(player, "exp.multiplier").asDouble(1.0);

// Get a string value
String prefix = perms.query(player, "chat.prefix").asString("[Member]");

// Check state and get value
PermissionResult result = perms.query(player, "plots.limit");
if (result.isAllowed()) {
  int limit = result.asInt(1);
  // Use the limit
}
```

### Context-Aware Queries

```java
// Build the player's current context
ContextSet context = ContextSet.builder()
    .add(ContextKey.WORLD, "creative")
    .add(ContextKey.GAMEMODE, "creative")
    .build();

// Query with context
if (perms.has(player, "build.unlimited", context)) {
  // Allow unlimited building in creative
}
```

## Wildcards

The permission tree supports wildcards for efficient group permissions:

```java
// Grant all commands
tree.allow("cmd.*");

// These all return ALLOW:
tree.has("cmd.teleport");  // true
tree.has("cmd.give");      // true
tree.has("cmd.anything");  // true

// Root wildcard grants everything
tree.allow("*");
tree.has("literally.anything.here");  // true
```

Wildcard performance is O(1) - traversal stops immediately when a wildcard is found.

## The Permission Hook

A `PermissionCheckCallback` event fires after the provider calculates a result but before the game receives it. This allows temporary overrides without modifying stored permissions.

### Denying During Minigames

```java
PermissionCheckCallback.EVENT.register((player, key, context, result) -> {
  // Deny teleport commands during matches
  if (matchManager.isInMatch(player) && key.startsWith("cmd.teleport")) {
    return CheckResult.deny();
  }
  return CheckResult.unmodified();
});
```

### Granting Temporary Abilities

```java
PermissionCheckCallback.EVENT.register((player, key, context, result) -> {
  // Grant fly during events
  if (eventManager.hasTemporaryFlight(player) && key.equals("ability.fly")) {
    return CheckResult.allow();
  }
  return CheckResult.unmodified();
});
```

### Modifying Limits

```java
PermissionCheckCallback.EVENT.register((player, key, context, result) -> {
  // Double plot limits during weekend events
  if (isWeekendEvent() && key.equals("plots.limit")) {
    int original = result.asInt(1);
    return CheckResult.allow(original * 2);
  }
  return CheckResult.unmodified();
});
```

## Architecture: SPI Pattern

The permission system uses the **Service Provider Interface** pattern:

```
                  ┌──────────────────────┐
                  │  PermissionService   │  ← Public API
                  │    (Singleton)       │
                  └──────────┬───────────┘
                             │
                  ┌──────────▼───────────┐
                  │  PermissionProvider  │  ← SPI Interface
                  │    (Pluggable)       │
                  └──────────┬───────────┘
                             │
              ┌──────────────┼──────────────┐
              ▼                             ▼
      ┌───────────────┐             ┌───────────────┐
      │DefaultProvider│             │ Custom Plugin │
      │   (JSON)      │             │   Provider    │
      └───────────────┘             └───────────────┘
```

- **The Engine** provides a default JSON-based provider
- **Third-party plugins** can register their own high-performance provider
- **The Hook** allows additional customization without changing providers

## Creating a Custom Provider

Implement `PermissionProvider` to create a custom backend:

```java
public class MyDatabaseProvider implements PermissionProvider {

  @Override
  public String getId() {
    return "mydatabase";
  }

  @Override
  public String getName() {
    return "My Database Provider";
  }

  @Override
  public PermissionResult query(TalePlayer player, String key) {
    // Query your database
    return PermissionResult.ALLOWED;
  }

  @Override
  public PermissionResult query(TalePlayer player, String key, ContextSet context) {
    // Context-aware query
    return query(player, key);
  }

  @Override
  public CompletableFuture<Void> loadPlayer(TalePlayer player) {
    // Load from database, cache locally
    return CompletableFuture.runAsync(() -> {
      // Load logic
    });
  }

  @Override
  public void unloadPlayer(TalePlayer player) {
    // Clear cache
  }

  // ... implement other methods
}
```

Register your provider:

```java
PermissionService.getInstance().setProvider(new MyDatabaseProvider());
```

## The Radix Tree

Permissions are stored in a **Radix Tree (Trie)** for efficient lookups:

```
root
├── cmd
│   ├── teleport [ALLOW]
│   ├── give [DENY]
│   └── * [ALLOW]        ← Wildcard: cmd.anything = ALLOW
└── plots
    ├── create [ALLOW]
    └── limit [ALLOW, payload=5]
```

**Performance characteristics:**

- **Lookup:** O(k) where k = permission string length
- **Wildcards:** O(1) early termination
- **Independent of tree size:** 10,000 permissions? Still O(k)

## Setting Permissions Programmatically

```java
PermissionService perms = PermissionService.getInstance();

// Set a simple permission
perms.setPermission(player, PermissionNode.allow("cmd.fly"));

// Set a permission with payload
perms.setPermission(player, PermissionNode.allow("plots.limit", 10));

// Set a contextual permission
PermissionNode node = PermissionNode.builder("ability.speed")
    .allow()
    .payload(2.0)  // Speed multiplier
    .context(ContextSet.of(ContextKey.WORLD, "hub"))
    .build();
perms.setPermission(player, node);

// Remove a permission
perms.removePermission(player, "cmd.fly");
```

## PermissionResult Methods

| Method               | Description                        |
| -------------------- | ---------------------------------- |
| `getState()`         | Returns the Tristate               |
| `isAllowed()`        | True if state is ALLOW             |
| `isDenied()`         | True if state is DENY              |
| `isUndefined()`      | True if state is UNDEFINED         |
| `hasPayload()`       | True if a payload value exists     |
| `asInt(default)`     | Get payload as int                 |
| `asLong(default)`    | Get payload as long                |
| `asDouble(default)`  | Get payload as double              |
| `asString()`         | Get payload as string              |
| `asString(default)`  | Get payload as string with default |
| `asBoolean(default)` | Get payload as boolean             |
| `as(Class<T>)`       | Get typed payload as Optional      |
| `asOptionalInt()`    | Get payload as OptionalInt         |
| `asOptionalLong()`   | Get payload as OptionalLong        |
| `asOptionalDouble()` | Get payload as OptionalDouble      |

## Best Practices

1. **Use descriptive permission keys**: `mymod.feature.action` is better than `mymod.1`

2. **Use payloads for limits**: Don't create `plots.limit.1`, `plots.limit.2`, etc. Use `plots.limit` with a payload.

3. **Use context for world/server-specific permissions**: Instead of `plots.create.creative`, use `plots.create` with world context.

4. **Hook sparingly**: The permission hook is powerful but adds overhead. Use it for temporary overrides, not permanent logic.

5. **Cache results**: If checking the same permission many times per tick, cache the result locally.

6. **Use wildcards wisely**: `admin.*` is convenient but be careful not to grant too much.

## Complete Example

```java
public class PlotManager {

  public void onPlotClaim(TalePlayer player) {
    PermissionService perms = PermissionService.getInstance();

    // Check if player can claim plots
    PermissionResult canClaim = perms.query(player, "plots.claim");
    if (!canClaim.isAllowed()) {
      player.sendMessage("You don't have permission to claim plots!");
      return;
    }

    // Get their plot limit
    int maxPlots = perms.query(player, "plots.limit").asInt(1);
    int currentPlots = getPlayerPlotCount(player);

    if (currentPlots >= maxPlots) {
      player.sendMessage("You've reached your plot limit (" + maxPlots + ")!");
      return;
    }

    // Check world-specific permissions
    ContextSet context = ContextSet.of(ContextKey.WORLD, player.getWorld());
    if (!perms.has(player, "plots.claim", context)) {
      player.sendMessage("You can't claim plots in this world!");
      return;
    }

    // Claim the plot
    claimPlot(player);
    player.sendMessage("Plot claimed! (" + (currentPlots + 1) + "/" + maxPlots + ")");
  }
}
```
