package fish.finder.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.junit.After;
import org.junit.Test;

import fish.finder.Client;
import fish.finder.Connection;
import fish.finder.FileMessageChannel;
import fish.finder.Route;
import fish.finder.gui.SearchResultModel;
import fish.finder.proto.Message.FileEntry;
import fish.finder.proto.Message.SearchResults;

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

  private void closeAll() {
    for(Client c : createdClients) {
      c.close();
    }
    createdClients.clear();
  }


  @Test 
  public void testRouting() throws ClassNotFoundException, IOException, InterruptedException {
    Thread.sleep(1000);
    Route.DEBUG = false;
    Client client1 = createClient();
    Client client2 = createClient();
    Client client3 = createClient();

    System.out.println("Client1: " + client1.getLocalIdentity());
    System.out.println("Client2: " + client2.getLocalIdentity());
    System.out.println("Client3: " + client3.getLocalIdentity());

    client1.connectoTo(client2);
    client2.connectoTo(client3);
    Thread.sleep(1500);


    assertTrue(client1.getRoute().hasRoute(client2));
    assertTrue(client2.getRoute().hasRoute(client1));
    assertTrue(client2.getRoute().hasRoute(client3));
    assertTrue(client3.getRoute().hasRoute(client2));

    client1.broadcastPing();
    Thread.sleep(1000);
    System.out.println("Routes from " + client2.getLocalIdentity());
    client2.getRoute().printRoutes();

    assertTrue(client1.getRoute().hasRoute(client3));
    assertTrue(client3.getRoute().hasRoute(client1));
    
    assertEquals(client1.getRoute().
                 getViaPeers(client2.getLocalIdentity()).size(), 1);

    assertEquals(client2.getRoute().directConnections(), 2);
    assertEquals(client2.getRoute().reachableNodes(), 2);

    assertEquals(client1.getRoute().directConnections(), 1);
    assertEquals(client1.getRoute().reachableNodes(), 2);

    assertEquals(client3.getRoute().directConnections(), 1);
    assertEquals(client3.getRoute().reachableNodes(), 2);

    assertEquals(client1.getRoute().getRoutedMessage(), 0);
    assertEquals(client2.getRoute().getRoutedMessage(), 1);
    assertEquals(client3.getRoute().getRoutedMessage(), 0);
    closeAll();
    Thread.sleep(1000);
  }
 
  @Test
  public void testRoutedFileTransfer() throws ClassNotFoundException, IOException, InterruptedException {
    Route.DEBUG = false;
    Client client1 = createClient();
    Client client2 = createClient();
    Client client3 = createClient();

    System.out.println("Client1: " + client1.getLocalIdentity());
    System.out.println("Client2: " + client2.getLocalIdentity());
    System.out.println("Client3: " + client3.getLocalIdentity());

    client1.shareDirectory(".");

    client1.connectoTo(client2);
    client2.connectoTo(client3);

    client1.broadcastPing();
    Thread.sleep(1000);
    client3.remoteSearch("client");
    Thread.sleep(1000);

    SearchResultModel s = client3.getSearchResults();
    assertTrue(s.getSize() > 0);
 
    FileEntry d = s.getFileEntry(0);
    System.out.println("Downloading file "+ d.getName() + 
                       " from " + d.getHost() + " " + d.getSize() + " Bytes");
    FileMessageChannel.DEBUG = true;
    Connection.DEBUG = true;
    String to = client3.downloadFile(s.getFileEntry(0), "../");
    System.out.println("Downloading to :" + to);
    Thread.sleep(5000);
    File dst =new File(to);
    assertTrue(dst.exists());
    assertEquals(dst.length(), d.getSize());
    closeAll();
    Thread.sleep(1000);
  }

}
