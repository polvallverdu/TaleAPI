package dev.polv.taleapi.config;

import java.io.Reader;
import java.io.Writer;

/**
 * Interface for configuration format providers.
 * <p>
 * Implementations handle serialization and deserialization for specific formats
 * (e.g., JSON, YAML, TOML). Each provider manages a single format type.
 * </p>
 *
 * <h2>Example Implementation</h2>
 * <pre>{@code
 * public class JsonProvider implements ConfigProvider {
 *   @Override
 *   public <T> T deserialize(Reader reader, Class<T> type) throws ConfigException {
 *     // Parse JSON and return typed object
 *   }
 *
 *   @Override
 *   public ConfigNode deserializeToNode(Reader reader) throws ConfigException {
 *     // Parse JSON and return a traversable node
 *   }
 *
 *   @Override
 *   public <T> void serialize(Writer writer, T value) throws ConfigException {
 *     // Write object as JSON
 *   }
 *
 *   @Override
 *   public String fileExtension() {
 *     return "json";
 *   }
 * }
 * }</pre>
 *
 * @see ConfigLoader
 * @see ConfigNode
 * @see dev.polv.taleapi.config.json.JsonProvider
 */
public interface ConfigProvider {

  /**
   * Deserializes configuration data from a reader into an object of the specified
   * type.
   *
   * @param reader the reader to read configuration data from
   * @param type   the class of the object to deserialize into
   * @param <T>    the type of the configuration object
   * @return the deserialized configuration object
   * @throws ConfigException if deserialization fails
   */
  <T> T deserialize(Reader reader, Class<T> type) throws ConfigException;

  /**
   * Deserializes configuration data from a reader into a traversable
   * {@link ConfigNode}.
   * <p>
   * Use this when you want dynamic access to configuration values without
   * defining a specific class structure.
   * </p>
   *
   * @param reader the reader to read configuration data from
   * @return the configuration as a traversable node
   * @throws ConfigException if deserialization fails
   */
  ConfigNode deserializeToNode(Reader reader) throws ConfigException;

  /**
   * Serializes a configuration object and writes it to the given writer.
   *
   * @param writer the writer to write the serialized data to
   * @param value  the configuration object to serialize
   * @param <T>    the type of the configuration object
   * @throws ConfigException if serialization fails
   */
  <T> void serialize(Writer writer, T value) throws ConfigException;

  /**
   * Serializes a {@link ConfigNode} and writes it to the given writer.
   *
   * @param writer the writer to write the serialized data to
   * @param node   the configuration node to serialize
   * @throws ConfigException if serialization fails
   */
  void serialize(Writer writer, ConfigNode node) throws ConfigException;

  /**
   * Returns the file extension associated with this provider's format.
   * <p>
   * The extension should not include the leading dot (e.g., "json" not ".json").
   * </p>
   *
   * @return the file extension for this format
   */
  String fileExtension();

  /**
   * Returns a human-readable name for this format.
   *
   * @return the format name (e.g., "JSON", "YAML")
   */
  default String formatName() {
    return fileExtension().toUpperCase();
  }
}
