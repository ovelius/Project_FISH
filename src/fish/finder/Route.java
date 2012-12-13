package fish.finder;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import fish.finder.proto.Message.MessageType;
import fish.finder.proto.Message.Request;

public class Route {

  public static boolean DEBUG = false;

  private int routedMessage = 0;
  private HashMap<Long, Long> routes = new HashMap<Long, Long>();
  private HashMap<Long, Integer> bestRouteTTL = new HashMap<Long, Integer>();
  
  private HashMap<Long, Connection> idToConnection =
      new HashMap<Long, Connection>();

  public static Request reduceTTL(Request message) {
    int ttl = message.getTtl();
    return Request.newBuilder(message).setTtl(ttl - 1).build(); 
  }
  public void addConnection(Connection connection) {
    if (DEBUG) {
      System.out.println(toString() + ": Added direct link to: " + 
                         connection.getRemoteIdentity());
    }
    idToConnection.put(connection.getRemoteIdentity(), connection);
  }

  public int directConnections() {
    return idToConnection.size();
  }

  public void connectionLost(Connection c) {
    idToConnection.remove(c.getRemoteIdentity());
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

  public boolean learn(Connection connection, Request message) {
    if (connection.getRemoteIdentity() != message.getSource() &&
        !idToConnection.containsKey(message.getSource())) {
      if (!routes.containsKey(message.getSource())) {
        if (DEBUG) {
          System.out.println(toString() + ": Added link to: " + 
            message.getSource() + " via " + connection.getRemoteIdentity()); 
        }
        if (message.getSource() == connection.getLocalIdentity()) {
          System.err.println("Based on packet from "+connection.getRemoteIdentity()+"about to add route to self: \n" + message);
        }
        routes.put(message.getSource(), connection.getRemoteIdentity());
        bestRouteTTL.put(message.getSource(), message.getTtl());
        return true;
      } else {
        int ttl = bestRouteTTL.get(message.getSource());
        if (ttl < message.getTtl()) {
          if (DEBUG) {
            System.out.println(toString() + ": Replaced link to: " + 
              message.getSource() + " via " + connection.getRemoteIdentity()); 
          }
          routes.put(message.getSource(), connection.getRemoteIdentity());
          bestRouteTTL.put(message.getSource(), message.getTtl());
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

  private void broadCast(Request r) {
    broadCast(r, null);
  }
  private void broadCast(Request r, Connection sourceConnection) {
    for (Connection connection : idToConnection.values()) {
      if (connection.getRemoteIdentity() == r.getSource()) continue;
      if (sourceConnection == connection) continue;
      connection.send(r);
    }
  }
  public void route(Request message) {
    route(message, null);
  }
  public void route(Request message, Connection sourceConnection) {
    if (message.getTtl() <= 1) {
      System.err.println(toString() + ": TTL expired: \n" + message.toString());
      return;
    }
    message = reduceTTL(message);

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
