package fish.finder.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import fish.finder.Client;
import fish.finder.Connection;

public class TestConnection {

  @Test
  public void testClientConnect() throws ClassNotFoundException, IOException {
    Connection.DEBUG = true;
    Client.DEBUG = true;
    Client c = new Client(Client.LISTEN_PORT, null);
    Connection connection = new Connection(c, "localhost", Client.LISTEN_PORT);
    assertTrue(connection.isOpen());
    assertTrue(c.getRoute().directConnections() > 0);
    assertEquals(c.getLocalIdentity(), connection.getRemoteIdentity());

    connection.close();
    c.close();
  }

  @Test
  public void testDisconnect() throws ClassNotFoundException, IOException, InterruptedException {
    Connection.DEBUG = true;
    Client c = new Client(Client.LISTEN_PORT, null);
    Connection connection = new Connection(c, "localhost", Client.LISTEN_PORT);

    assertTrue(connection.isOpen());
    assertTrue(c.getRoute().directConnections() > 0);
    assertEquals(c.getLocalIdentity(), connection.getRemoteIdentity());

    Thread.sleep(500);
    connection.close();
    Thread.sleep(500);

    assertFalse(connection.isOpen());
    assertTrue(c.getRoute().directConnections()  == 0);
    c.close();
  }
  

}
