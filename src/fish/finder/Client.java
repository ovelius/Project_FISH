package fish.finder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

public class Client implements Runnable {
  public final static int LISTEN_PORT = 9119;
  public final static String NODE_CACHE = "HashSet-String-cache.object";

  private ArrayList<String> nodes;
  private HashMap<String, Connection> nodeConnections;

  private Index index;
  private ServerSocket socket;
  
  private long localIdentity;

  public Client() throws IOException, ClassNotFoundException {
    socket = new ServerSocket(LISTEN_PORT);
    nodes = new ArrayList<String>();
    index = new Index();
    nodeConnections = new HashMap<String, Connection>();

    localIdentity = new Random().nextLong();

    File fileCache = new File(NODE_CACHE);
    if (fileCache.exists()) {
      ObjectInputStream ois = 
          new ObjectInputStream(new FileInputStream(fileCache));
      nodes  = (ArrayList<String>)ois.readObject();  
    }
  }

  public long getLocalIdentity() {
    return localIdentity;
  }

  @Override
  public void run() {
    for (int i = 0; i < nodes.size(); ++i) {
      String node = nodes.get(i);
      if (nodeConnections.containsKey(node)) continue;
 
      Connection c = new Connection(this, node, LISTEN_PORT);
      if (c.isOpen()) {
        nodeConnections.put(node, c);
      }
    }
    while (!socket.isClosed()) {
      Socket newConnection;
      try {
        newConnection = socket.accept();
      } catch (IOException e) {
        continue;
      }
      String adress = socket.getInetAddress().toString();
      Connection c = new Connection(this, newConnection);
      if (c.isOpen()) {
        nodeConnections.put(adress, c);
      }
    }
  }
}
