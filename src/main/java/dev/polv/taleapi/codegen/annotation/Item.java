package dev.polv.taleapi.codegen.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as an item definition for JSON generation.
 * <p>
 * When annotated, the annotation processor will generate a corresponding
 * JSON file in the resources directory.
 * </p>
 *
 * <pre>{@code
 * @Item(id = "mymod:ruby_sword")
 * public class RubySword implements TaleItem {
 *   // ...
 * }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Item {

  /**
   * The unique identifier for this item.
   * <p>
   * Format: {@code namespace:item_name}
   * </p>
   *
   * @return the item identifier
   */
  String id();

}
