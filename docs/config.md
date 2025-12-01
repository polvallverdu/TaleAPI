# TaleAPI Configuration

TaleAPI provides a flexible configuration API for loading and saving data files. It supports multiple formats through a provider abstraction, with JSON support (via Jackson) included out of the box.

## Two Ways to Load Config

TaleAPI supports two loading modes:

1. **Object Mapping** — Load directly into a typed Java class
2. **Dynamic Access** — Load into a `ConfigNode` for key-based traversal

```java
ConfigLoader loader = new ConfigLoader(new JsonProvider());

// Object mapping - load into a typed class
ServerConfig config = loader.load(new File("config.json"), ServerConfig.class);

// Dynamic access - traverse values by key
ConfigNode node = loader.load(new File("config.json"));
String name = node.getString("serverName");
int port = node.getInt("database.port", 5432);
```

## Quick Start

### Object Mapping (Typed)

Define a configuration class and load it directly:

```java
public class ServerConfig {
    public String serverName = "My Server";
    public int maxPlayers = 100;
    public boolean enableWhitelist = false;
}

// Load
ConfigLoader loader = new ConfigLoader(new JsonProvider());
ServerConfig config = loader.load(new File("server.json"), ServerConfig.class);

// Save
loader.save(new File("server.json"), config);
```

### Dynamic Access (ConfigNode)

Access values dynamically without defining a class:

```java
ConfigLoader loader = new ConfigLoader(new JsonProvider());
ConfigNode config = loader.load(new File("config.json"));

// Get values
String name = config.getString("serverName");
int port = config.getInt("port", 8080);  // with default
boolean debug = config.getBoolean("debug", false);

// Navigate nested objects with dot notation
String dbHost = config.getString("database.host");
int dbPort = config.getInt("database.port");

// Or navigate step by step
ConfigNode database = config.getSection("database");
String host = database.getString("host");

// Work with lists
List<String> mods = config.getStringList("enabledMods");
List<Integer> ports = config.getIntList("allowedPorts");

// Check existence
if (config.has("optionalFeature")) {
    // ...
}
```

## ConfigNode API

### Type Checking

```java
node.isObject()   // true for JSON objects
node.isArray()    // true for JSON arrays
node.isValue()    // true for primitives
node.isNull()     // true for null/missing
```

### Key Operations

```java
node.has("key")           // check if key exists
node.has("nested.key")    // works with dot notation
node.keys()               // get all keys
node.size()               // number of keys/elements
```

### Getting Values

All getters support dot notation for nested paths:

```java
// Required values (throws ConfigException if missing)
String name = node.getString("name");
int count = node.getInt("count");
long bigNum = node.getLong("bigNumber");
double ratio = node.getDouble("ratio");
boolean flag = node.getBoolean("enabled");

// With defaults (returns default if missing)
String name = node.getString("name", "default");
int count = node.getInt("count", 0);
boolean flag = node.getBoolean("enabled", true);

// Optional values (returns Optional.empty() if missing)
Optional<String> name = node.getOptionalString("name");
Optional<Integer> count = node.getOptionalInt("count");
```

### Lists

```java
List<ConfigNode> items = node.getList("items");
List<String> tags = node.getStringList("tags");
List<Integer> numbers = node.getIntList("numbers");
```

### Navigation

```java
// By key
ConfigNode child = node.get("key");
ConfigNode nested = node.get("parent.child.grandchild");
ConfigNode section = node.getSection("database");

// By array index
ConfigNode first = node.get(0);
ConfigNode second = node.get(1);
```

### Converting to Typed Objects

```java
// Convert entire node
ServerConfig config = node.as(ServerConfig.class);

// Convert child node
DatabaseConfig db = node.get("database", DatabaseConfig.class);
```

### JSON Output

```java
String json = node.toJson();           // compact
String pretty = node.toPrettyJson();   // pretty-printed
```

## Async Operations

All load and save operations have async variants:

```java
ConfigLoader loader = new ConfigLoader(new JsonProvider());

// Async load to typed object
loader.loadAsync(file, ServerConfig.class)
    .thenAccept(config -> {
        System.out.println("Server: " + config.serverName);
    });

// Async load to ConfigNode
loader.loadAsync(file)
    .thenAccept(node -> {
        System.out.println("Server: " + node.getString("serverName"));
    });

// Async save
loader.saveAsync(file, config)
    .thenRun(() -> System.out.println("Saved!"));
```

### Custom Executor

```java
Executor ioExecutor = Executors.newFixedThreadPool(4);

// Via constructor
ConfigLoader loader = new ConfigLoader(new JsonProvider(), ioExecutor);

// Or per-operation
loader.loadAsync(file, ServerConfig.class, ioExecutor);
```

## Nested Objects and Collections

Both APIs fully support complex data structures:

```java
public class GameConfig {
    public String gameName;
    public DatabaseConfig database;
    public List<String> enabledMods;
    public Map<String, Integer> scores;
}

public class DatabaseConfig {
    public String host = "localhost";
    public int port = 5432;
}
```

Resulting JSON:

```json
{
  "gameName": "My Game",
  "database": {
    "host": "localhost",
    "port": 5432
  },
  "enabledMods": ["mod1", "mod2"],
  "scores": {
    "player1": 100,
    "player2": 250
  }
}
```

## Supported Types

| Type           | Support | Notes                                        |
| -------------- | ------- | -------------------------------------------- |
| Primitives     | ✅      | `int`, `long`, `double`, `boolean`, etc.     |
| Wrappers       | ✅      | `Integer`, `Long`, `Double`, `Boolean`, etc. |
| Strings        | ✅      |                                              |
| Arrays         | ✅      | `String[]`, `int[]`, etc.                    |
| Lists          | ✅      | `List<T>`, `ArrayList<T>`                    |
| Sets           | ✅      | `Set<T>`, `HashSet<T>`                       |
| Maps           | ✅      | `Map<String, T>` (String keys for JSON)      |
| Nested Objects | ✅      | Any depth                                    |
| Records        | ✅      | Java 14+ records                             |
| Enums          | ✅      | Serialized as strings                        |

## JsonProvider Configuration

### Default Settings

```java
// Pretty printing enabled, unknown properties ignored
JsonProvider provider = new JsonProvider();
```

### Builder API

```java
JsonProvider provider = JsonProvider.builder()
    .prettyPrinting()           // Enable pretty printing
    .compact()                  // Disable pretty printing
    .ignoreUnknownProperties()  // Ignore extra JSON fields
    .failOnUnknownProperties()  // Throw on extra JSON fields
    .dateFormat("yyyy-MM-dd")   // Custom date format
    .build();
```

### Custom ObjectMapper

For full control, provide your own Jackson ObjectMapper:

```java
ObjectMapper mapper = new ObjectMapper()
    .enable(SerializationFeature.INDENT_OUTPUT)
    .registerModule(new JavaTimeModule());

JsonProvider provider = new JsonProvider(mapper);
```

## Utility Methods

### Load with Defaults

```java
ServerConfig defaultConfig = new ServerConfig();
defaultConfig.serverName = "Default Server";

// If file doesn't exist, saves defaultConfig and returns it
ServerConfig config = loader.loadOrDefault(file, ServerConfig.class, defaultConfig);
```

### Validation

```java
// Check if file is valid (typed)
if (loader.isValid(file, ServerConfig.class)) {
    ServerConfig config = loader.load(file, ServerConfig.class);
}

// Check if file is valid (untyped)
if (loader.isValid(file)) {
    ConfigNode node = loader.load(file);
}
```

## Error Handling

All errors are wrapped in `ConfigException`:

```java
try {
    ConfigNode config = loader.load(new File("config.json"));
} catch (ConfigException e) {
    System.err.println("Failed: " + e.getMessage());

    // Original exception available
    if (e.getCause() instanceof IOException) {
        // Handle I/O error
    }
}
```

## Creating Custom Providers

Implement `ConfigProvider` for other formats (YAML, TOML, etc.):

```java
public class YamlProvider implements ConfigProvider {

    @Override
    public <T> T deserialize(Reader reader, Class<T> type) throws ConfigException {
        // Parse YAML and return typed object
    }

    @Override
    public ConfigNode deserializeToNode(Reader reader) throws ConfigException {
        // Parse YAML and return ConfigNode
    }

    @Override
    public <T> void serialize(Writer writer, T value) throws ConfigException {
        // Write object as YAML
    }

    @Override
    public void serialize(Writer writer, ConfigNode node) throws ConfigException {
        // Write ConfigNode as YAML
    }

    @Override
    public String fileExtension() {
        return "yml";
    }
}
```

## Best Practices

### 1. Use ConfigNode for Unknown Structures

When reading third-party configs or dynamic data:

```java
ConfigNode config = loader.load(new File("unknown.json"));

// Safely check and access
if (config.has("version")) {
    int version = config.getInt("version");
}
```

### 2. Use Object Mapping for Your Own Configs

When you control the structure:

```java
public class MyModConfig {
    public boolean enableFeatureX = true;
    public int cooldownSeconds = 30;
}

MyModConfig config = loader.loadOrDefault(file, MyModConfig.class, new MyModConfig());
```

### 3. Use Defaults for Optional Values

```java
// With ConfigNode
int timeout = config.getInt("timeout", 30);

// With classes
public class Config {
    public int timeout = 30;  // default value
}
```

### 4. Validate After Loading

```java
ServerConfig config = loader.load(file, ServerConfig.class);

if (config.maxPlayers < 1 || config.maxPlayers > 1000) {
    throw new IllegalArgumentException("maxPlayers must be 1-1000");
}
```

## API Reference

### ConfigLoader Methods

| Method                             | Description                      |
| ---------------------------------- | -------------------------------- |
| `load(File, Class<T>)`             | Load file into typed object      |
| `load(File)`                       | Load file into ConfigNode        |
| `load(Path, Class<T>)`             | Load path into typed object      |
| `load(Path)`                       | Load path into ConfigNode        |
| `loadFromString(String, Class<T>)` | Parse string into typed object   |
| `loadNodeFromString(String)`       | Parse string into ConfigNode     |
| `loadAsync(File, Class<T>)`        | Async load to typed object       |
| `loadAsync(File)`                  | Async load to ConfigNode         |
| `loadOrDefault(File, Class<T>, T)` | Load or create with default      |
| `save(File, T)`                    | Save typed object to file        |
| `save(File, ConfigNode)`           | Save ConfigNode to file          |
| `saveToString(T)`                  | Serialize to string              |
| `saveAsync(File, T)`               | Async save                       |
| `isValid(File, Class<T>)`          | Check if file is valid (typed)   |
| `isValid(File)`                    | Check if file is valid (untyped) |

### ConfigNode Methods

| Method                                            | Description               |
| ------------------------------------------------- | ------------------------- |
| `getString(key)`                                  | Get required string       |
| `getString(key, default)`                         | Get string with default   |
| `getInt(key)` / `getLong(key)` / `getDouble(key)` | Get numbers               |
| `getBoolean(key)`                                 | Get boolean               |
| `getStringList(key)` / `getIntList(key)`          | Get typed lists           |
| `getList(key)`                                    | Get list of ConfigNodes   |
| `get(key)` / `getSection(key)`                    | Navigate to child node    |
| `get(index)`                                      | Get array element         |
| `has(key)`                                        | Check key existence       |
| `keys()`                                          | Get all keys              |
| `as(Class<T>)`                                    | Convert to typed object   |
| `get(key, Class<T>)`                              | Get child as typed object |
| `toJson()` / `toPrettyJson()`                     | Serialize to JSON         |
