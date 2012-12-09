package fish.finder;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import fish.finder.proto.Message.MessageType;
import fish.finder.proto.Message.Request;

public class Connection implements Runnable {

  public static final int DEFAULT_TTL = 5;
  private Client client;
  private Socket socket;
  private boolean open = false;
  private long remoteIdentity;

  public Connection(Client client, Socket socket) {
    this.socket = socket;
    this.client = client;

    Request request = readMessage();
    if (request != null && request.getType() == MessageType.PING) {
      remoteIdentity = request.getSource();
      Request response = createRequest(MessageType.PONG).build();
      send(response);
      open = true;
    }
  }
  
  public Connection(Client client, String host, int port) {
    try {
      socket = new Socket(host, port);
      socket.setKeepAlive(true);
      this.client = client;

      // Communicate identity.
      Request request = createRequest(MessageType.PING).build();
      send(request);
      Request response = readMessage();
      if (response != null && response.getType() == MessageType.PONG) {
        remoteIdentity = response.getSource();
        open = true;
      } else {
        socket.close();
      }
    } catch (Exception e) {
      System.err.println("Error connection to " + host + ":" + e.getMessage());
      open = false;
    }
  }

  public Request.Builder createRequest(MessageType type) {
    return Request.newBuilder()
        .setSource(client.getLocalIdentity())
        .setType(type);
  }

  public void send(Request r) {
    try {
      r.writeTo(socket.getOutputStream());
    } catch (IOException e) {
      open = false;
    }
  }
  
  public Request readMessage() {
    try {
      return Request.parseFrom(socket.getInputStream());
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
  }

}
