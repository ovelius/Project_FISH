package fish.finder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import com.google.protobuf.ByteString;

import fish.finder.gui.SearchResultModel;
import fish.finder.proto.Message.ConnectionData;
import fish.finder.proto.Message.FileEntry;
import fish.finder.proto.Message.MessageType;
import fish.finder.proto.Message.Request;
import fish.finder.proto.Message.SearchResults;

public class Client implements Runnable {
  public static boolean DEBUG = false;
  public final static int LISTEN_PORT = 9119;
  public final static String NODE_CACHE = "HashSet-String-cache.object";

  private ArrayList<String> sharedDirectories = new ArrayList<String>();
  private SearchResultModel searchResults = new SearchResultModel();

  private String nodeCache;
  private HashSet<ConnectionData> nodes;

  private Index index;
  private ServerSocket socket;
  private Route route;
  
  private long localIdentity;

  public Client(int port) throws ClassNotFoundException, IOException {
    this(port, NODE_CACHE);
  }
  public Client() throws ClassNotFoundException, IOException {
    this(LISTEN_PORT, NODE_CACHE);
  }
  public Client(int port, String cacheFile) throws IOException, ClassNotFoundException {
    nodeCache = cacheFile;
    socket = new ServerSocket(port);
    nodes = new HashSet<ConnectionData>();
    index = new Index();
    route = new Route();

    localIdentity = Connection.BROADCAST;
    while (localIdentity == Connection.BROADCAST) {
      localIdentity = new Random().nextLong();
    }

    File fileCache = new File(cacheFile);
    if (fileCache.exists()) {
      ObjectInputStream ois = 
          new ObjectInputStream(new FileInputStream(nodeCache));
      nodes  = (HashSet<ConnectionData>)ois.readObject(); 
      if (DEBUG) {
        System.out.println(toString() + ": read " + nodes.size() + " nodes from cache.");
      }
      ois.close();
    }
    /*if (nodes.isEmpty()) {
      nodes.add("o.is-a-geek.com");
    }*/
    new Thread(this).start();
  }
  
  public Long connectoTo(Client other) throws IOException {
    return addConnection(other.getListenningIP(), other.getListenningPort());
  }

  public Long addConnection(ConnectionData d) throws IOException {
    return addConnection(d.getHost(), d.getPort());
  }

  public Long addConnection(String node) throws IOException {
    int port = LISTEN_PORT;
    String ip = node;
    if (node.contains(":")) {
      ip = node.substring(0, node.indexOf(":"));
      port = Integer.valueOf(node.substring(node.indexOf(":")+1));
    }
    return addConnection(ip, port);
  }
  
  public Long addConnection(String host, int port) throws IOException {
    Connection c = new Connection(this, host, port);
    if (c.isOpen()) {
      if (c.getRemoteIdentity() != this.localIdentity) {
        return c.getRemoteIdentity();
      } else {
        c.close();
        if (DEBUG) {
          System.out.println(toString()+ ": Dropped connection to self");
        }
      }
    }
    return null;
  }

  public void close() {
    try {
      socket.close();
    } catch (IOException e) { }
    nodes.clear();
    ArrayList<Connection> connections = new ArrayList<Connection>();
    connections.addAll(route.getDirectConnections());
    for (Connection c : connections) {
      if (c != null && c.isOpen()) {
        try {
            c.close();
        } catch (IOException e) { }
      }
      if (!c.isHost()) {
        nodes.add(c.getConnectionData());
      }
    }
    File fileCache = new File(nodeCache);
    ObjectOutputStream oos;
    try {
      oos = new ObjectOutputStream(new FileOutputStream(fileCache));
      oos.writeObject(nodes);
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public void shareDirectory(String dir) {
    File f = new File(dir);
    if (f.exists()) {
      sharedDirectories.add(f.getAbsolutePath());
      index.addDirectoryAsynchronous(dir);
    }
  }

  public ArrayList<String> getSharedDirectories() {
    return this.sharedDirectories;
  }

  public long getLocalIdentity() {
    return localIdentity;
  }
  
  public Route getRoute() {
    return route;
  }
  
  public int getSharedFileCount() {
    return index.getFileCount();
  }
  
  public long getShareFileSize() {
    return index.getTotalFileSize();
  }

  public int getListenningPort() {
    return socket.getLocalPort();
  }
  
  public String getListenningIP()  {
    return socket.getInetAddress().getHostAddress();
  }

  public Request.Builder createRequest(MessageType type) {
    return Request.newBuilder()
        .setTtl(Connection.DEFAULT_TTL)
        .setSource(getLocalIdentity())
        .setType(type);
  }

  public void remoteSearch(String q) {
    searchResults.clear();
    Request r = createRequest(MessageType.SEARCH)
          .setData(ByteString.copyFrom(q.getBytes()))
          .setDestination(Connection.BROADCAST).build();
    route.route(r, null);
  }

  public void broadcastPing() {
    searchResults.clear();
    Request r = createRequest(MessageType.PING)
          .setDestination(Connection.BROADCAST).build();
    route.route(r, null);
  }

  public SearchResultModel getSearchResults() {
    return this.searchResults;
  }
  
  public synchronized void addResults(SearchResults results) {
    for (int i = 0; i < results.getResultsCount(); ++i) {
      searchResults.addResult(results.getResults(i));
    }
  }
  
  public ArrayList<FileEntry> search(String q) {
    return index.search(q);
  }

  @Override
  public void run() {
    for (ConnectionData node : nodes) {
      Long remoteId = null;
      try {
        remoteId = addConnection(node);
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      if (remoteId != null) {
        if (DEBUG) {
          System.out.println(toString() + ": connected to " + node);
        }
      } else if (DEBUG) {
        System.out.println(toString() + ": connection failed to " + node);
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
      if (DEBUG) {
        System.out.println(toString() + ": accepted connection from " + adress);
      }
      Connection c = new Connection(this, newConnection);
    }
  }

  public String toString() {
    return "Client:" + socket.getLocalPort() + ":";
  }
}
