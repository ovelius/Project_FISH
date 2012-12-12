package fish.finder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import fish.finder.proto.Message.Request;

public class Route {

  public static boolean DEBUG = false;

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

  public int reachableNodes() {
    return idToConnection.size() + routes.size();
  }

  public Collection<Connection> getDirectConnections() {
    return this.idToConnection.values();
  }
 
  public Collection<Long> getViaPeers(Long peer) {
    ArrayList<Long> peers = new ArrayList<Long>();
    for (Long dst : routes.keySet()) {
      if (routes.get(dst) == peer) {
        peers.add(dst);
      }
    }
    return peers;
  }

  public void learn(Connection connection, Request message) {
    if (connection.getRemoteIdentity() != message.getSource() &&
        !idToConnection.containsKey(message.getSource())) {
      if (!routes.containsKey(message.getSource())) {
        if (DEBUG) {
          System.out.println(toString() + ": Added link to: " + 
            message.getSource() + " via " + connection.getRemoteIdentity()); 
        }
        routes.put(message.getSource(), connection.getRemoteIdentity());
        bestRouteTTL.put(message.getSource(), message.getTtl());
      } else {
        int ttl = bestRouteTTL.get(message.getSource());
        if (ttl < message.getTtl()) {
          if (DEBUG) {
            System.out.println(toString() + ": Replaced link to: " + 
              message.getSource() + " via " + connection.getRemoteIdentity()); 
          }
          routes.put(message.getSource(), connection.getRemoteIdentity());
          bestRouteTTL.put(message.getSource(), message.getTtl());
        }
      }
    }
  }

  public boolean hasRoute(Client c) {
    return hasRoute(c.getLocalIdentity());
  }
  
  public boolean hasRoute(Long id) {
    return idToConnection.containsKey(id) || routes.containsKey(id);
  }
  
  private void broadCast(Request r) {
    for (Connection connection : idToConnection.values()) {
      if (connection.getRemoteIdentity() == r.getSource()) continue;
      connection.send(r);
    }
  }
  public void route(Request message) {
    message = reduceTTL(message);

    if (message.getTtl() <= 0) {
      System.err.println(toString() + ": TTL expired: \n" + message.toString());
      return;
    }

    if (message.getDestination() == Connection.BROADCAST) {
      broadCast(message);
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
      } else {
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
