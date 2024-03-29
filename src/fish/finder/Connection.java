package fish.finder;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;

import com.google.protobuf.InvalidProtocolBufferException;

import fish.finder.proto.Message.ConnectionData;
import fish.finder.proto.Message.FileEntry;
import fish.finder.proto.Message.FishMessage;
import fish.finder.proto.Message.MessageType;
import fish.finder.proto.Message.RequestFilePart;
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
  
  private long lastMessage = 0;
  private int messageCount = 0;
  
  private long bytesIn, bytesOut = 0;

  public long getBytesIn() {
    return bytesIn;
  }

  public long getBytesOut() {
    return bytesOut;
  }

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

    FishMessage request = readMessage();
    if (request != null && request.getType() == MessageType.PING) {
      remoteIdentity = request.getSource();
      FishMessage response = client.createRequest(MessageType.PONG).build();
      send(response);
      success();
    }
  }
  
  public Connection(Client client, ConnectionData data) {
    this(client, data.getHost(), data.getPort());
  }
  
  public Connection(Client client, String host, int port) {
    try {
      this.host = false;
      socket = new Socket(host, port);
      socket.setKeepAlive(true);
      this.client = client;

      // Communicate identity.
      FishMessage request = client.createRequest(MessageType.PING).build();
      send(request);
      FishMessage response = readMessage();
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

  public String getConnectString() {
    return getRemoteIP() + ":" + this.getRemotePort();
  }
  
  private void success() {
    open = true;
    client.getRoute().addConnection(this);
    new Thread(this).start();
  }

  public void close() {
    open = false;
    try {
      socket.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public ConnectionData getConnectionData() {
    return ConnectionData.newBuilder()
        .setHost(this.getRemoteIP())
        .setPort(getRemotePort()).build();
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

  public void send(FishMessage r) {
    try {
      OutputStream out = socket.getOutputStream();
      synchronized (out) {
        byte[] b = r.toByteArray();
        if (DEBUG) {
          System.out.println(toString() + ": Sending(" + b.length + "): \n" + r.toString());
        }
        int length = b.length;
        bytesOut += b.length + 2;
        out.write((length & 0xff00) >> 8);
        out.write((length & 0x00ff));
        out.write(b);
      }
    } catch (IOException e) {
      e.printStackTrace();
      open = false;
    }
  }
  
  public synchronized FishMessage readMessage() {
    try {
      int length = (socket.getInputStream().read() << 8) + 
                   socket.getInputStream().read();
      if( length < 0) return null;

      byte[] b = new byte[length];
      int readData = 0;
      while (readData < length) {
        readData += socket.getInputStream().read(b, readData, b.length - readData);
      }
      bytesIn += length + 2;
      FishMessage r = FishMessage.parseFrom(b);
      if (DEBUG) {
        System.out.println(toString() + ": ReadMessage(" + length + "): \n" + 
                           r.toString());
      }
      return r;
    } catch (IOException e) {
      open = false;
     // e.printStackTrace();
      return null;
    }
  }
  
  public boolean isOpen() {
    return open;
  }

  private boolean handleMessage() {
    FishMessage message = readMessage();
    if (message == null) {
      System.err.println(toString() + ": readMessage = NULL");
      return false;
    }
    FishMessage response = null;

    client.getRoute().learn(this, message);

    long destination = message.getDestination();
    if (destination != Connection.BROADCAST && 
        message.getDestination() != client.getLocalIdentity()) {
      client.getRoute().route(message, this);
    } else {

      client.getRoute().learnConnectionData(message, this);

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
            FileEntry result = FileEntry.newBuilder(results.get(i))
                .setHost(getLocalIdentity())
                .build();
            resultsBuilder.addResults(result);
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

        case MessageType.REQUEST_FILE_PART_VALUE:
          try {
            RequestFilePart requestData = 
                RequestFilePart.parseFrom(message.getData());
            RequestFilePart responseData = client.getFileChunk(requestData);
            if (responseData == null) {
              System.err.println("Unable to get file chunk for " + requestData);
              break;
            }
            response = client.createRequest(MessageType.RESPONSE_FILE_PART)
                .setDestination(message.getSource())
                .setData(responseData.toByteString())
                .build();
            send(response);
          } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
          }
          break;

        case MessageType.RESPONSE_FILE_PART_VALUE:
          client.processFileChunk(message);
          break;

        default: 
          System.err.println(toString() + ": unhandled message: \n" + 
                             message.toString());
          return false;
      }
      if (message.getDestination() == Connection.BROADCAST) {
        client.getRoute().route(message, this);
      }
    }
    return true;
  }
  
  @Override
  public void run() {
    boolean handleMessage = false;
    while (!socket.isClosed()) {
      handleMessage = handleMessage();
      if (!open || !handleMessage) {
        break;
      }
      messageCount++;
      lastMessage = System.currentTimeMillis();
    }
    String lost = toString() + ": Connection lost";
    if (socket.isClosed()) {
      lost += "Socket closed";
    }
    if (!open) {
      lost += " Open=false";
    }
    if (!handleMessage) {
      lost += " Unhandled mesasge";
    }
    System.err.println(lost);
    if (!socket.isClosed()) {
      try {
        socket.close();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    client.getRoute().connectionLost(this);
  }

  public String toString() {
    if (socket != null) {
      return socket.getRemoteSocketAddress().toString() + "(" + remoteIdentity + ")";
    } else {
      return Connection.class.toString();
    }
  }

}
