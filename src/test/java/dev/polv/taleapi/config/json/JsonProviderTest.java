package dev.polv.taleapi.config.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dev.polv.taleapi.config.ConfigException;
import dev.polv.taleapi.config.ConfigNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JsonProvider")
class JsonProviderTest {

  // Test configuration classes
  public static class SimpleConfig {
    public String name;
    public int value;

    public SimpleConfig() {
    }

    public SimpleConfig(String name, int value) {
      this.name = name;
      this.value = value;
    }
  }

  public static class NestedConfig {
    public String title;
    public SimpleConfig nested;
    public List<String> items;

    public NestedConfig() {
    }

    public NestedConfig(String title, SimpleConfig nested, List<String> items) {
      this.title = title;
      this.nested = nested;
      this.items = items;
    }
  }

  public static class ConfigWithDefaults {
    public String name = "default";
    public int count = 10;
    public boolean enabled = true;

    public ConfigWithDefaults() {
    }
  }

  @Nested
  @DisplayName("Object Deserialization")
  class ObjectDeserialization {

    @Test
    @DisplayName("should deserialize simple JSON object")
    void shouldDeserializeSimpleObject() {
      JsonProvider provider = new JsonProvider();
      String json = """
          {
              "name": "test",
              "value": 42
          }
          """;

      SimpleConfig config = provider.deserialize(new StringReader(json), SimpleConfig.class);

      assertEquals("test", config.name);
      assertEquals(42, config.value);
    }

    @Test
    @DisplayName("should deserialize nested objects")
    void shouldDeserializeNestedObjects() {
      JsonProvider provider = new JsonProvider();
      String json = """
          {
              "title": "My Config",
              "nested": {
                  "name": "inner",
                  "value": 100
              },
              "items": ["a", "b", "c"]
          }
          """;

      NestedConfig config = provider.deserialize(new StringReader(json), NestedConfig.class);

      assertEquals("My Config", config.title);
      assertNotNull(config.nested);
      assertEquals("inner", config.nested.name);
      assertEquals(100, config.nested.value);
      assertEquals(List.of("a", "b", "c"), config.items);
    }

    @Test
    @DisplayName("should preserve default values for missing fields")
    void shouldPreserveDefaultsForMissingFields() {
      JsonProvider provider = new JsonProvider();
      String json = """
          {
              "name": "custom"
          }
          """;

      ConfigWithDefaults config = provider.deserialize(new StringReader(json), ConfigWithDefaults.class);

      assertEquals("custom", config.name);
      assertEquals(10, config.count); // default value
      assertTrue(config.enabled); // default value
    }

    @Test
    @DisplayName("should throw ConfigException for invalid JSON")
    void shouldThrowForInvalidJson() {
      JsonProvider provider = new JsonProvider();
      String invalidJson = "{ invalid json }";

      assertThrows(ConfigException.class,
          () -> provider.deserialize(new StringReader(invalidJson), SimpleConfig.class));
    }

    @Test
    @DisplayName("should throw NullPointerException for null reader")
    void shouldThrowForNullReader() {
      JsonProvider provider = new JsonProvider();

      assertThrows(NullPointerException.class, () -> provider.deserialize(null, SimpleConfig.class));
    }

    @Test
    @DisplayName("should throw NullPointerException for null type")
    void shouldThrowForNullType() {
      JsonProvider provider = new JsonProvider();

      assertThrows(NullPointerException.class, () -> provider.deserialize(new StringReader("{}"), null));
    }
  }

  @Nested
  @DisplayName("Node Deserialization")
  class NodeDeserialization {

    @Test
    @DisplayName("should deserialize to ConfigNode")
    void shouldDeserializeToNode() {
      JsonProvider provider = new JsonProvider();
      String json = """
          {
              "name": "test",
              "value": 42,
              "enabled": true
          }
          """;

      ConfigNode node = provider.deserializeToNode(new StringReader(json));

      assertTrue(node.isObject());
      assertEquals("test", node.getString("name"));
      assertEquals(42, node.getInt("value"));
      assertTrue(node.getBoolean("enabled"));
    }

    @Test
    @DisplayName("should support nested paths with dot notation")
    void shouldSupportDotNotation() {
      JsonProvider provider = new JsonProvider();
      String json = """
          {
              "database": {
                  "host": "localhost",
                  "port": 5432
              }
          }
          """;

      ConfigNode node = provider.deserializeToNode(new StringReader(json));

      assertEquals("localhost", node.getString("database.host"));
      assertEquals(5432, node.getInt("database.port"));
    }

    @Test
    @DisplayName("should handle arrays")
    void shouldHandleArrays() {
      JsonProvider provider = new JsonProvider();
      String json = """
          {
              "items": ["a", "b", "c"],
              "numbers": [1, 2, 3]
          }
          """;

      ConfigNode node = provider.deserializeToNode(new StringReader(json));

      assertEquals(List.of("a", "b", "c"), node.getStringList("items"));
      assertEquals(List.of(1, 2, 3), node.getIntList("numbers"));
    }

    @Test
    @DisplayName("should return default for missing keys")
    void shouldReturnDefaultForMissingKeys() {
      JsonProvider provider = new JsonProvider();
      String json = """
          {
              "name": "test"
          }
          """;

      ConfigNode node = provider.deserializeToNode(new StringReader(json));

      assertEquals("default", node.getString("missing", "default"));
      assertEquals(42, node.getInt("missing", 42));
      assertTrue(node.getBoolean("missing", true));
    }

    @Test
    @DisplayName("should convert node to typed object")
    void shouldConvertNodeToObject() {
      JsonProvider provider = new JsonProvider();
      String json = """
          {
              "name": "test",
              "value": 42
          }
          """;

      ConfigNode node = provider.deserializeToNode(new StringReader(json));
      SimpleConfig config = node.as(SimpleConfig.class);

      assertEquals("test", config.name);
      assertEquals(42, config.value);
    }
  }

  @Nested
  @DisplayName("Serialization")
  class Serialization {

    @Test
    @DisplayName("should serialize simple object")
    void shouldSerializeSimpleObject() {
      JsonProvider provider = new JsonProvider();
      SimpleConfig config = new SimpleConfig("test", 42);
      StringWriter writer = new StringWriter();

      provider.serialize(writer, config);
      String json = writer.toString();

      assertTrue(json.contains("\"name\""));
      assertTrue(json.contains("\"test\""));
      assertTrue(json.contains("\"value\""));
      assertTrue(json.contains("42"));
    }

    @Test
    @DisplayName("should serialize nested objects")
    void shouldSerializeNestedObjects() {
      JsonProvider provider = new JsonProvider();
      NestedConfig config = new NestedConfig(
          "My Config",
          new SimpleConfig("inner", 100),
          List.of("a", "b", "c"));
      StringWriter writer = new StringWriter();

      provider.serialize(writer, config);
      String json = writer.toString();

      assertTrue(json.contains("\"title\""));
      assertTrue(json.contains("\"My Config\""));
      assertTrue(json.contains("\"nested\""));
      assertTrue(json.contains("\"items\""));
    }

    @Test
    @DisplayName("should produce pretty-printed JSON by default")
    void shouldProducePrettyPrintedJson() {
      JsonProvider provider = new JsonProvider();
      SimpleConfig config = new SimpleConfig("test", 42);
      StringWriter writer = new StringWriter();

      provider.serialize(writer, config);
      String json = writer.toString();

      // Pretty printed JSON should have newlines
      assertTrue(json.contains("\n"));
    }

    @Test
    @DisplayName("should throw NullPointerException for null writer")
    void shouldThrowForNullWriter() {
      JsonProvider provider = new JsonProvider();

      assertThrows(NullPointerException.class, () -> provider.serialize(null, new SimpleConfig()));
    }

    @Test
    @DisplayName("should throw NullPointerException for null value")
    void shouldThrowForNullValue() {
      JsonProvider provider = new JsonProvider();

      assertThrows(NullPointerException.class, () -> provider.serialize(new StringWriter(), null));
    }
  }

  @Nested
  @DisplayName("Roundtrip")
  class Roundtrip {

    @Test
    @DisplayName("should roundtrip simple config")
    void shouldRoundtripSimpleConfig() {
      JsonProvider provider = new JsonProvider();
      SimpleConfig original = new SimpleConfig("test", 42);

      StringWriter writer = new StringWriter();
      provider.serialize(writer, original);

      SimpleConfig loaded = provider.deserialize(new StringReader(writer.toString()), SimpleConfig.class);

      assertEquals(original.name, loaded.name);
      assertEquals(original.value, loaded.value);
    }

    @Test
    @DisplayName("should roundtrip nested config")
    void shouldRoundtripNestedConfig() {
      JsonProvider provider = new JsonProvider();
      NestedConfig original = new NestedConfig(
          "My Config",
          new SimpleConfig("inner", 100),
          List.of("a", "b", "c"));

      StringWriter writer = new StringWriter();
      provider.serialize(writer, original);

      NestedConfig loaded = provider.deserialize(new StringReader(writer.toString()), NestedConfig.class);

      assertEquals(original.title, loaded.title);
      assertEquals(original.nested.name, loaded.nested.name);
      assertEquals(original.nested.value, loaded.nested.value);
      assertEquals(original.items, loaded.items);
    }
  }

  @Nested
  @DisplayName("Provider Properties")
  class ProviderProperties {

    @Test
    @DisplayName("should return json file extension")
    void shouldReturnJsonExtension() {
      JsonProvider provider = new JsonProvider();
      assertEquals("json", provider.fileExtension());
    }

    @Test
    @DisplayName("should return JSON format name")
    void shouldReturnJsonFormatName() {
      JsonProvider provider = new JsonProvider();
      assertEquals("JSON", provider.formatName());
    }

    @Test
    @DisplayName("should expose ObjectMapper instance")
    void shouldExposeMapperInstance() {
      JsonProvider provider = new JsonProvider();
      assertNotNull(provider.getMapper());
    }
  }

  @Nested
  @DisplayName("Builder")
  class BuilderTests {

    @Test
    @DisplayName("should create provider with builder")
    void shouldCreateWithBuilder() {
      JsonProvider provider = JsonProvider.builder()
          .prettyPrinting()
          .ignoreUnknownProperties()
          .build();

      assertNotNull(provider);
      assertNotNull(provider.getMapper());
    }

    @Test
    @DisplayName("should create compact output when configured")
    void shouldCreateCompactOutput() {
      JsonProvider provider = JsonProvider.builder()
          .compact()
          .build();

      SimpleConfig config = new SimpleConfig("test", 42);
      StringWriter writer = new StringWriter();
      provider.serialize(writer, config);

      // Compact JSON should not have newlines between fields
      assertFalse(writer.toString().contains("\n  "));
    }
  }

  @Nested
  @DisplayName("Custom ObjectMapper")
  class CustomMapper {

    @Test
    @DisplayName("should accept custom ObjectMapper instance")
    void shouldAcceptCustomMapper() {
      var customMapper = new ObjectMapper()
          .enable(SerializationFeature.INDENT_OUTPUT);

      JsonProvider provider = new JsonProvider(customMapper);

      assertSame(customMapper, provider.getMapper());
    }

    @Test
    @DisplayName("should throw NullPointerException for null mapper")
    void shouldThrowForNullMapper() {
      assertThrows(NullPointerException.class, () -> new JsonProvider(null));
    }
  }
}
