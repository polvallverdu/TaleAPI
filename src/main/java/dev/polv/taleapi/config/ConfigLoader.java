package dev.polv.taleapi.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * Main entry point for loading and saving configuration files.
 * <p>
 * ConfigLoader provides both synchronous and asynchronous operations for
 * reading and writing configuration files. It uses {@link ConfigProvider}
 * implementations to handle different file formats.
 * </p>
 * <p>
 * Two loading modes are supported:
 * </p>
 * <ul>
 * <li><b>Object mapping</b>: Load directly into a typed class</li>
 * <li><b>Dynamic access</b>: Load into a {@link ConfigNode} for key-based
 * traversal</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * 
 * <pre>{@code
 * ConfigLoader loader = new ConfigLoader(new JsonProvider());
 *
 * // Object mapping - load into a typed class
 * MyConfig config = loader.load(new File("config.json"), MyConfig.class);
 *
 * // Dynamic access - load into a traversable node
 * ConfigNode node = loader.load(new File("config.json"));
 * String name = node.getString("serverName");
 * int port = node.getInt("database.port", 5432);
 *
 * // Async operations
 * loader.loadAsync(file, MyConfig.class)
 *     .thenAccept(cfg -> System.out.println("Loaded: " + cfg));
 *
 * // Save configuration
 * loader.save(new File("config.json"), config);
 * }</pre>
 *
 * @see ConfigProvider
 * @see ConfigNode
 */
public class ConfigLoader {

  private final ConfigProvider provider;
  private final Executor defaultExecutor;

  /**
   * Creates a new ConfigLoader with the given provider.
   * <p>
   * Uses the common ForkJoinPool for async operations by default.
   * </p>
   *
   * @param provider the configuration provider to use for serialization
   * @throws NullPointerException if provider is null
   */
  public ConfigLoader(ConfigProvider provider) {
    this(provider, ForkJoinPool.commonPool());
  }

  /**
   * Creates a new ConfigLoader with the given provider and executor.
   *
   * @param provider        the configuration provider to use for serialization
   * @param defaultExecutor the default executor for async operations
   * @throws NullPointerException if provider or defaultExecutor is null
   */
  public ConfigLoader(ConfigProvider provider, Executor defaultExecutor) {
    this.provider = Objects.requireNonNull(provider, "provider");
    this.defaultExecutor = Objects.requireNonNull(defaultExecutor, "defaultExecutor");
  }

  /**
   * Returns the configuration provider used by this loader.
   *
   * @return the configuration provider
   */
  public ConfigProvider getProvider() {
    return provider;
  }

  // ========== Synchronous Load to Object ==========

  /**
   * Loads configuration from a file into a typed object.
   *
   * @param file the file to load from
   * @param type the configuration class type
   * @param <T>  the configuration type
   * @return the loaded configuration object
   * @throws ConfigException      if loading or parsing fails
   * @throws NullPointerException if file or type is null
   */
  public <T> T load(File file, Class<T> type) {
    Objects.requireNonNull(file, "file");
    Objects.requireNonNull(type, "type");

    try (BufferedReader reader = new BufferedReader(
        new FileReader(file, StandardCharsets.UTF_8))) {
      return provider.deserialize(reader, type);
    } catch (IOException e) {
      throw new ConfigException("Failed to load configuration from: " + file.getPath(), e);
    }
  }

  /**
   * Loads configuration from a path into a typed object.
   *
   * @param path the path to load from
   * @param type the configuration class type
   * @param <T>  the configuration type
   * @return the loaded configuration object
   * @throws ConfigException      if loading or parsing fails
   * @throws NullPointerException if path or type is null
   */
  public <T> T load(Path path, Class<T> type) {
    Objects.requireNonNull(path, "path");
    return load(path.toFile(), type);
  }

  /**
   * Loads configuration from a reader into a typed object.
   *
   * @param reader the reader to load from
   * @param type   the configuration class type
   * @param <T>    the configuration type
   * @return the loaded configuration object
   * @throws ConfigException      if parsing fails
   * @throws NullPointerException if reader or type is null
   */
  public <T> T load(Reader reader, Class<T> type) {
    Objects.requireNonNull(reader, "reader");
    Objects.requireNonNull(type, "type");
    return provider.deserialize(reader, type);
  }

  /**
   * Loads configuration from a string into a typed object.
   *
   * @param content the string content to parse
   * @param type    the configuration class type
   * @param <T>     the configuration type
   * @return the loaded configuration object
   * @throws ConfigException      if parsing fails
   * @throws NullPointerException if content or type is null
   */
  public <T> T loadFromString(String content, Class<T> type) {
    Objects.requireNonNull(content, "content");
    return load(new StringReader(content), type);
  }

  // ========== Synchronous Load to ConfigNode ==========

  /**
   * Loads configuration from a file into a traversable {@link ConfigNode}.
   * <p>
   * Use this when you want dynamic access to configuration values without
   * defining a specific class structure.
   * </p>
   *
   * @param file the file to load from
   * @return the configuration as a traversable node
   * @throws ConfigException      if loading or parsing fails
   * @throws NullPointerException if file is null
   */
  public ConfigNode load(File file) {
    Objects.requireNonNull(file, "file");

    try (BufferedReader reader = new BufferedReader(
        new FileReader(file, StandardCharsets.UTF_8))) {
      return provider.deserializeToNode(reader);
    } catch (IOException e) {
      throw new ConfigException("Failed to load configuration from: " + file.getPath(), e);
    }
  }

  /**
   * Loads configuration from a path into a traversable {@link ConfigNode}.
   *
   * @param path the path to load from
   * @return the configuration as a traversable node
   * @throws ConfigException      if loading or parsing fails
   * @throws NullPointerException if path is null
   */
  public ConfigNode load(Path path) {
    Objects.requireNonNull(path, "path");
    return load(path.toFile());
  }

  /**
   * Loads configuration from a reader into a traversable {@link ConfigNode}.
   *
   * @param reader the reader to load from
   * @return the configuration as a traversable node
   * @throws ConfigException      if parsing fails
   * @throws NullPointerException if reader is null
   */
  public ConfigNode loadNode(Reader reader) {
    Objects.requireNonNull(reader, "reader");
    return provider.deserializeToNode(reader);
  }

  /**
   * Loads configuration from a string into a traversable {@link ConfigNode}.
   *
   * @param content the string content to parse
   * @return the configuration as a traversable node
   * @throws ConfigException      if parsing fails
   * @throws NullPointerException if content is null
   */
  public ConfigNode loadNodeFromString(String content) {
    Objects.requireNonNull(content, "content");
    return loadNode(new StringReader(content));
  }

  // ========== Asynchronous Load to Object ==========

  /**
   * Loads configuration from a file asynchronously using the default executor.
   *
   * @param file the file to load from
   * @param type the configuration class type
   * @param <T>  the configuration type
   * @return a CompletableFuture that completes with the loaded configuration
   * @throws NullPointerException if file or type is null
   */
  public <T> CompletableFuture<T> loadAsync(File file, Class<T> type) {
    return loadAsync(file, type, defaultExecutor);
  }

  /**
   * Loads configuration from a file asynchronously using the specified executor.
   *
   * @param file     the file to load from
   * @param type     the configuration class type
   * @param executor the executor to use
   * @param <T>      the configuration type
   * @return a CompletableFuture that completes with the loaded configuration
   * @throws NullPointerException if any argument is null
   */
  public <T> CompletableFuture<T> loadAsync(File file, Class<T> type, Executor executor) {
    Objects.requireNonNull(file, "file");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(executor, "executor");
    return CompletableFuture.supplyAsync(() -> load(file, type), executor);
  }

  /**
   * Loads configuration from a path asynchronously using the default executor.
   *
   * @param path the path to load from
   * @param type the configuration class type
   * @param <T>  the configuration type
   * @return a CompletableFuture that completes with the loaded configuration
   * @throws NullPointerException if path or type is null
   */
  public <T> CompletableFuture<T> loadAsync(Path path, Class<T> type) {
    return loadAsync(path, type, defaultExecutor);
  }

  /**
   * Loads configuration from a path asynchronously using the specified executor.
   *
   * @param path     the path to load from
   * @param type     the configuration class type
   * @param executor the executor to use
   * @param <T>      the configuration type
   * @return a CompletableFuture that completes with the loaded configuration
   * @throws NullPointerException if any argument is null
   */
  public <T> CompletableFuture<T> loadAsync(Path path, Class<T> type, Executor executor) {
    Objects.requireNonNull(path, "path");
    return loadAsync(path.toFile(), type, executor);
  }

  // ========== Asynchronous Load to ConfigNode ==========

  /**
   * Loads configuration from a file into a {@link ConfigNode} asynchronously.
   *
   * @param file the file to load from
   * @return a CompletableFuture that completes with the ConfigNode
   * @throws NullPointerException if file is null
   */
  public CompletableFuture<ConfigNode> loadAsync(File file) {
    return loadAsync(file, defaultExecutor);
  }

  /**
   * Loads configuration from a file into a {@link ConfigNode} asynchronously.
   *
   * @param file     the file to load from
   * @param executor the executor to use
   * @return a CompletableFuture that completes with the ConfigNode
   * @throws NullPointerException if any argument is null
   */
  public CompletableFuture<ConfigNode> loadAsync(File file, Executor executor) {
    Objects.requireNonNull(file, "file");
    Objects.requireNonNull(executor, "executor");
    return CompletableFuture.supplyAsync(() -> load(file), executor);
  }

  /**
   * Loads configuration from a path into a {@link ConfigNode} asynchronously.
   *
   * @param path the path to load from
   * @return a CompletableFuture that completes with the ConfigNode
   * @throws NullPointerException if path is null
   */
  public CompletableFuture<ConfigNode> loadAsync(Path path) {
    return loadAsync(path, defaultExecutor);
  }

  /**
   * Loads configuration from a path into a {@link ConfigNode} asynchronously.
   *
   * @param path     the path to load from
   * @param executor the executor to use
   * @return a CompletableFuture that completes with the ConfigNode
   * @throws NullPointerException if any argument is null
   */
  public CompletableFuture<ConfigNode> loadAsync(Path path, Executor executor) {
    Objects.requireNonNull(path, "path");
    return loadAsync(path.toFile(), executor);
  }

  // ========== Synchronous Save Operations ==========

  /**
   * Saves configuration to a file.
   * <p>
   * Creates parent directories if they don't exist.
   * </p>
   *
   * @param file  the file to save to
   * @param value the configuration object to save
   * @param <T>   the configuration type
   * @throws ConfigException      if saving fails
   * @throws NullPointerException if file or value is null
   */
  public <T> void save(File file, T value) {
    Objects.requireNonNull(file, "file");
    Objects.requireNonNull(value, "value");

    createParentDirs(file);

    try (BufferedWriter writer = new BufferedWriter(
        new FileWriter(file, StandardCharsets.UTF_8))) {
      provider.serialize(writer, value);
    } catch (IOException e) {
      throw new ConfigException("Failed to save configuration to: " + file.getPath(), e);
    }
  }

  /**
   * Saves configuration to a path.
   * <p>
   * Creates parent directories if they don't exist.
   * </p>
   *
   * @param path  the path to save to
   * @param value the configuration object to save
   * @param <T>   the configuration type
   * @throws ConfigException      if saving fails
   * @throws NullPointerException if path or value is null
   */
  public <T> void save(Path path, T value) {
    Objects.requireNonNull(path, "path");
    save(path.toFile(), value);
  }

  /**
   * Saves configuration to a writer.
   *
   * @param writer the writer to save to
   * @param value  the configuration object to save
   * @param <T>    the configuration type
   * @throws ConfigException      if serialization fails
   * @throws NullPointerException if writer or value is null
   */
  public <T> void save(Writer writer, T value) {
    Objects.requireNonNull(writer, "writer");
    Objects.requireNonNull(value, "value");
    provider.serialize(writer, value);
  }

  /**
   * Saves a {@link ConfigNode} to a file.
   * <p>
   * Creates parent directories if they don't exist.
   * </p>
   *
   * @param file the file to save to
   * @param node the configuration node to save
   * @throws ConfigException      if saving fails
   * @throws NullPointerException if file or node is null
   */
  public void save(File file, ConfigNode node) {
    Objects.requireNonNull(file, "file");
    Objects.requireNonNull(node, "node");

    createParentDirs(file);

    try (BufferedWriter writer = new BufferedWriter(
        new FileWriter(file, StandardCharsets.UTF_8))) {
      provider.serialize(writer, node);
    } catch (IOException e) {
      throw new ConfigException("Failed to save configuration to: " + file.getPath(), e);
    }
  }

  /**
   * Saves a {@link ConfigNode} to a path.
   *
   * @param path the path to save to
   * @param node the configuration node to save
   * @throws ConfigException      if saving fails
   * @throws NullPointerException if path or node is null
   */
  public void save(Path path, ConfigNode node) {
    Objects.requireNonNull(path, "path");
    save(path.toFile(), node);
  }

  /**
   * Serializes a configuration object to a string.
   *
   * @param value the configuration object to serialize
   * @param <T>   the configuration type
   * @return the serialized string
   * @throws ConfigException      if serialization fails
   * @throws NullPointerException if value is null
   */
  public <T> String saveToString(T value) {
    Objects.requireNonNull(value, "value");
    StringWriter writer = new StringWriter();
    provider.serialize(writer, value);
    return writer.toString();
  }

  // ========== Asynchronous Save Operations ==========

  /**
   * Saves configuration to a file asynchronously using the default executor.
   *
   * @param file  the file to save to
   * @param value the configuration object to save
   * @param <T>   the configuration type
   * @return a CompletableFuture that completes when saving is done
   * @throws NullPointerException if file or value is null
   */
  public <T> CompletableFuture<Void> saveAsync(File file, T value) {
    return saveAsync(file, value, defaultExecutor);
  }

  /**
   * Saves configuration to a file asynchronously using the specified executor.
   *
   * @param file     the file to save to
   * @param value    the configuration object to save
   * @param executor the executor to use
   * @param <T>      the configuration type
   * @return a CompletableFuture that completes when saving is done
   * @throws NullPointerException if any argument is null
   */
  public <T> CompletableFuture<Void> saveAsync(File file, T value, Executor executor) {
    Objects.requireNonNull(file, "file");
    Objects.requireNonNull(value, "value");
    Objects.requireNonNull(executor, "executor");
    return CompletableFuture.runAsync(() -> save(file, value), executor);
  }

  /**
   * Saves configuration to a path asynchronously using the default executor.
   *
   * @param path  the path to save to
   * @param value the configuration object to save
   * @param <T>   the configuration type
   * @return a CompletableFuture that completes when saving is done
   * @throws NullPointerException if path or value is null
   */
  public <T> CompletableFuture<Void> saveAsync(Path path, T value) {
    return saveAsync(path, value, defaultExecutor);
  }

  /**
   * Saves configuration to a path asynchronously using the specified executor.
   *
   * @param path     the path to save to
   * @param value    the configuration object to save
   * @param executor the executor to use
   * @param <T>      the configuration type
   * @return a CompletableFuture that completes when saving is done
   * @throws NullPointerException if any argument is null
   */
  public <T> CompletableFuture<Void> saveAsync(Path path, T value, Executor executor) {
    Objects.requireNonNull(path, "path");
    return saveAsync(path.toFile(), value, executor);
  }

  // ========== Utility Methods ==========

  /**
   * Loads configuration from a file, or returns a default value if the file
   * doesn't exist.
   * <p>
   * If the file doesn't exist, the default value is saved to the file and
   * returned.
   * </p>
   *
   * @param file         the file to load from
   * @param type         the configuration class type
   * @param defaultValue the default value to use if file doesn't exist
   * @param <T>          the configuration type
   * @return the loaded configuration or the default value
   * @throws ConfigException      if loading fails (but not if file doesn't exist)
   * @throws NullPointerException if any argument is null
   */
  public <T> T loadOrDefault(File file, Class<T> type, T defaultValue) {
    Objects.requireNonNull(file, "file");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(defaultValue, "defaultValue");

    if (!file.exists()) {
      save(file, defaultValue);
      return defaultValue;
    }
    return load(file, type);
  }

  /**
   * Loads configuration from a path, or returns a default value if the file
   * doesn't exist.
   * <p>
   * If the file doesn't exist, the default value is saved to the file and
   * returned.
   * </p>
   *
   * @param path         the path to load from
   * @param type         the configuration class type
   * @param defaultValue the default value to use if file doesn't exist
   * @param <T>          the configuration type
   * @return the loaded configuration or the default value
   * @throws ConfigException      if loading fails (but not if file doesn't exist)
   * @throws NullPointerException if any argument is null
   */
  public <T> T loadOrDefault(Path path, Class<T> type, T defaultValue) {
    Objects.requireNonNull(path, "path");
    return loadOrDefault(path.toFile(), type, defaultValue);
  }

  /**
   * Checks if a file exists and can be loaded as the specified type.
   *
   * @param file the file to check
   * @param type the configuration class type
   * @param <T>  the configuration type
   * @return true if the file exists and can be parsed, false otherwise
   */
  public <T> boolean isValid(File file, Class<T> type) {
    Objects.requireNonNull(file, "file");
    Objects.requireNonNull(type, "type");

    if (!file.exists() || !file.isFile()) {
      return false;
    }
    try {
      load(file, type);
      return true;
    } catch (ConfigException e) {
      return false;
    }
  }

  /**
   * Checks if a path exists and can be loaded as the specified type.
   *
   * @param path the path to check
   * @param type the configuration class type
   * @param <T>  the configuration type
   * @return true if the file exists and can be parsed, false otherwise
   */
  public <T> boolean isValid(Path path, Class<T> type) {
    Objects.requireNonNull(path, "path");
    return isValid(path.toFile(), type);
  }

  /**
   * Checks if a file exists and can be loaded as a ConfigNode.
   *
   * @param file the file to check
   * @return true if the file exists and can be parsed, false otherwise
   */
  public boolean isValid(File file) {
    Objects.requireNonNull(file, "file");

    if (!file.exists() || !file.isFile()) {
      return false;
    }
    try {
      load(file);
      return true;
    } catch (ConfigException e) {
      return false;
    }
  }

  /**
   * Checks if a path exists and can be loaded as a ConfigNode.
   *
   * @param path the path to check
   * @return true if the file exists and can be parsed, false otherwise
   */
  public boolean isValid(Path path) {
    Objects.requireNonNull(path, "path");
    return isValid(path.toFile());
  }

  private void createParentDirs(File file) {
    File parent = file.getParentFile();
    if (parent != null && !parent.exists()) {
      if (!parent.mkdirs()) {
        throw new ConfigException("Failed to create directories: " + parent.getPath());
      }
    }
  }
}
