package dev.polv.taleapi.config;

/**
 * Exception thrown when configuration operations fail.
 * <p>
 * This exception wraps underlying I/O and parsing errors that may occur
 * during configuration loading, saving, or parsing operations.
 * </p>
 */
public class ConfigException extends RuntimeException {

  /**
   * Constructs a new ConfigException with the specified message.
   *
   * @param message the detail message
   */
  public ConfigException(String message) {
    super(message);
  }

  /**
   * Constructs a new ConfigException with the specified message and cause.
   *
   * @param message the detail message
   * @param cause   the underlying cause
   */
  public ConfigException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new ConfigException with the specified cause.
   *
   * @param cause the underlying cause
   */
  public ConfigException(Throwable cause) {
    super(cause);
  }
}
