package fish.finder;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import fish.finder.proto.Message.ConnectionData;
import fish.finder.proto.Message.FishMessage;


public class Route {

  public static boolean DEBUG = false;

  private int routedMessage = 0;
  private HashMap<Long, Long> routes = new HashMap<Long, Long>();
  private HashMap<Long, Integer> bestRouteTTL = new HashMap<Long, Integer>();
  private HashMap<Long, ConnectionData> connectionData 
      = new HashMap<Long, ConnectionData>();
  
  private HashMap<Long, Connection> idToConnection =
      new HashMap<Long, Connection>();
  
  private Client client;

  public Route(Client c) {
    this.client = c;
  }
  
  public static FishMessage.Builder reduceTTL(FishMessage message) {
    int ttl = message.getTtl();
    return FishMessage.newBuilder(message).setTtl(ttl - 1); 
  }
  public void addConnection(Connection connection) {
    if (DEBUG) {
      System.out.println(toString() + ": Added direct link to: " + 
                         connection.getRemoteIdentity());
    }
    Connection c = idToConnection.get(connection.getRemoteIdentity());
    if (c != null) {
      if (DEBUG) {
        System.out.println(toString() + ": Replaced connection to " + 
                           c.getRemoteIdentity());
      }
      if (c != null) {
        c.close();
      }
    }
    idToConnection.put(connection.getRemoteIdentity(), connection);
  }

  public int directConnections() {
    return idToConnection.size();
  }

  public void connectionLost(Connection c) {
    idToConnection.remove(c.getRemoteIdentity());
    final HashSet<Long> remoteRouteDestinations = new HashSet<Long>();
      for (Long dst : routes.keySet()) {
        if (routes.get(dst) == c.getRemoteIdentity()) {
          remoteRouteDestinations.add(dst);
        }
      }

    new Thread() { public void run() {
      for (Long dst : remoteRouteDestinations) {
        routes.remove(dst);
        if (connectionData.containsKey(dst)) {
          try {
            System.err.println("Trying to establish direct connection to " + connectionData.get(dst));
            client.addConnection(connectionData.get(dst));
          } catch (IOException e) {
            e.printStackTrace();
          }
          connectionData.remove(dst);
        }
      }
    }}.start();
  }
  
  public int reachableNodes() {
    return idToConnection.size() + routes.size();
  }

  public Collection<Connection> getDirectConnections() {
    return this.idToConnection.values();
  }
 
  public Collection<Long> getViaPeers(Long peer) {
    ArrayList<Long> peers = new ArrayList<Long>();
    for (Long dst : routes.keySet()) {
      if (peer.equals(routes.get(dst))) {
        peers.add(dst);
      }
    }
    return peers;
  }

  private void putRouteAndConnectionData(FishMessage message, Connection connection) {
    routes.put(message.getSource(), connection.getRemoteIdentity());
    bestRouteTTL.put(message.getSource(), message.getTtl());
    learnConnectionData(message, connection);
  }
  
  public void learnConnectionData(FishMessage message, Connection connection) {
    if (message.getSource() != connection.getRemoteIdentity() && 
        message.hasSourceConnection()) {
      connectionData.put(message.getSource(), message.getSourceConnection());
    }
  }
  
  public String getConnectionData(Long dst) {
    ConnectionData d = connectionData.get(dst);
    if (d != null) {
      return d.getHost()+":"+d.getPort();
    }
    return null;
  }
  
  public boolean learn(Connection connection, FishMessage message) {
    if (connection.getRemoteIdentity() != message.getSource() &&
        !idToConnection.containsKey(message.getSource())) {
      if (!routes.containsKey(message.getSource())) {
        if (DEBUG) {
          System.out.println(toString() + ": Added link to: " + 
            message.getSource() + " via " + connection.getRemoteIdentity()); 
        }
        putRouteAndConnectionData(message, connection);
        return true;
      } else {
        int ttl = bestRouteTTL.get(message.getSource());
        if (ttl < message.getTtl()) {
          if (DEBUG) {
            System.out.println(toString() + ": Replaced link to: " + 
              message.getSource() + " via " + connection.getRemoteIdentity()); 
          }
          putRouteAndConnectionData(message, connection);
          return true;
        }
      }
    }
    return false;
  }

  public void printRoutes() {
    printRoutes(System.out);
  }

  public void printRoutes(PrintStream w) {
    w.println("Direct routes:");
    for (Connection connection : idToConnection.values()) {
      w.println(connection.getRemoteIdentity() + " : " +
                connection.getRemoteIP() + ":" + connection.getRemotePort());
    }
    w.println("Via routes:");
    for (Long dst : routes.keySet()) {
      w.println(routes.get(dst) + "->" + dst);
    }
  }

  public int getRoutedMessage() {
    return routedMessage;
  }
  public boolean hasRoute(Client c) {
    return hasRoute(c.getLocalIdentity());
  }

  public boolean hasRoute(Long id) {
    return idToConnection.containsKey(id) || routes.containsKey(id);
  }

  private void broadCast(FishMessage r, Connection sourceConnection) {
    for (Connection connection : idToConnection.values()) {
      if (connection.getRemoteIdentity() == r.getSource()) continue;
      if (sourceConnection == connection) continue;
      connection.send(r);
    }
  }
  public void route(FishMessage message) {
    route(message, null);
  }
  public void route(FishMessage message, Connection sourceConnection) {
    if (message.getTtl() <= 1) {
      System.err.println(toString() + ": TTL expired: \n" + message.toString());
      return;
    }
    FishMessage.Builder newMessageBuilder = reduceTTL(message);
    // Put connection data in each message.
    if (sourceConnection != null && !sourceConnection.isHost() &&
        message.getSource() == sourceConnection.getRemoteIdentity()) {
      newMessageBuilder
          .setSourceConnection(sourceConnection.getConnectionData());
    }
    message = newMessageBuilder.build();

    if (message.getDestination() == Connection.BROADCAST) {
      broadCast(message, sourceConnection);
      return;
    }

    Connection connection = idToConnection.get(message.getDestination());
    if (connection == null) {
      Long via = routes.get(message.getDestination());
      connection = idToConnection.get(via);
    }
    if (connection != null) {
      if (!connection.isOpen()) {
        idToConnection.remove(connection.getRemoteIdentity());
        connection = null;
      } else if (message.getSource() != connection.getRemoteIdentity()) {
        this.routedMessage++;
        connection.send(message);
      }
    }
    if (connection == null) {
      System.err.println(toString()+": " + message.getSource() + " -> " +
                         message.getDestination() + " no route to host: " + 
                         message.getDestination());
    }
  }
}
