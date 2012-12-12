package fish.finder;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;

import com.google.protobuf.InvalidProtocolBufferException;

import fish.finder.proto.Message.FileEntry;
import fish.finder.proto.Message.MessageType;
import fish.finder.proto.Message.Request;
import fish.finder.proto.Message.SearchResults;

public class Connection implements Runnable {

  public static boolean DEBUG = false;
  public static final int DEFAULT_TTL = 5;
  public static final long BROADCAST = -1L;
  public static final long NULL_ADRESS = 0L;

  private Client client;
  private Socket socket;
  private boolean open = false;
  private boolean host = false;
  private long remoteIdentity;

  public long getRemoteIdentity() {
    return remoteIdentity;
  }

  public long getLocalIdentity() {
    return client.getLocalIdentity();
  }

  public Connection(Client client, Socket socket) {
    this.socket = socket;
    this.client = client;
    this.host = true;

    Request request = readMessage();
    if (request != null && request.getType() == MessageType.PING) {
      remoteIdentity = request.getSource();
      Request response = client.createRequest(MessageType.PONG).build();
      send(response);
      success();
    }
  }
  
  public Connection(Client client, String host, int port) {
    try {
      this.host = false;
      socket = new Socket(host, port);
      socket.setKeepAlive(true);
      this.client = client;

      // Communicate identity.
      Request request = client.createRequest(MessageType.PING).build();
      send(request);
      Request response = readMessage();
      if (response != null && response.getType() == MessageType.PONG) {
        remoteIdentity = response.getSource();
        success();
      } else {
        socket.close();
      }
    } catch (Exception e) {
      System.err.println("Error connection to " + host + ":" + e.getMessage());
      open = false;
    }
  }

  private void success() {
    open = true;
    client.getRoute().addConnection(this);
    new Thread(this).start();
  }

  public void close() throws IOException {
    open = false;
    socket.close();
  }

  public boolean isHost() {
    return host;
  }

  public String getRemoteIP() {
    return socket.getInetAddress().getHostAddress();
  }
  
  public int getRemotePort() {
    return socket.getPort();
  }

  public void send(Request r) {
    try {
      OutputStream out = socket.getOutputStream();
      synchronized (out) {
        if (DEBUG) {
          System.out.println(toString() + ": Sending: \n" + r.toString());
        }
        byte[] b = r.toByteArray();
        int length = b.length;
        out.write((length & 0xff00) >> 8);
        out.write((length & 0x00ff));
        out.write(b);
      }
    } catch (IOException e) {
      open = false;
    }
  }
  
  public synchronized Request readMessage() {
    try {
      int length = (socket.getInputStream().read() << 8) + 
                   socket.getInputStream().read();
      if( length < 0) return null;

      byte[] b = new byte[length];
      socket.getInputStream().read(b);
      Request r = Request.parseFrom(b);
      if (DEBUG) {
        System.out.println(toString() + ": ReadMessage(" + length + "): \n" + 
                           r.toString());
      }
      return r;
    } catch (IOException e) {
      open = false;
      return null;
    }
  }
  
  public boolean isOpen() {
    return open;
  }

  @Override
  public void run() {
    while (!socket.isClosed() && open) {
      Request message = readMessage();
      if (message == null) break;
      Request response = null;

      client.getRoute().learn(this, message);

      long destination = message.getDestination();
      if (destination != Connection.BROADCAST && 
          message.getDestination() != client.getLocalIdentity()) {
        client.getRoute().route(message);
      } else {
        switch (message.getType().getNumber()) {
          case MessageType.PING_VALUE:
            response = client.createRequest(MessageType.PONG)
                .setDestination(message.getSource()) 
                .build();
            send(response);
            break;
  
          case MessageType.PONG_VALUE:
            break;
  
          case MessageType.SEARCH_VALUE:
            String q = new String(message.getData().toByteArray());
            ArrayList<FileEntry> results = client.search(q);
            SearchResults.Builder resultsBuilder = SearchResults.newBuilder();
            for (int i = 0; i < results.size(); ++i) {
              resultsBuilder.addResults(results.get(i));
            }
            response = client.createRequest(MessageType.RESULTS)
                .setData(resultsBuilder.build().toByteString())
                .setDestination(message.getSource())
                .build();
            send(response);
            break;
          case MessageType.RESULTS_VALUE:
            try {
              SearchResults remoteResults = null;
              remoteResults = SearchResults.parseFrom(message.getData());
              client.addResults(remoteResults);
            } catch (InvalidProtocolBufferException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
            break;
          default: 
            System.out.println(toString() + ": unhandled message: \n" + 
                               message.toString());
        }
        if (message.getDestination() == Connection.BROADCAST) {
          client.getRoute().route(message);
        }
      }
    }
  }

  public String toString() {
    if (socket != null) {
      return socket.getRemoteSocketAddress().toString() + "(" + remoteIdentity + ")";
    } else {
      return Connection.class.toString();
    }
  }

}
