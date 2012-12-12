package fish.finder.test;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

import fish.finder.Client;
import fish.finder.Connection;

public class TestConnection {

  @Test
  public void testClientConnect() throws ClassNotFoundException, IOException {
    Connection.DEBUG = true;
    Client.DEBUG = true;
    Client c = new Client(Client.LISTEN_PORT, "null-file.object");
    Connection connection = new Connection(c, "localhost", Client.LISTEN_PORT);
    assertTrue(connection.isOpen());
    assertTrue(c.getConnectionCount() > 0);
    assertEquals(c.getLocalIdentity(), connection.getRemoteIdentity());

    connection.close();
    c.close();
  }

}
