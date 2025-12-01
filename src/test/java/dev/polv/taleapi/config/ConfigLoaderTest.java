package dev.polv.taleapi.config;

import dev.polv.taleapi.config.json.JsonProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ConfigLoader")
class ConfigLoaderTest {

  @TempDir
  Path tempDir;

  private ConfigLoader loader;

  // Test configuration classes
  public static class TestConfig {
    public String name;
    public int value;
    public List<String> items;

    public TestConfig() {
    }

    public TestConfig(String name, int value, List<String> items) {
      this.name = name;
      this.value = value;
      this.items = items;
    }
  }

  @BeforeEach
  void setUp() {
    loader = new ConfigLoader(new JsonProvider());
  }

  @Nested
  @DisplayName("Constructor")
  class Constructor {

    @Test
    @DisplayName("should create loader with provider")
    void shouldCreateWithProvider() {
      JsonProvider provider = new JsonProvider();
      ConfigLoader loader = new ConfigLoader(provider);

      assertSame(provider, loader.getProvider());
    }

    @Test
    @DisplayName("should throw NullPointerException for null provider")
    void shouldThrowForNullProvider() {
      assertThrows(NullPointerException.class, () -> new ConfigLoader(null));
    }

    @Test
    @DisplayName("should accept custom executor")
    void shouldAcceptCustomExecutor() {
      var executor = Executors.newSingleThreadExecutor();
      try {
        ConfigLoader loader = new ConfigLoader(new JsonProvider(), executor);
        assertNotNull(loader);
      } finally {
        executor.shutdown();
      }
    }
  }

  @Nested
  @DisplayName("Synchronous Load to Object")
  class SynchronousLoadToObject {

    @Test
    @DisplayName("should load config from file")
    void shouldLoadFromFile() throws IOException {
      File file = tempDir.resolve("config.json").toFile();
      String json = """
          {
              "name": "test",
              "value": 42,
              "items": ["a", "b"]
          }
          """;
      Files.writeString(file.toPath(), json, StandardCharsets.UTF_8);

      TestConfig config = loader.load(file, TestConfig.class);

      assertEquals("test", config.name);
      assertEquals(42, config.value);
      assertEquals(List.of("a", "b"), config.items);
    }

    @Test
    @DisplayName("should load config from path")
    void shouldLoadFromPath() throws IOException {
      Path path = tempDir.resolve("config.json");
      String json = """
          {
              "name": "path-test",
              "value": 100,
              "items": []
          }
          """;
      Files.writeString(path, json, StandardCharsets.UTF_8);

      TestConfig config = loader.load(path, TestConfig.class);

      assertEquals("path-test", config.name);
      assertEquals(100, config.value);
    }

    @Test
    @DisplayName("should load config from reader")
    void shouldLoadFromReader() {
      String json = """
          {
              "name": "reader-test",
              "value": 200,
              "items": ["x"]
          }
          """;

      TestConfig config = loader.load(new StringReader(json), TestConfig.class);

      assertEquals("reader-test", config.name);
      assertEquals(200, config.value);
      assertEquals(List.of("x"), config.items);
    }

    @Test
    @DisplayName("should load config from string")
    void shouldLoadFromString() {
      String json = """
          {
              "name": "string-test",
              "value": 300,
              "items": ["y", "z"]
          }
          """;

      TestConfig config = loader.loadFromString(json, TestConfig.class);

      assertEquals("string-test", config.name);
      assertEquals(300, config.value);
      assertEquals(List.of("y", "z"), config.items);
    }

    @Test
    @DisplayName("should throw ConfigException for non-existent file")
    void shouldThrowForNonExistentFile() {
      File file = tempDir.resolve("nonexistent.json").toFile();

      assertThrows(ConfigException.class, () -> loader.load(file, TestConfig.class));
    }

    @Test
    @DisplayName("should throw ConfigException for invalid JSON")
    void shouldThrowForInvalidJson() throws IOException {
      File file = tempDir.resolve("invalid.json").toFile();
      Files.writeString(file.toPath(), "{ invalid }", StandardCharsets.UTF_8);

      assertThrows(ConfigException.class, () -> loader.load(file, TestConfig.class));
    }
  }

  @Nested
  @DisplayName("Synchronous Load to ConfigNode")
  class SynchronousLoadToNode {

    @Test
    @DisplayName("should load config from file as ConfigNode")
    void shouldLoadFromFileAsNode() throws IOException {
      File file = tempDir.resolve("config.json").toFile();
      String json = """
          {
              "serverName": "MyServer",
              "port": 8080,
              "database": {
                  "host": "localhost",
                  "port": 5432
              }
          }
          """;
      Files.writeString(file.toPath(), json, StandardCharsets.UTF_8);

      ConfigNode node = loader.load(file);

      assertTrue(node.isObject());
      assertEquals("MyServer", node.getString("serverName"));
      assertEquals(8080, node.getInt("port"));
      assertEquals("localhost", node.getString("database.host"));
      assertEquals(5432, node.getInt("database.port"));
    }

    @Test
    @DisplayName("should load config from path as ConfigNode")
    void shouldLoadFromPathAsNode() throws IOException {
      Path path = tempDir.resolve("config.json");
      String json = """
          {
              "name": "test",
              "enabled": true
          }
          """;
      Files.writeString(path, json, StandardCharsets.UTF_8);

      ConfigNode node = loader.load(path);

      assertEquals("test", node.getString("name"));
      assertTrue(node.getBoolean("enabled"));
    }

    @Test
    @DisplayName("should load config from reader as ConfigNode")
    void shouldLoadFromReaderAsNode() {
      String json = """
          {
              "items": ["a", "b", "c"]
          }
          """;

      ConfigNode node = loader.loadNode(new StringReader(json));

      assertEquals(List.of("a", "b", "c"), node.getStringList("items"));
    }

    @Test
    @DisplayName("should load config from string as ConfigNode")
    void shouldLoadFromStringAsNode() {
      String json = """
          {
              "count": 42
          }
          """;

      ConfigNode node = loader.loadNodeFromString(json);

      assertEquals(42, node.getInt("count"));
    }
  }

  @Nested
  @DisplayName("Synchronous Save")
  class SynchronousSave {

    @Test
    @DisplayName("should save config to file")
    void shouldSaveToFile() throws IOException {
      File file = tempDir.resolve("output.json").toFile();
      TestConfig config = new TestConfig("saved", 42, List.of("a"));

      loader.save(file, config);

      assertTrue(file.exists());
      String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
      assertTrue(content.contains("\"name\""));
      assertTrue(content.contains("\"saved\""));
      assertTrue(content.contains("\"value\""));
      assertTrue(content.contains("42"));
    }

    @Test
    @DisplayName("should save config to path")
    void shouldSaveToPath() throws IOException {
      Path path = tempDir.resolve("output2.json");
      TestConfig config = new TestConfig("path-saved", 100, List.of("b"));

      loader.save(path, config);

      assertTrue(Files.exists(path));
    }

    @Test
    @DisplayName("should save config to writer")
    void shouldSaveToWriter() {
      StringWriter writer = new StringWriter();
      TestConfig config = new TestConfig("writer-saved", 200, List.of("c"));

      loader.save(writer, config);

      String json = writer.toString();
      assertTrue(json.contains("\"name\""));
      assertTrue(json.contains("\"writer-saved\""));
    }

    @Test
    @DisplayName("should save config to string")
    void shouldSaveToString() {
      TestConfig config = new TestConfig("string-saved", 300, List.of("d"));

      String json = loader.saveToString(config);

      assertTrue(json.contains("\"name\""));
      assertTrue(json.contains("\"string-saved\""));
      assertTrue(json.contains("\"value\""));
      assertTrue(json.contains("300"));
    }

    @Test
    @DisplayName("should create parent directories")
    void shouldCreateParentDirectories() {
      File file = tempDir.resolve("nested/dir/config.json").toFile();
      TestConfig config = new TestConfig("nested", 1, List.of());

      loader.save(file, config);

      assertTrue(file.exists());
      assertTrue(file.getParentFile().exists());
    }

    @Test
    @DisplayName("should overwrite existing file")
    void shouldOverwriteExistingFile() throws IOException {
      File file = tempDir.resolve("overwrite.json").toFile();
      Files.writeString(file.toPath(), "{}", StandardCharsets.UTF_8);

      TestConfig config = new TestConfig("new", 999, List.of());
      loader.save(file, config);

      String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
      assertTrue(content.contains("\"new\""));
      assertTrue(content.contains("999"));
    }
  }

  @Nested
  @DisplayName("Asynchronous Load to Object")
  class AsynchronousLoadToObject {

    @Test
    @DisplayName("should load config from file asynchronously")
    void shouldLoadAsyncFromFile() throws Exception {
      File file = tempDir.resolve("async-config.json").toFile();
      String json = """
          {
              "name": "async",
              "value": 42,
              "items": []
          }
          """;
      Files.writeString(file.toPath(), json, StandardCharsets.UTF_8);

      CompletableFuture<TestConfig> future = loader.loadAsync(file, TestConfig.class);
      TestConfig config = future.get(5, TimeUnit.SECONDS);

      assertEquals("async", config.name);
      assertEquals(42, config.value);
    }

    @Test
    @DisplayName("should load config from path asynchronously")
    void shouldLoadAsyncFromPath() throws Exception {
      Path path = tempDir.resolve("async-path-config.json");
      String json = """
          {
              "name": "async-path",
              "value": 100,
              "items": []
          }
          """;
      Files.writeString(path, json, StandardCharsets.UTF_8);

      CompletableFuture<TestConfig> future = loader.loadAsync(path, TestConfig.class);
      TestConfig config = future.get(5, TimeUnit.SECONDS);

      assertEquals("async-path", config.name);
    }

    @Test
    @DisplayName("should load with custom executor")
    void shouldLoadWithCustomExecutor() throws Exception {
      File file = tempDir.resolve("custom-executor.json").toFile();
      String json = """
          {
              "name": "executor",
              "value": 200,
              "items": []
          }
          """;
      Files.writeString(file.toPath(), json, StandardCharsets.UTF_8);

      var executor = Executors.newSingleThreadExecutor();
      try {
        CompletableFuture<TestConfig> future = loader.loadAsync(file, TestConfig.class, executor);
        TestConfig config = future.get(5, TimeUnit.SECONDS);

        assertEquals("executor", config.name);
      } finally {
        executor.shutdown();
      }
    }

    @Test
    @DisplayName("should complete exceptionally for non-existent file")
    void shouldFailForNonExistentFile() {
      File file = tempDir.resolve("nonexistent-async.json").toFile();

      CompletableFuture<TestConfig> future = loader.loadAsync(file, TestConfig.class);

      ExecutionException exception = assertThrows(ExecutionException.class,
          () -> future.get(5, TimeUnit.SECONDS));
      assertTrue(exception.getCause() instanceof ConfigException);
    }
  }

  @Nested
  @DisplayName("Asynchronous Load to ConfigNode")
  class AsynchronousLoadToNode {

    @Test
    @DisplayName("should load config from file as ConfigNode asynchronously")
    void shouldLoadAsyncAsNode() throws Exception {
      File file = tempDir.resolve("async-node.json").toFile();
      String json = """
          {
              "name": "async-node",
              "value": 42
          }
          """;
      Files.writeString(file.toPath(), json, StandardCharsets.UTF_8);

      CompletableFuture<ConfigNode> future = loader.loadAsync(file);
      ConfigNode node = future.get(5, TimeUnit.SECONDS);

      assertEquals("async-node", node.getString("name"));
      assertEquals(42, node.getInt("value"));
    }

    @Test
    @DisplayName("should load config from path as ConfigNode asynchronously")
    void shouldLoadAsyncFromPathAsNode() throws Exception {
      Path path = tempDir.resolve("async-path-node.json");
      String json = """
          {
              "enabled": true
          }
          """;
      Files.writeString(path, json, StandardCharsets.UTF_8);

      CompletableFuture<ConfigNode> future = loader.loadAsync(path);
      ConfigNode node = future.get(5, TimeUnit.SECONDS);

      assertTrue(node.getBoolean("enabled"));
    }
  }

  @Nested
  @DisplayName("Asynchronous Save")
  class AsynchronousSave {

    @Test
    @DisplayName("should save config to file asynchronously")
    void shouldSaveAsyncToFile() throws Exception {
      File file = tempDir.resolve("async-save.json").toFile();
      TestConfig config = new TestConfig("async-saved", 42, List.of());

      CompletableFuture<Void> future = loader.saveAsync(file, config);
      future.get(5, TimeUnit.SECONDS);

      assertTrue(file.exists());
      String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
      assertTrue(content.contains("\"async-saved\""));
    }

    @Test
    @DisplayName("should save config to path asynchronously")
    void shouldSaveAsyncToPath() throws Exception {
      Path path = tempDir.resolve("async-save-path.json");
      TestConfig config = new TestConfig("async-path-saved", 100, List.of());

      CompletableFuture<Void> future = loader.saveAsync(path, config);
      future.get(5, TimeUnit.SECONDS);

      assertTrue(Files.exists(path));
    }

    @Test
    @DisplayName("should save with custom executor")
    void shouldSaveWithCustomExecutor() throws Exception {
      File file = tempDir.resolve("custom-executor-save.json").toFile();
      TestConfig config = new TestConfig("executor-saved", 200, List.of());

      var executor = Executors.newSingleThreadExecutor();
      try {
        CompletableFuture<Void> future = loader.saveAsync(file, config, executor);
        future.get(5, TimeUnit.SECONDS);

        assertTrue(file.exists());
      } finally {
        executor.shutdown();
      }
    }
  }

  @Nested
  @DisplayName("Load Or Default")
  class LoadOrDefault {

    @Test
    @DisplayName("should load existing config")
    void shouldLoadExistingConfig() throws IOException {
      File file = tempDir.resolve("existing.json").toFile();
      String json = """
          {
              "name": "existing",
              "value": 42,
              "items": []
          }
          """;
      Files.writeString(file.toPath(), json, StandardCharsets.UTF_8);

      TestConfig defaultConfig = new TestConfig("default", 0, List.of());
      TestConfig config = loader.loadOrDefault(file, TestConfig.class, defaultConfig);

      assertEquals("existing", config.name);
      assertEquals(42, config.value);
    }

    @Test
    @DisplayName("should return default and save when file doesn't exist")
    void shouldReturnAndSaveDefault() throws IOException {
      File file = tempDir.resolve("new.json").toFile();
      TestConfig defaultConfig = new TestConfig("default", 999, List.of("default-item"));

      TestConfig config = loader.loadOrDefault(file, TestConfig.class, defaultConfig);

      assertEquals("default", config.name);
      assertEquals(999, config.value);
      assertTrue(file.exists());

      // Verify the default was saved
      String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
      assertTrue(content.contains("\"default\""));
    }

    @Test
    @DisplayName("should work with path parameter")
    void shouldWorkWithPath() throws IOException {
      Path path = tempDir.resolve("path-default.json");
      TestConfig defaultConfig = new TestConfig("path-default", 123, List.of());

      TestConfig config = loader.loadOrDefault(path, TestConfig.class, defaultConfig);

      assertEquals("path-default", config.name);
      assertTrue(Files.exists(path));
    }
  }

  @Nested
  @DisplayName("Validation")
  class Validation {

    @Test
    @DisplayName("should return true for valid config file (typed)")
    void shouldReturnTrueForValidFileTyped() throws IOException {
      File file = tempDir.resolve("valid.json").toFile();
      String json = """
          {
              "name": "valid",
              "value": 42,
              "items": []
          }
          """;
      Files.writeString(file.toPath(), json, StandardCharsets.UTF_8);

      assertTrue(loader.isValid(file, TestConfig.class));
    }

    @Test
    @DisplayName("should return true for valid config file (untyped)")
    void shouldReturnTrueForValidFileUntyped() throws IOException {
      File file = tempDir.resolve("valid.json").toFile();
      Files.writeString(file.toPath(), "{\"key\": \"value\"}", StandardCharsets.UTF_8);

      assertTrue(loader.isValid(file));
    }

    @Test
    @DisplayName("should return false for non-existent file")
    void shouldReturnFalseForNonExistent() {
      File file = tempDir.resolve("nonexistent.json").toFile();

      assertFalse(loader.isValid(file, TestConfig.class));
      assertFalse(loader.isValid(file));
    }

    @Test
    @DisplayName("should return false for invalid JSON")
    void shouldReturnFalseForInvalidJson() throws IOException {
      File file = tempDir.resolve("invalid.json").toFile();
      Files.writeString(file.toPath(), "{ invalid }", StandardCharsets.UTF_8);

      assertFalse(loader.isValid(file, TestConfig.class));
      assertFalse(loader.isValid(file));
    }

    @Test
    @DisplayName("should return false for directory")
    void shouldReturnFalseForDirectory() throws IOException {
      Path dir = tempDir.resolve("directory");
      Files.createDirectory(dir);

      assertFalse(loader.isValid(dir.toFile(), TestConfig.class));
      assertFalse(loader.isValid(dir.toFile()));
    }

    @Test
    @DisplayName("should work with path parameter")
    void shouldWorkWithPath() throws IOException {
      Path path = tempDir.resolve("path-valid.json");
      Files.writeString(path, "{\"name\":\"test\",\"value\":1,\"items\":[]}", StandardCharsets.UTF_8);

      assertTrue(loader.isValid(path, TestConfig.class));
      assertTrue(loader.isValid(path));
    }
  }

  @Nested
  @DisplayName("Roundtrip")
  class Roundtrip {

    @Test
    @DisplayName("should roundtrip config through file")
    void shouldRoundtripThroughFile() throws IOException {
      File file = tempDir.resolve("roundtrip.json").toFile();
      TestConfig original = new TestConfig("roundtrip", 42, List.of("a", "b", "c"));

      loader.save(file, original);
      TestConfig loaded = loader.load(file, TestConfig.class);

      assertEquals(original.name, loaded.name);
      assertEquals(original.value, loaded.value);
      assertEquals(original.items, loaded.items);
    }

    @Test
    @DisplayName("should roundtrip config through string")
    void shouldRoundtripThroughString() {
      TestConfig original = new TestConfig("string-roundtrip", 100, List.of("x", "y"));

      String json = loader.saveToString(original);
      TestConfig loaded = loader.loadFromString(json, TestConfig.class);

      assertEquals(original.name, loaded.name);
      assertEquals(original.value, loaded.value);
      assertEquals(original.items, loaded.items);
    }

    @Test
    @DisplayName("should roundtrip between typed and untyped")
    void shouldRoundtripBetweenTypedAndUntyped() throws IOException {
      File file = tempDir.resolve("mixed-roundtrip.json").toFile();
      TestConfig original = new TestConfig("mixed", 50, List.of("item1"));

      // Save as typed
      loader.save(file, original);

      // Load as node
      ConfigNode node = loader.load(file);
      assertEquals("mixed", node.getString("name"));
      assertEquals(50, node.getInt("value"));

      // Convert node to typed
      TestConfig loaded = node.as(TestConfig.class);
      assertEquals(original.name, loaded.name);
      assertEquals(original.value, loaded.value);
    }
  }
}
