package fish.finder.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import fish.finder.Client;
import fish.finder.Connection;

public class TestClient {


  @Test
  public void testClientReconnect() throws ClassNotFoundException, IOException, InterruptedException {
    new File("file-cache.object").delete();
    new File("file-cache2.object").delete();

    Client client1 = new Client(9219, "file-cache.object");
    Client client2 = new Client(9129, "file-cache2.object");

    Long indentity = client1.addConnection("localhost", 9129);
    System.out.println("Remote identity:" + indentity);
    System.out.println("Client2 local identitiy:" + client2.getLocalIdentity());
    assertTrue(indentity == client2.getLocalIdentity());

    client1.close();
    client2.close();

    client2 = new Client(9129, "file-cache2.object");
    client1 = new Client(9219, "file-cache.object");
    Thread.sleep(1000);
    assertTrue(client2.getConnectionCount() > 0);
    client1.close();
    client2.close();

    new File("file-cache.object").delete();
    new File("file-cache2.object").delete();
  }

  @Test
  public void testClientSearchRequest() throws ClassNotFoundException, IOException, InterruptedException {
    Client.DEBUG = true;
    Connection.DEBUG = true;
    final String searchFor = "Client";
    new File("file-cache.object").delete();
    new File("file-cache2.object").delete();

    Client client1 = new Client(9219, "file-cache.object");
    Client client2 = new Client(9129, "file-cache2.object");

    Long indentity = client1.connectoTo(client2);
    assertTrue(indentity == client2.getLocalIdentity());
    client2.shareDirectory(".");

    assertTrue(client1.search(searchFor).size() == 0);
    assertTrue(client2.search(searchFor).size() > 0);

    client1.remoteSearch(searchFor);
    Thread.sleep(2000);

    assertTrue(client1.getSearchResults().size() > 0);

    client1.close();
    client2.close();
  }

}
