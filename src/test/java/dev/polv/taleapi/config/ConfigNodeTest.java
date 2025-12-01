package dev.polv.taleapi.config;

import dev.polv.taleapi.config.json.JsonProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ConfigNode")
class ConfigNodeTest {

  private JsonProvider provider;

  @BeforeEach
  void setUp() {
    provider = new JsonProvider();
  }

  private ConfigNode parse(String json) {
    return provider.deserializeToNode(new StringReader(json));
  }

  @Nested
  @DisplayName("Type Checking")
  class TypeChecking {

    @Test
    @DisplayName("should identify object nodes")
    void shouldIdentifyObjectNodes() {
      ConfigNode node = parse("{\"key\": \"value\"}");

      assertTrue(node.isObject());
      assertFalse(node.isArray());
      assertFalse(node.isValue());
      assertFalse(node.isNull());
    }

    @Test
    @DisplayName("should identify array nodes")
    void shouldIdentifyArrayNodes() {
      ConfigNode node = parse("[1, 2, 3]");

      assertFalse(node.isObject());
      assertTrue(node.isArray());
      assertFalse(node.isValue());
      assertFalse(node.isNull());
    }

    @Test
    @DisplayName("should identify value nodes")
    void shouldIdentifyValueNodes() {
      ConfigNode node = parse("{\"key\": \"value\"}");
      ConfigNode value = node.get("key");

      assertFalse(value.isObject());
      assertFalse(value.isArray());
      assertTrue(value.isValue());
      assertFalse(value.isNull());
    }

    @Test
    @DisplayName("should identify null nodes")
    void shouldIdentifyNullNodes() {
      ConfigNode node = parse("{\"key\": null}");
      ConfigNode value = node.get("key");

      assertTrue(value.isNull());
    }
  }

  @Nested
  @DisplayName("Key Operations")
  class KeyOperations {

    @Test
    @DisplayName("should check key existence")
    void shouldCheckKeyExistence() {
      ConfigNode node = parse("{\"name\": \"test\", \"value\": 42}");

      assertTrue(node.has("name"));
      assertTrue(node.has("value"));
      assertFalse(node.has("missing"));
    }

    @Test
    @DisplayName("should check nested key existence with dot notation")
    void shouldCheckNestedKeyExistence() {
      ConfigNode node = parse("{\"database\": {\"host\": \"localhost\"}}");

      assertTrue(node.has("database"));
      assertTrue(node.has("database.host"));
      assertFalse(node.has("database.port"));
    }

    @Test
    @DisplayName("should return all keys")
    void shouldReturnAllKeys() {
      ConfigNode node = parse("{\"a\": 1, \"b\": 2, \"c\": 3}");

      Set<String> keys = node.keys();

      assertEquals(Set.of("a", "b", "c"), keys);
    }

    @Test
    @DisplayName("should return size")
    void shouldReturnSize() {
      ConfigNode objectNode = parse("{\"a\": 1, \"b\": 2}");
      ConfigNode arrayNode = parse("[1, 2, 3, 4]");

      assertEquals(2, objectNode.size());
      assertEquals(4, arrayNode.size());
    }
  }

  @Nested
  @DisplayName("Navigation")
  class Navigation {

    @Test
    @DisplayName("should navigate by key")
    void shouldNavigateByKey() {
      ConfigNode node = parse("{\"name\": \"test\"}");

      ConfigNode child = node.get("name");

      assertTrue(child.isValue());
      assertEquals("test", child.rawValue());
    }

    @Test
    @DisplayName("should navigate with dot notation")
    void shouldNavigateWithDotNotation() {
      ConfigNode node = parse("{\"db\": {\"conn\": {\"host\": \"localhost\"}}}");

      ConfigNode host = node.get("db.conn.host");

      assertTrue(host.isValue());
      assertEquals("localhost", host.rawValue());
    }

    @Test
    @DisplayName("should navigate by array index")
    void shouldNavigateByArrayIndex() {
      ConfigNode node = parse("[\"a\", \"b\", \"c\"]");

      assertEquals("a", node.get(0).rawValue());
      assertEquals("b", node.get(1).rawValue());
      assertEquals("c", node.get(2).rawValue());
    }

    @Test
    @DisplayName("should return null node for missing keys")
    void shouldReturnNullNodeForMissingKeys() {
      ConfigNode node = parse("{\"name\": \"test\"}");

      ConfigNode missing = node.get("missing");

      assertTrue(missing.isNull());
    }

    @Test
    @DisplayName("should use getSection for nested objects")
    void shouldUseSectionForNestedObjects() {
      ConfigNode node = parse("{\"database\": {\"host\": \"localhost\"}}");

      ConfigNode db = node.getSection("database");

      assertTrue(db.isObject());
      assertEquals("localhost", db.getString("host"));
    }
  }

  @Nested
  @DisplayName("String Values")
  class StringValues {

    @Test
    @DisplayName("should get string value")
    void shouldGetStringValue() {
      ConfigNode node = parse("{\"name\": \"test\"}");

      assertEquals("test", node.getString("name"));
    }

    @Test
    @DisplayName("should get string with default")
    void shouldGetStringWithDefault() {
      ConfigNode node = parse("{\"name\": \"test\"}");

      assertEquals("test", node.getString("name", "default"));
      assertEquals("default", node.getString("missing", "default"));
    }

    @Test
    @DisplayName("should get optional string")
    void shouldGetOptionalString() {
      ConfigNode node = parse("{\"name\": \"test\"}");

      assertEquals(Optional.of("test"), node.getOptionalString("name"));
      assertEquals(Optional.empty(), node.getOptionalString("missing"));
    }

    @Test
    @DisplayName("should throw for missing required string")
    void shouldThrowForMissingRequiredString() {
      ConfigNode node = parse("{\"name\": \"test\"}");

      assertThrows(ConfigException.class, () -> node.getString("missing"));
    }
  }

  @Nested
  @DisplayName("Integer Values")
  class IntegerValues {

    @Test
    @DisplayName("should get integer value")
    void shouldGetIntegerValue() {
      ConfigNode node = parse("{\"count\": 42}");

      assertEquals(42, node.getInt("count"));
    }

    @Test
    @DisplayName("should get integer with default")
    void shouldGetIntegerWithDefault() {
      ConfigNode node = parse("{\"count\": 42}");

      assertEquals(42, node.getInt("count", 0));
      assertEquals(100, node.getInt("missing", 100));
    }

    @Test
    @DisplayName("should get optional integer")
    void shouldGetOptionalInteger() {
      ConfigNode node = parse("{\"count\": 42}");

      assertEquals(Optional.of(42), node.getOptionalInt("count"));
      assertEquals(Optional.empty(), node.getOptionalInt("missing"));
    }

    @Test
    @DisplayName("should throw for missing required integer")
    void shouldThrowForMissingRequiredInteger() {
      ConfigNode node = parse("{\"count\": 42}");

      assertThrows(ConfigException.class, () -> node.getInt("missing"));
    }
  }

  @Nested
  @DisplayName("Long Values")
  class LongValues {

    @Test
    @DisplayName("should get long value")
    void shouldGetLongValue() {
      ConfigNode node = parse("{\"bigNumber\": 9223372036854775807}");

      assertEquals(9223372036854775807L, node.getLong("bigNumber"));
    }

    @Test
    @DisplayName("should get long with default")
    void shouldGetLongWithDefault() {
      ConfigNode node = parse("{\"value\": 100}");

      assertEquals(100L, node.getLong("value", 0L));
      assertEquals(999L, node.getLong("missing", 999L));
    }
  }

  @Nested
  @DisplayName("Double Values")
  class DoubleValues {

    @Test
    @DisplayName("should get double value")
    void shouldGetDoubleValue() {
      ConfigNode node = parse("{\"ratio\": 3.14159}");

      assertEquals(3.14159, node.getDouble("ratio"), 0.00001);
    }

    @Test
    @DisplayName("should get double with default")
    void shouldGetDoubleWithDefault() {
      ConfigNode node = parse("{\"ratio\": 3.14}");

      assertEquals(3.14, node.getDouble("ratio", 0.0), 0.01);
      assertEquals(1.5, node.getDouble("missing", 1.5), 0.01);
    }
  }

  @Nested
  @DisplayName("Boolean Values")
  class BooleanValues {

    @Test
    @DisplayName("should get boolean value")
    void shouldGetBooleanValue() {
      ConfigNode node = parse("{\"enabled\": true, \"disabled\": false}");

      assertTrue(node.getBoolean("enabled"));
      assertFalse(node.getBoolean("disabled"));
    }

    @Test
    @DisplayName("should get boolean with default")
    void shouldGetBooleanWithDefault() {
      ConfigNode node = parse("{\"enabled\": true}");

      assertTrue(node.getBoolean("enabled", false));
      assertTrue(node.getBoolean("missing", true));
      assertFalse(node.getBoolean("missing", false));
    }

    @Test
    @DisplayName("should get optional boolean")
    void shouldGetOptionalBoolean() {
      ConfigNode node = parse("{\"enabled\": true}");

      assertEquals(Optional.of(true), node.getOptionalBoolean("enabled"));
      assertEquals(Optional.empty(), node.getOptionalBoolean("missing"));
    }
  }

  @Nested
  @DisplayName("List Values")
  class ListValues {

    @Test
    @DisplayName("should get list of nodes")
    void shouldGetListOfNodes() {
      ConfigNode node = parse("{\"items\": [{\"name\": \"a\"}, {\"name\": \"b\"}]}");

      List<ConfigNode> items = node.getList("items");

      assertEquals(2, items.size());
      assertEquals("a", items.get(0).getString("name"));
      assertEquals("b", items.get(1).getString("name"));
    }

    @Test
    @DisplayName("should get string list")
    void shouldGetStringList() {
      ConfigNode node = parse("{\"tags\": [\"java\", \"kotlin\", \"scala\"]}");

      List<String> tags = node.getStringList("tags");

      assertEquals(List.of("java", "kotlin", "scala"), tags);
    }

    @Test
    @DisplayName("should get integer list")
    void shouldGetIntegerList() {
      ConfigNode node = parse("{\"numbers\": [1, 2, 3, 4, 5]}");

      List<Integer> numbers = node.getIntList("numbers");

      assertEquals(List.of(1, 2, 3, 4, 5), numbers);
    }

    @Test
    @DisplayName("should return empty list for missing key")
    void shouldReturnEmptyListForMissingKey() {
      ConfigNode node = parse("{\"name\": \"test\"}");

      assertTrue(node.getStringList("missing").isEmpty());
      assertTrue(node.getIntList("missing").isEmpty());
      assertTrue(node.getList("missing").isEmpty());
    }
  }

  @Nested
  @DisplayName("Object Mapping")
  class ObjectMapping {

    public static class Person {
      public String name;
      public int age;
    }

    @Test
    @DisplayName("should convert node to typed object")
    void shouldConvertNodeToTypedObject() {
      ConfigNode node = parse("{\"name\": \"Alice\", \"age\": 30}");

      Person person = node.as(Person.class);

      assertEquals("Alice", person.name);
      assertEquals(30, person.age);
    }

    @Test
    @DisplayName("should convert child node to typed object")
    void shouldConvertChildNodeToTypedObject() {
      ConfigNode node = parse("{\"person\": {\"name\": \"Bob\", \"age\": 25}}");

      Person person = node.get("person", Person.class);

      assertEquals("Bob", person.name);
      assertEquals(25, person.age);
    }
  }

  @Nested
  @DisplayName("JSON Output")
  class JsonOutput {

    @Test
    @DisplayName("should convert to JSON string")
    void shouldConvertToJsonString() {
      ConfigNode node = parse("{\"name\": \"test\"}");

      String json = node.toJson();

      assertTrue(json.contains("\"name\""));
      assertTrue(json.contains("\"test\""));
    }

    @Test
    @DisplayName("should convert to pretty JSON string")
    void shouldConvertToPrettyJsonString() {
      ConfigNode node = parse("{\"name\": \"test\", \"value\": 42}");

      String prettyJson = node.toPrettyJson();

      assertTrue(prettyJson.contains("\n"));
    }
  }

  @Nested
  @DisplayName("Complex Scenarios")
  class ComplexScenarios {

    @Test
    @DisplayName("should handle deeply nested structures")
    void shouldHandleDeeplyNestedStructures() {
      String json = """
          {
              "level1": {
                  "level2": {
                      "level3": {
                          "value": "deep"
                      }
                  }
              }
          }
          """;
      ConfigNode node = parse(json);

      assertEquals("deep", node.getString("level1.level2.level3.value"));
    }

    @Test
    @DisplayName("should handle mixed arrays and objects")
    void shouldHandleMixedArraysAndObjects() {
      String json = """
          {
              "servers": [
                  {"host": "server1.com", "port": 8080},
                  {"host": "server2.com", "port": 9090}
              ]
          }
          """;
      ConfigNode node = parse(json);

      List<ConfigNode> servers = node.getList("servers");
      assertEquals(2, servers.size());
      assertEquals("server1.com", servers.get(0).getString("host"));
      assertEquals(9090, servers.get(1).getInt("port"));
    }

    @Test
    @DisplayName("should handle config with all value types")
    void shouldHandleAllValueTypes() {
      String json = """
          {
              "string": "hello",
              "integer": 42,
              "long": 9999999999,
              "double": 3.14,
              "boolean": true,
              "null": null,
              "array": [1, 2, 3],
              "object": {"nested": true}
          }
          """;
      ConfigNode node = parse(json);

      assertEquals("hello", node.getString("string"));
      assertEquals(42, node.getInt("integer"));
      assertEquals(9999999999L, node.getLong("long"));
      assertEquals(3.14, node.getDouble("double"), 0.01);
      assertTrue(node.getBoolean("boolean"));
      assertTrue(node.get("null").isNull());
      assertEquals(List.of(1, 2, 3), node.getIntList("array"));
      assertTrue(node.get("object").isObject());
    }
  }
}

