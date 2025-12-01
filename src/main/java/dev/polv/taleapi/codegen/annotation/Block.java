package dev.polv.taleapi.codegen.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a block definition for JSON generation.
 * <p>
 * When annotated, the annotation processor will generate a corresponding
 * JSON file in the resources directory.
 * </p>
 *
 * <pre>{@code
 * @Block(id = "mymod:magic_stone")
 * public class MagicStone implements TaleBlock {
 *   // ...
 * }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Block {

  /**
   * The unique identifier for this block.
   * <p>
   * Format: {@code namespace:block_name}
   * </p>
   *
   * @return the block identifier
   */
  String id();

}
