package fish.finder.test;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.junit.After;

import fish.finder.Client;
import fish.finder.gui.TopologyMap;

public class testTopologyGUI {

  private TopologyMap map;
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
  public void tearDown() {
    for(Client c : createdClients) {
      c.close();
    }
    createdClients.clear();
  }
  
  public testTopologyGUI() throws ClassNotFoundException, IOException, InterruptedException {
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
    map = new TopologyMap(client1);
    map.setVisible(true);
  }
  /**
   * @param args
   * @throws InterruptedException 
   * @throws IOException 
   * @throws ClassNotFoundException 
   */
  public static void main(String[] args) throws ClassNotFoundException, IOException, InterruptedException {
    testTopologyGUI g = new testTopologyGUI();
    while(g.map.isVisible()) {
      Thread.sleep(1000);
    }
    g.tearDown();
  }

}
