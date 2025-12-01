package dev.polv.taleapi.testutil;

import dev.polv.taleapi.server.TaleServer;

/**
 * Test implementation of TaleServer for testing purposes.
 * <p>
 * This class provides a simple in-memory implementation of the TaleServer
 * interface that can be used for testing server-related functionality.
 * </p>
 */
public class TestServer implements TaleServer {
  private final String name;

  /**
   * Creates a new test server with a default name.
   */
  public TestServer() {
    this.name = "TestServer";
  }

  /**
   * Creates a new test server with the specified name.
   *
   * @param name the server name
   */
  public TestServer(String name) {
    this.name = name;
  }

  /**
   * @return the server name
   */
  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return "TestServer{" +
        "name='" + name + '\'' +
        '}';
  }
}

