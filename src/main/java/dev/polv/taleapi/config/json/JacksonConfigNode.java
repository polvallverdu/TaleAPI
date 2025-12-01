package dev.polv.taleapi.config.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.polv.taleapi.config.ConfigException;
import dev.polv.taleapi.config.ConfigNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Jackson-based implementation of {@link ConfigNode}.
 */
public class JacksonConfigNode implements ConfigNode {

  private final JsonNode node;
  private final ObjectMapper mapper;

  /**
   * Creates a new JacksonConfigNode wrapping the given JsonNode.
   *
   * @param node   the Jackson JsonNode
   * @param mapper the ObjectMapper for conversions
   */
  public JacksonConfigNode(JsonNode node, ObjectMapper mapper) {
    this.node = node != null ? node : NullNode.getInstance();
    this.mapper = Objects.requireNonNull(mapper, "mapper");
  }

  /**
   * Returns the underlying Jackson JsonNode.
   *
   * @return the JsonNode
   */
  public JsonNode getJsonNode() {
    return node;
  }

  // ========== Type Checking ==========

  @Override
  public boolean isObject() {
    return node.isObject();
  }

  @Override
  public boolean isArray() {
    return node.isArray();
  }

  @Override
  public boolean isValue() {
    return node.isValueNode();
  }

  @Override
  public boolean isNull() {
    return node.isNull() || node.isMissingNode();
  }

  // ========== Key Operations ==========

  @Override
  public boolean has(String key) {
    Objects.requireNonNull(key, "key");
    JsonNode child = navigate(key);
    return !child.isNull() && !child.isMissingNode();
  }

  @Override
  public Set<String> keys() {
    if (!node.isObject()) {
      return Collections.emptySet();
    }
    Set<String> keys = new HashSet<>();
    node.fieldNames().forEachRemaining(keys::add);
    return keys;
  }

  @Override
  public int size() {
    return node.size();
  }

  // ========== Navigation ==========

  @Override
  public ConfigNode get(String key) {
    Objects.requireNonNull(key, "key");
    return new JacksonConfigNode(navigate(key), mapper);
  }

  @Override
  public ConfigNode get(int index) {
    if (!node.isArray() || index < 0 || index >= node.size()) {
      return new JacksonConfigNode(NullNode.getInstance(), mapper);
    }
    return new JacksonConfigNode(node.get(index), mapper);
  }

  /**
   * Navigates to a nested node using dot notation.
   */
  private JsonNode navigate(String path) {
    if (!path.contains(".")) {
      return node.path(path);
    }

    String[] parts = path.split("\\.");
    JsonNode current = node;
    for (String part : parts) {
      current = current.path(part);
      if (current.isMissingNode()) {
        return current;
      }
    }
    return current;
  }

  // ========== String Values ==========

  @Override
  public String getString(String key) {
    JsonNode child = navigate(key);
    if (child.isMissingNode() || child.isNull()) {
      throw new ConfigException("Missing required key: " + key);
    }
    return child.asText();
  }

  @Override
  public String getString(String key, String defaultValue) {
    JsonNode child = navigate(key);
    if (child.isMissingNode() || child.isNull()) {
      return defaultValue;
    }
    return child.asText();
  }

  @Override
  public Optional<String> getOptionalString(String key) {
    JsonNode child = navigate(key);
    if (child.isMissingNode() || child.isNull()) {
      return Optional.empty();
    }
    return Optional.of(child.asText());
  }

  // ========== Integer Values ==========

  @Override
  public int getInt(String key) {
    JsonNode child = navigate(key);
    if (child.isMissingNode() || child.isNull()) {
      throw new ConfigException("Missing required key: " + key);
    }
    if (!child.isNumber()) {
      throw new ConfigException("Key '" + key + "' is not a number");
    }
    return child.asInt();
  }

  @Override
  public int getInt(String key, int defaultValue) {
    JsonNode child = navigate(key);
    if (child.isMissingNode() || child.isNull()) {
      return defaultValue;
    }
    return child.asInt(defaultValue);
  }

  @Override
  public Optional<Integer> getOptionalInt(String key) {
    JsonNode child = navigate(key);
    if (child.isMissingNode() || child.isNull()) {
      return Optional.empty();
    }
    return Optional.of(child.asInt());
  }

  // ========== Long Values ==========

  @Override
  public long getLong(String key) {
    JsonNode child = navigate(key);
    if (child.isMissingNode() || child.isNull()) {
      throw new ConfigException("Missing required key: " + key);
    }
    if (!child.isNumber()) {
      throw new ConfigException("Key '" + key + "' is not a number");
    }
    return child.asLong();
  }

  @Override
  public long getLong(String key, long defaultValue) {
    JsonNode child = navigate(key);
    if (child.isMissingNode() || child.isNull()) {
      return defaultValue;
    }
    return child.asLong(defaultValue);
  }

  @Override
  public Optional<Long> getOptionalLong(String key) {
    JsonNode child = navigate(key);
    if (child.isMissingNode() || child.isNull()) {
      return Optional.empty();
    }
    return Optional.of(child.asLong());
  }

  // ========== Double Values ==========

  @Override
  public double getDouble(String key) {
    JsonNode child = navigate(key);
    if (child.isMissingNode() || child.isNull()) {
      throw new ConfigException("Missing required key: " + key);
    }
    if (!child.isNumber()) {
      throw new ConfigException("Key '" + key + "' is not a number");
    }
    return child.asDouble();
  }

  @Override
  public double getDouble(String key, double defaultValue) {
    JsonNode child = navigate(key);
    if (child.isMissingNode() || child.isNull()) {
      return defaultValue;
    }
    return child.asDouble(defaultValue);
  }

  @Override
  public Optional<Double> getOptionalDouble(String key) {
    JsonNode child = navigate(key);
    if (child.isMissingNode() || child.isNull()) {
      return Optional.empty();
    }
    return Optional.of(child.asDouble());
  }

  // ========== Boolean Values ==========

  @Override
  public boolean getBoolean(String key) {
    JsonNode child = navigate(key);
    if (child.isMissingNode() || child.isNull()) {
      throw new ConfigException("Missing required key: " + key);
    }
    if (!child.isBoolean()) {
      throw new ConfigException("Key '" + key + "' is not a boolean");
    }
    return child.asBoolean();
  }

  @Override
  public boolean getBoolean(String key, boolean defaultValue) {
    JsonNode child = navigate(key);
    if (child.isMissingNode() || child.isNull()) {
      return defaultValue;
    }
    return child.asBoolean(defaultValue);
  }

  @Override
  public Optional<Boolean> getOptionalBoolean(String key) {
    JsonNode child = navigate(key);
    if (child.isMissingNode() || child.isNull()) {
      return Optional.empty();
    }
    return Optional.of(child.asBoolean());
  }

  // ========== List Values ==========

  @Override
  public List<ConfigNode> getList(String key) {
    JsonNode child = navigate(key);
    if (child.isMissingNode() || child.isNull() || !child.isArray()) {
      return Collections.emptyList();
    }
    List<ConfigNode> result = new ArrayList<>(child.size());
    for (JsonNode element : child) {
      result.add(new JacksonConfigNode(element, mapper));
    }
    return result;
  }

  @Override
  public List<String> getStringList(String key) {
    JsonNode child = navigate(key);
    if (child.isMissingNode() || child.isNull() || !child.isArray()) {
      return Collections.emptyList();
    }
    List<String> result = new ArrayList<>(child.size());
    for (JsonNode element : child) {
      result.add(element.asText());
    }
    return result;
  }

  @Override
  public List<Integer> getIntList(String key) {
    JsonNode child = navigate(key);
    if (child.isMissingNode() || child.isNull() || !child.isArray()) {
      return Collections.emptyList();
    }
    List<Integer> result = new ArrayList<>(child.size());
    for (JsonNode element : child) {
      result.add(element.asInt());
    }
    return result;
  }

  // ========== Object Mapping ==========

  @Override
  public <T> T as(Class<T> type) {
    Objects.requireNonNull(type, "type");
    try {
      return mapper.treeToValue(node, type);
    } catch (JsonProcessingException e) {
      throw new ConfigException("Failed to convert to " + type.getName(), e);
    }
  }

  @Override
  public <T> T get(String key, Class<T> type) {
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(type, "type");
    JsonNode child = navigate(key);
    if (child.isMissingNode() || child.isNull()) {
      throw new ConfigException("Missing required key: " + key);
    }
    try {
      return mapper.treeToValue(child, type);
    } catch (JsonProcessingException e) {
      throw new ConfigException("Failed to convert '" + key + "' to " + type.getName(), e);
    }
  }

  // ========== Raw Value Access ==========

  @Override
  public Object rawValue() {
    if (node.isNull() || node.isMissingNode()) {
      return null;
    }
    if (node.isTextual()) {
      return node.asText();
    }
    if (node.isInt()) {
      return node.asInt();
    }
    if (node.isLong()) {
      return node.asLong();
    }
    if (node.isDouble() || node.isFloat()) {
      return node.asDouble();
    }
    if (node.isBoolean()) {
      return node.asBoolean();
    }
    if (node.isArray()) {
      List<Object> list = new ArrayList<>(node.size());
      for (JsonNode element : node) {
        list.add(new JacksonConfigNode(element, mapper).rawValue());
      }
      return list;
    }
    if (node.isObject()) {
      try {
        return mapper.treeToValue(node, Map.class);
      } catch (JsonProcessingException e) {
        throw new ConfigException("Failed to convert to Map", e);
      }
    }
    return node.asText();
  }

  @Override
  public String toJson() {
    try {
      return mapper.writeValueAsString(node);
    } catch (JsonProcessingException e) {
      throw new ConfigException("Failed to serialize to JSON", e);
    }
  }

  @Override
  public String toPrettyJson() {
    try {
      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
    } catch (JsonProcessingException e) {
      throw new ConfigException("Failed to serialize to JSON", e);
    }
  }

  @Override
  public String toString() {
    return toJson();
  }
}

