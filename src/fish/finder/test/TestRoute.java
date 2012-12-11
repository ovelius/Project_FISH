package fish.finder.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.junit.After;
import org.junit.Test;

import fish.finder.Client;
import fish.finder.Connection;
import fish.finder.Route;

public class TestRoute {

  private int count = 1;
  private ArrayList<Client> createdClients = new ArrayList<Client>();
  private Client createClient() throws ClassNotFoundException, IOException {
    String file = "file-cache" + count + ".object";
    int port = Client.LISTEN_PORT + count;
    new File(file).delete();
    Client client = new Client(port, file);
    createdClients.add(client);
    count++;
    return client;
  }
  @After
  public void tearDown() {
    for(Client c : createdClients) {
      c.close();
    }
    createdClients.clear();
  }
  
  @Test
  public void testRouting() throws ClassNotFoundException, IOException, InterruptedException {
    Route.DEBUG = true;
    Client client1 = createClient();
    Client client2 = createClient();
    Client client3 = createClient();

    System.out.println("Client1: " + client1.getLocalIdentity());
    System.out.println("Client2: " + client2.getLocalIdentity());
    System.out.println("Client3: " + client3.getLocalIdentity());

    client1.connectoTo(client2);
    client2.connectoTo(client3);

    assertTrue(client1.getRoute().hasRoute(client2));
    assertTrue(client2.getRoute().hasRoute(client1));
    assertTrue(client2.getRoute().hasRoute(client3));
    assertTrue(client3.getRoute().hasRoute(client2));

    client1.broadcastPing();
    Thread.sleep(1000);

    assertTrue(client1.getRoute().hasRoute(client3));
    assertTrue(client3.getRoute().hasRoute(client1));
  }

}
