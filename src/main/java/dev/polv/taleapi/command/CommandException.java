package dev.polv.taleapi.command;

/**
 * Exception thrown when a command fails to execute or parse.
 * <p>
 * This exception can be thrown by {@link CommandExecutor} implementations
 * or during command parsing to indicate various failure conditions.
 * </p>
 */
public class CommandException extends RuntimeException {

  /** The type of this command exception. */
  private final Type type;

  /**
   * Creates a new CommandException with a message.
   *
   * @param message the error message
   */
  public CommandException(String message) {
    super(message);
    this.type = Type.EXECUTION;
  }

  /**
   * Creates a new CommandException with a message and cause.
   *
   * @param message the error message
   * @param cause   the underlying cause
   */
  public CommandException(String message, Throwable cause) {
    super(message, cause);
    this.type = Type.EXECUTION;
  }

  /**
   * Creates a new CommandException with a message and type.
   *
   * @param message the error message
   * @param type    the exception type
   */
  public CommandException(String message, Type type) {
    super(message);
    this.type = type;
  }

  /**
   * Creates a new CommandException with a message, cause, and type.
   *
   * @param message the error message
   * @param cause   the underlying cause
   * @param type    the exception type
   */
  public CommandException(String message, Throwable cause, Type type) {
    super(message, cause);
    this.type = type;
  }

  /**
   * Returns the type of this exception.
   *
   * @return the exception type
   */
  public Type getType() {
    return type;
  }

  /**
   * Creates a syntax exception indicating invalid command syntax.
   *
   * @param message the error message
   * @return a new CommandException of type SYNTAX
   */
  public static CommandException syntax(String message) {
    return new CommandException(message, Type.SYNTAX);
  }

  /**
   * Creates a permission exception indicating missing permissions.
   *
   * @param permission the missing permission
   * @return a new CommandException of type PERMISSION
   */
  public static CommandException permission(String permission) {
    return new CommandException("Missing permission: " + permission, Type.PERMISSION);
  }

  /**
   * Creates an argument exception indicating an invalid argument.
   *
   * @param argument the argument name
   * @param message  the error message
   * @return a new CommandException of type ARGUMENT
   */
  public static CommandException argument(String argument, String message) {
    return new CommandException("Invalid argument '" + argument + "': " + message, Type.ARGUMENT);
  }

  /**
   * The type of command exception.
   */
  public enum Type {
    /**
     * Invalid command syntax.
     */
    SYNTAX,

    /**
     * Missing required permission.
     */
    PERMISSION,

    /**
     * Invalid argument value.
     */
    ARGUMENT,

    /**
     * General execution failure.
     */
    EXECUTION
  }
}

