package dev.polv.taleapi.config;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A node in a configuration tree that provides dynamic access to values.
 * <p>
 * ConfigNode allows traversing and reading configuration data without
 * defining a specific class structure. It supports nested objects,
 * arrays, and all common primitive types.
 * </p>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * ConfigNode config = loader.load(new File("config.json"));
 *
 * // Get simple values
 * String name = config.getString("serverName");
 * int port = config.getInt("port", 8080);  // with default
 *
 * // Navigate nested objects
 * ConfigNode database = config.getSection("database");
 * String host = database.getString("host");
 *
 * // Or use dot notation
 * String host = config.getString("database.host");
 *
 * // Work with lists
 * List<String> mods = config.getStringList("enabledMods");
 *
 * // Check existence
 * if (config.has("optionalFeature")) {
 *     // ...
 * }
 * }</pre>
 *
 * @see ConfigLoader
 */
public interface ConfigNode {

  // ========== Type Checking ==========

  /**
   * Checks if this node represents a JSON object (key-value pairs).
   *
   * @return true if this node is an object
   */
  boolean isObject();

  /**
   * Checks if this node represents a JSON array.
   *
   * @return true if this node is an array
   */
  boolean isArray();

  /**
   * Checks if this node represents a primitive value (string, number, boolean).
   *
   * @return true if this node is a value
   */
  boolean isValue();

  /**
   * Checks if this node is null or missing.
   *
   * @return true if this node is null
   */
  boolean isNull();

  // ========== Key Operations ==========

  /**
   * Checks if a key exists in this object node.
   * <p>
   * Supports dot notation for nested paths: {@code "database.host"}
   * </p>
   *
   * @param key the key or path to check
   * @return true if the key exists
   */
  boolean has(String key);

  /**
   * Returns all keys in this object node.
   *
   * @return set of keys, or empty set if not an object
   */
  Set<String> keys();

  /**
   * Returns the number of elements in this node.
   * <p>
   * For objects, returns the number of keys.
   * For arrays, returns the number of elements.
   * For values, returns 1.
   * </p>
   *
   * @return the size of this node
   */
  int size();

  // ========== Navigation ==========

  /**
   * Gets a child node by key.
   * <p>
   * Supports dot notation for nested paths: {@code "database.host"}
   * </p>
   *
   * @param key the key or path
   * @return the child node, or a null node if not found
   */
  ConfigNode get(String key);

  /**
   * Gets a child node by array index.
   *
   * @param index the array index
   * @return the child node, or a null node if out of bounds
   */
  ConfigNode get(int index);

  /**
   * Gets a nested object section by key.
   * <p>
   * Equivalent to {@link #get(String)} but more readable for object access.
   * </p>
   *
   * @param key the key or path
   * @return the section node, or a null node if not found
   */
  default ConfigNode getSection(String key) {
    return get(key);
  }

  // ========== String Values ==========

  /**
   * Gets a string value by key.
   *
   * @param key the key or path
   * @return the string value
   * @throws ConfigException if the key doesn't exist or isn't a string
   */
  String getString(String key);

  /**
   * Gets a string value by key, with a default.
   *
   * @param key          the key or path
   * @param defaultValue the default value if key doesn't exist
   * @return the string value or default
   */
  String getString(String key, String defaultValue);

  /**
   * Gets an optional string value by key.
   *
   * @param key the key or path
   * @return an Optional containing the value, or empty if not found
   */
  Optional<String> getOptionalString(String key);

  // ========== Integer Values ==========

  /**
   * Gets an integer value by key.
   *
   * @param key the key or path
   * @return the integer value
   * @throws ConfigException if the key doesn't exist or isn't a number
   */
  int getInt(String key);

  /**
   * Gets an integer value by key, with a default.
   *
   * @param key          the key or path
   * @param defaultValue the default value if key doesn't exist
   * @return the integer value or default
   */
  int getInt(String key, int defaultValue);

  /**
   * Gets an optional integer value by key.
   *
   * @param key the key or path
   * @return an Optional containing the value, or empty if not found
   */
  Optional<Integer> getOptionalInt(String key);

  // ========== Long Values ==========

  /**
   * Gets a long value by key.
   *
   * @param key the key or path
   * @return the long value
   * @throws ConfigException if the key doesn't exist or isn't a number
   */
  long getLong(String key);

  /**
   * Gets a long value by key, with a default.
   *
   * @param key          the key or path
   * @param defaultValue the default value if key doesn't exist
   * @return the long value or default
   */
  long getLong(String key, long defaultValue);

  /**
   * Gets an optional long value by key.
   *
   * @param key the key or path
   * @return an Optional containing the value, or empty if not found
   */
  Optional<Long> getOptionalLong(String key);

  // ========== Double Values ==========

  /**
   * Gets a double value by key.
   *
   * @param key the key or path
   * @return the double value
   * @throws ConfigException if the key doesn't exist or isn't a number
   */
  double getDouble(String key);

  /**
   * Gets a double value by key, with a default.
   *
   * @param key          the key or path
   * @param defaultValue the default value if key doesn't exist
   * @return the double value or default
   */
  double getDouble(String key, double defaultValue);

  /**
   * Gets an optional double value by key.
   *
   * @param key the key or path
   * @return an Optional containing the value, or empty if not found
   */
  Optional<Double> getOptionalDouble(String key);

  // ========== Boolean Values ==========

  /**
   * Gets a boolean value by key.
   *
   * @param key the key or path
   * @return the boolean value
   * @throws ConfigException if the key doesn't exist or isn't a boolean
   */
  boolean getBoolean(String key);

  /**
   * Gets a boolean value by key, with a default.
   *
   * @param key          the key or path
   * @param defaultValue the default value if key doesn't exist
   * @return the boolean value or default
   */
  boolean getBoolean(String key, boolean defaultValue);

  /**
   * Gets an optional boolean value by key.
   *
   * @param key the key or path
   * @return an Optional containing the value, or empty if not found
   */
  Optional<Boolean> getOptionalBoolean(String key);

  // ========== List Values ==========

  /**
   * Gets a list of child nodes by key.
   *
   * @param key the key or path
   * @return list of ConfigNode elements
   */
  List<ConfigNode> getList(String key);

  /**
   * Gets a list of strings by key.
   *
   * @param key the key or path
   * @return list of strings
   */
  List<String> getStringList(String key);

  /**
   * Gets a list of integers by key.
   *
   * @param key the key or path
   * @return list of integers
   */
  List<Integer> getIntList(String key);

  // ========== Object Mapping ==========

  /**
   * Converts this node to a typed object.
   *
   * @param type the class to convert to
   * @param <T>  the type
   * @return the converted object
   * @throws ConfigException if conversion fails
   */
  <T> T as(Class<T> type);

  /**
   * Converts a child node to a typed object.
   *
   * @param key  the key or path
   * @param type the class to convert to
   * @param <T>  the type
   * @return the converted object
   * @throws ConfigException if key doesn't exist or conversion fails
   */
  <T> T get(String key, Class<T> type);

  // ========== Raw Value Access ==========

  /**
   * Gets this node's value as a raw Object.
   * <p>
   * Returns:
   * <ul>
   *   <li>String, Number, Boolean for primitives</li>
   *   <li>List for arrays</li>
   *   <li>Map for objects</li>
   *   <li>null for null nodes</li>
   * </ul>
   * </p>
   *
   * @return the raw value
   */
  Object rawValue();

  /**
   * Returns the JSON string representation of this node.
   *
   * @return JSON string
   */
  String toJson();

  /**
   * Returns the pretty-printed JSON string representation of this node.
   *
   * @return pretty-printed JSON string
   */
  String toPrettyJson();
}

