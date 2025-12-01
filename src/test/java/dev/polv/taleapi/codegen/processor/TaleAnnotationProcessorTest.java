package dev.polv.taleapi.codegen.processor;

import dev.polv.taleapi.codegen.annotation.Block;
import dev.polv.taleapi.codegen.annotation.Item;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TaleAnnotationProcessor")
class TaleAnnotationProcessorTest {

  @TempDir
  Path tempDir;

  private CompilationResult compile(String className, String sourceCode) throws IOException {
    Path sourceDir = tempDir.resolve("src");
    Path outputDir = tempDir.resolve("out");
    Files.createDirectories(sourceDir);
    Files.createDirectories(outputDir);

    // Write the source file
    Path sourceFile = sourceDir.resolve(className + ".java");
    Files.writeString(sourceFile, sourceCode);

    // Get the Java compiler
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

    try (
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, Locale.getDefault(), null)) {
      fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(outputDir.toFile()));

      Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjects(sourceFile.toFile());

      JavaCompiler.CompilationTask task = compiler.getTask(
          null,
          fileManager,
          diagnostics,
          List.of("-classpath", System.getProperty("java.class.path")),
          null,
          compilationUnits);

      task.setProcessors(List.of(new TaleAnnotationProcessor()));

      boolean success = task.call();
      return new CompilationResult(success, diagnostics.getDiagnostics(), outputDir);
    }
  }

  record CompilationResult(
      boolean success,
      List<Diagnostic<? extends JavaFileObject>> diagnostics,
      Path outputDir) {
    String getGeneratedJson(String path) throws IOException {
      Path jsonFile = outputDir.resolve(path);
      if (Files.exists(jsonFile)) {
        return Files.readString(jsonFile);
      }
      return null;
    }

    boolean hasError(String messageFragment) {
      return diagnostics.stream()
          .filter(d -> d.getKind() == Diagnostic.Kind.ERROR)
          .anyMatch(d -> d.getMessage(Locale.getDefault()).contains(messageFragment));
    }
  }

  @Nested
  @DisplayName("Block Processing")
  class BlockProcessing {

    @Test
    @DisplayName("should generate JSON for @Block annotation")
    void shouldGenerateBlockJson() throws IOException {
      String source = """
          import dev.polv.taleapi.codegen.annotation.Block;

          @Block(id = "mymod:magic_stone")
          public class MagicStone {
          }
          """;

      CompilationResult result = compile("MagicStone", source);

      assertTrue(result.success(), "Compilation should succeed");
      String json = result.getGeneratedJson("blocks/magic_stone.json");
      assertNotNull(json, "JSON file should be generated");
      assertTrue(json.contains("\"id\": \"mymod:magic_stone\""), "JSON should contain the block id");
    }

    @Test
    @DisplayName("should fail if block id is missing namespace")
    void shouldFailWithoutNamespace() throws IOException {
      String source = """
          import dev.polv.taleapi.codegen.annotation.Block;

          @Block(id = "invalid_id")
          public class InvalidBlock {
          }
          """;

      CompilationResult result = compile("InvalidBlock", source);

      assertFalse(result.success(), "Compilation should fail");
      assertTrue(result.hasError("namespace:name"), "Should report format error");
    }

    @Test
    @DisplayName("should fail if block id is empty")
    void shouldFailWithEmptyId() throws IOException {
      String source = """
          import dev.polv.taleapi.codegen.annotation.Block;

          @Block(id = "")
          public class EmptyBlock {
          }
          """;

      CompilationResult result = compile("EmptyBlock", source);

      assertFalse(result.success(), "Compilation should fail");
      assertTrue(result.hasError("cannot be empty"), "Should report empty id error");
    }
  }

  @Nested
  @DisplayName("Item Processing")
  class ItemProcessing {

    @Test
    @DisplayName("should generate JSON for @Item annotation")
    void shouldGenerateItemJson() throws IOException {
      String source = """
          import dev.polv.taleapi.codegen.annotation.Item;

          @Item(id = "mymod:ruby_sword")
          public class RubySword {
          }
          """;

      CompilationResult result = compile("RubySword", source);

      assertTrue(result.success(), "Compilation should succeed");
      String json = result.getGeneratedJson("items/ruby_sword.json");
      assertNotNull(json, "JSON file should be generated");
      assertTrue(json.contains("\"id\": \"mymod:ruby_sword\""), "JSON should contain the item id");
    }

    @Test
    @DisplayName("should fail if item id is missing namespace")
    void shouldFailWithoutNamespace() throws IOException {
      String source = """
          import dev.polv.taleapi.codegen.annotation.Item;

          @Item(id = "no_namespace")
          public class InvalidItem {
          }
          """;

      CompilationResult result = compile("InvalidItem", source);

      assertFalse(result.success(), "Compilation should fail");
      assertTrue(result.hasError("namespace:name"), "Should report format error");
    }
  }

  @Nested
  @DisplayName("File Naming")
  class FileNaming {

    @Test
    @DisplayName("should use name after colon for filename")
    void shouldExtractNameForFilename() throws IOException {
      String source = """
          import dev.polv.taleapi.codegen.annotation.Block;

          @Block(id = "somemod:super_special_block")
          public class SuperBlock {
          }
          """;

      CompilationResult result = compile("SuperBlock", source);

      assertTrue(result.success(), "Compilation should succeed");
      String json = result.getGeneratedJson("blocks/super_special_block.json");
      assertNotNull(json, "JSON file should use name after colon");
    }
  }
}
