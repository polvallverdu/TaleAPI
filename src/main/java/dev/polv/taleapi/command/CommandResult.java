package dev.polv.taleapi.command;

/**
 * Represents the result of a command execution.
 * <p>
 * This enum indicates whether a command completed successfully, failed,
 * or was not handled.
 * </p>
 */
public enum CommandResult {

  /**
   * The command executed successfully.
   */
  SUCCESS,

  /**
   * The command failed to execute.
   * <p>
   * This typically indicates a logical failure (e.g., player not found,
   * invalid state) rather than a syntax error.
   * </p>
   */
  FAILURE,

  /**
   * The command was not handled.
   * <p>
   * This can be used when a command executor decides not to handle
   * the command and wants to pass it to the next handler.
   * </p>
   */
  PASS;

  /**
   * @return {@code true} if the command was handled (SUCCESS or FAILURE)
   */
  public boolean wasHandled() {
    return this != PASS;
  }

  /**
   * @return {@code true} if the command executed successfully
   */
  public boolean isSuccess() {
    return this == SUCCESS;
  }
}

