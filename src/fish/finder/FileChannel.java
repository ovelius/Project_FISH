package fish.finder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

import fish.finder.proto.Message.ConnectionData;
import fish.finder.proto.Message.FileEntry;

public class FileChannel implements Runnable {

  public static final int DEFAULT_PORT = 6810;
  private ServerSocket listenSocket; 
  private Socket clientSocket;

  private long fileSize;
  private long progress;

  public FileChannel() {
    openListenningSocketOnFreePort();
  }
  
  public void close() {
    if (listenSocket != null) {
      try {
        listenSocket.close();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    if (clientSocket != null) {
      try {
        clientSocket.close();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }
  
  public double progressPercent() {
    return progress / (1.0*fileSize);
  }
  
  public int getPort() {
    return listenSocket.getLocalPort();
  }
  
  private boolean openListenningSocketOnFreePort() {
    for (int i = 0; i < 5; ++i) {
      // Pick a random port above 1024
      int port = Math.abs((1024 + new Random().nextInt())) % 65535;
      try {
        listenSocket = new ServerSocket(port);
        return true;
      } catch (IOException e) {
      }
    }
    return false;
  }
  
  private Socket connectTo(ConnectionData connection) {
    try {
      Socket s = new Socket(connection.getHost(), connection.getPort());
      return s;
    } catch (IOException e) {
      return null;
    }
  }
  
  private Socket listenForConnection() {
    try {
      new Thread(this).start();
      return listenSocket.accept();
    } catch (IOException e) {
      clientSocket = null;
      return null;
    }
  }
  
  private boolean doTransfer(File file) {
    try {
      FileInputStream in = new FileInputStream(file);
      byte buf[] = new byte[clientSocket.getSendBufferSize() / 2];
      int read = 0;
      progress = 0;
      fileSize = file.length();
      do {
        read = in.read(buf, 0, buf.length);
        clientSocket.getOutputStream().write(buf, 0, read);
        progress += read;
      } while (read > 0);
      in.close();
    } catch (IOException io) {
      return false;
    }
    return true;
  }
  
  private boolean receiveFile(File file, FileEntry remoteFile) {
    try {
      FileOutputStream out = new FileOutputStream(file);
      byte buf[] = new byte[clientSocket.getReceiveBufferSize() / 2];
      int read = 0; 
      fileSize = remoteFile.getSize();
      progress = 0;
      do {
        read = clientSocket.getInputStream().read(buf, 0, buf.length);
        out.write(buf, 0, read);
        progress += read;
      } while (read > 0);
      out.close();
    } catch (Exception e) {
      return false;
    }
    return true;
  }
  
  public boolean receiveFile(String dir, FileEntry file, ConnectionData connection) {
    clientSocket = listenForConnection();
    if (clientSocket == null) {
      clientSocket = connectTo(connection);
    }
    if (clientSocket != null) {
      return this.receiveFile(new File(dir, file.getName()), file);
    } else {
      return false;
    }
  }

  public boolean transferFile(File file, ConnectionData connection) {
    // 1. Try to push the file to the Receiver.
    clientSocket = connectTo(connection);
    if (clientSocket == null) {
      // 2. Open a listenSocket to allow the other side to connect to us.
      clientSocket = listenForConnection();
    }

    if (clientSocket != null) {
      return doTransfer(file);
    } else {
      return false;
    }
  }
  
  @Override
  public void run() {
    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    if (clientSocket == null) {
      try {
        listenSocket.close();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }

}
