package dev.polv.taleapi.config.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dev.polv.taleapi.config.ConfigException;
import dev.polv.taleapi.config.ConfigNode;
import dev.polv.taleapi.config.ConfigProvider;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Objects;

/**
 * A {@link ConfigProvider} implementation for JSON format using Jackson.
 * <p>
 * This provider supports both object mapping and dynamic node traversal.
 * It uses Jackson with sensible defaults including pretty printing.
 * </p>
 *
 * <h2>Example Usage</h2>
 * 
 * <pre>{@code
 * // Create with default settings (pretty printing enabled)
 * JsonProvider provider = new JsonProvider();
 *
 * // Create with custom ObjectMapper
 * ObjectMapper customMapper = new ObjectMapper()
 *     .enable(SerializationFeature.INDENT_OUTPUT)
 *     .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
 * JsonProvider customProvider = new JsonProvider(customMapper);
 *
 * // Use with ConfigLoader
 * ConfigLoader loader = new ConfigLoader(provider);
 * }</pre>
 *
 * <h2>Supported Types</h2>
 * <p>
 * JsonProvider supports all types that Jackson can serialize:
 * </p>
 * <ul>
 * <li>Primitives and their wrappers</li>
 * <li>Strings</li>
 * <li>Arrays and Collections</li>
 * <li>Maps (with String keys)</li>
 * <li>POJOs with public fields or getters/setters</li>
 * <li>Records (Java 14+)</li>
 * </ul>
 *
 * @see ConfigProvider
 * @see dev.polv.taleapi.config.ConfigLoader
 */
public class JsonProvider implements ConfigProvider {

  private final ObjectMapper mapper;

  /**
   * Creates a new JsonProvider with default Jackson settings.
   * <p>
   * Default settings include:
   * </p>
   * <ul>
   * <li>Pretty printing enabled</li>
   * <li>Unknown properties ignored during deserialization</li>
   * </ul>
   */
  public JsonProvider() {
    this(createDefaultMapper());
  }

  /**
   * Creates a new JsonProvider with a custom ObjectMapper.
   *
   * @param mapper the ObjectMapper to use for serialization
   * @throws NullPointerException if mapper is null
   */
  public JsonProvider(ObjectMapper mapper) {
    this.mapper = Objects.requireNonNull(mapper, "mapper");
  }

  /**
   * Creates the default ObjectMapper with sensible settings.
   */
  private static ObjectMapper createDefaultMapper() {
    return new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  }

  /**
   * Returns the ObjectMapper used by this provider.
   *
   * @return the ObjectMapper
   */
  public ObjectMapper getMapper() {
    return mapper;
  }

  @Override
  public <T> T deserialize(Reader reader, Class<T> type) throws ConfigException {
    Objects.requireNonNull(reader, "reader");
    Objects.requireNonNull(type, "type");

    try {
      T result = mapper.readValue(reader, type);
      if (result == null) {
        throw new ConfigException("Deserialization returned null for type: " + type.getName());
      }
      return result;
    } catch (IOException e) {
      throw new ConfigException("Failed to deserialize JSON", e);
    }
  }

  @Override
  public ConfigNode deserializeToNode(Reader reader) throws ConfigException {
    Objects.requireNonNull(reader, "reader");

    try {
      JsonNode jsonNode = mapper.readTree(reader);
      if (jsonNode == null) {
        throw new ConfigException("Deserialization returned null");
      }
      return new JacksonConfigNode(jsonNode, mapper);
    } catch (IOException e) {
      throw new ConfigException("Failed to deserialize JSON", e);
    }
  }

  @Override
  public <T> void serialize(Writer writer, T value) throws ConfigException {
    Objects.requireNonNull(writer, "writer");
    Objects.requireNonNull(value, "value");

    try {
      mapper.writeValue(writer, value);
    } catch (IOException e) {
      throw new ConfigException("Failed to serialize to JSON", e);
    }
  }

  @Override
  public void serialize(Writer writer, ConfigNode node) throws ConfigException {
    Objects.requireNonNull(writer, "writer");
    Objects.requireNonNull(node, "node");

    if (node instanceof JacksonConfigNode jacksonNode) {
      try {
        mapper.writeValue(writer, jacksonNode.getJsonNode());
      } catch (IOException e) {
        throw new ConfigException("Failed to serialize to JSON", e);
      }
    } else {
      // Fallback: serialize the raw value
      serialize(writer, node.rawValue());
    }
  }

  @Override
  public String fileExtension() {
    return "json";
  }

  @Override
  public String formatName() {
    return "JSON";
  }

  /**
   * Creates a builder for configuring a JsonProvider.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for creating customized JsonProvider instances.
   */
  public static class Builder {
    private final ObjectMapper mapper;

    private Builder() {
      this.mapper = new ObjectMapper();
    }

    /**
     * Enables pretty printing for output.
     *
     * @return this builder
     */
    public Builder prettyPrinting() {
      mapper.enable(SerializationFeature.INDENT_OUTPUT);
      return this;
    }

    /**
     * Disables pretty printing for compact output.
     *
     * @return this builder
     */
    public Builder compact() {
      mapper.disable(SerializationFeature.INDENT_OUTPUT);
      return this;
    }

    /**
     * Ignores unknown properties during deserialization.
     *
     * @return this builder
     */
    public Builder ignoreUnknownProperties() {
      mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
      return this;
    }

    /**
     * Fails on unknown properties during deserialization.
     *
     * @return this builder
     */
    public Builder failOnUnknownProperties() {
      mapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
      return this;
    }

    /**
     * Enables serialization of null values.
     *
     * @return this builder
     */
    public Builder serializeNulls() {
      mapper.disable(SerializationFeature.WRITE_NULL_MAP_VALUES);
      return this;
    }

    /**
     * Sets a custom date format for serialization.
     *
     * @param pattern the date format pattern
     * @return this builder
     */
    public Builder dateFormat(String pattern) {
      mapper.setDateFormat(new java.text.SimpleDateFormat(pattern));
      return this;
    }

    /**
     * Builds the JsonProvider with the configured settings.
     *
     * @return a new JsonProvider instance
     */
    public JsonProvider build() {
      return new JsonProvider(mapper);
    }
  }
}
