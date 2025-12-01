package dev.polv.taleapi.codegen.processor;

import dev.polv.taleapi.codegen.annotation.Block;
import dev.polv.taleapi.codegen.annotation.Item;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;

/**
 * Annotation processor that generates JSON files for blocks and items.
 * <p>
 * Processes {@link Block} and {@link Item} annotations and generates
 * corresponding JSON files in the output resources directory.
 * </p>
 */
@SupportedAnnotationTypes({
    "dev.polv.taleapi.codegen.annotation.Block",
    "dev.polv.taleapi.codegen.annotation.Item"
})
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class TaleAnnotationProcessor extends AbstractProcessor {

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    for (Element element : roundEnv.getElementsAnnotatedWith(Block.class)) {
      processBlock(element);
    }

    for (Element element : roundEnv.getElementsAnnotatedWith(Item.class)) {
      processItem(element);
    }

    return true;
  }

  private void processBlock(Element element) {
    Block annotation = element.getAnnotation(Block.class);
    String id = annotation.id();

    if (!validateId(id, element)) {
      return;
    }

    String fileName = getFileName(id);
    String json = generateBlockJson(id);

    writeResourceFile("blocks/" + fileName + ".json", json, element);
  }

  private void processItem(Element element) {
    Item annotation = element.getAnnotation(Item.class);
    String id = annotation.id();

    if (!validateId(id, element)) {
      return;
    }

    String fileName = getFileName(id);
    String json = generateItemJson(id);

    writeResourceFile("items/" + fileName + ".json", json, element);
  }

  private boolean validateId(String id, Element element) {
    if (id == null || id.isBlank()) {
      processingEnv.getMessager().printMessage(
          Diagnostic.Kind.ERROR,
          "id cannot be empty",
          element);
      return false;
    }

    if (!id.contains(":")) {
      processingEnv.getMessager().printMessage(
          Diagnostic.Kind.ERROR,
          "id must be in format 'namespace:name' (e.g., 'mymod:ruby')",
          element);
      return false;
    }

    return true;
  }

  private String getFileName(String id) {
    // Extract name after the colon: "mymod:magic_stone" -> "magic_stone"
    int colonIndex = id.indexOf(':');
    return id.substring(colonIndex + 1);
  }

  private String generateBlockJson(String id) {
    return """
        {
          "id": "%s"
        }
        """.formatted(id);
  }

  private String generateItemJson(String id) {
    return """
        {
          "id": "%s"
        }
        """.formatted(id);
  }

  private void writeResourceFile(String path, String content, Element element) {
    try {
      FileObject file = processingEnv.getFiler().createResource(
          StandardLocation.CLASS_OUTPUT,
          "",
          path,
          element);

      try (Writer writer = file.openWriter()) {
        writer.write(content);
      }

      processingEnv.getMessager().printMessage(
          Diagnostic.Kind.NOTE,
          "Generated: " + path,
          element);
    } catch (IOException e) {
      processingEnv.getMessager().printMessage(
          Diagnostic.Kind.ERROR,
          "Failed to generate " + path + ": " + e.getMessage(),
          element);
    }
  }

}
