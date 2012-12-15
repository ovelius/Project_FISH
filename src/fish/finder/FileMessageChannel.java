package fish.finder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.google.protobuf.InvalidProtocolBufferException;

import fish.finder.proto.Message.FileEntry;
import fish.finder.proto.Message.MessageType;
import fish.finder.proto.Message.Request;
import fish.finder.proto.Message.RequestFilePart;

public class FileMessageChannel implements Runnable {

  public static boolean DEBUG = false;
  private String directory;
  private FileEntry remoteFile;
  private Client client;
  private long progress;
  private File file;

  private FileOutputStream out;
  private boolean successFul = false;
  private boolean abort = false;
  
  public static final int MAX_CHUNK = 32000;

  public String getDirectory() {
    return directory;
  }

  public FileEntry getRemoteFile() {
    return remoteFile;
  }

  public FileMessageChannel(Client client,
      FileEntry remoteFile, String directory) {
    this.client = client;
    this.remoteFile = remoteFile;
    this.directory = directory;
  }

  private void requestChunk() {
    RequestFilePart fileRequest = 
        RequestFilePart.newBuilder().setFile(remoteFile)
            .setFromByte(progress)
            .setToByte(progress + MAX_CHUNK).build();
    Request r = client.createRequest(MessageType.REQUEST_FILE_PART)
        .setDestination(remoteFile.getHost())
        .setData(fileRequest.toByteString())
        .build();
    client.getRoute().route(r);
  }
  
  private void chunkMissMatch(RequestFilePart r) {
    System.err.println("Out of order file transfer, progress: " + 
                       progress + " but got: \n" + r.getFromByte() +
                       ":" + r.getToByte() + " ?");
  }

  public synchronized boolean receiveChunk(Request message) {
    try {
      if (DEBUG) {
        System.out.println(this.toString() + ": Received chunk: \n" + message);
      }
      return receiveChunkInternal(message);
    } catch (IOException e) {
      e.printStackTrace();
      abort = true;
      return false;
    }
  }
  
  private boolean receiveChunkInternal(Request message) throws IOException {
   // System.out.println("Chunk data size:"+message.getData().size());
    RequestFilePart r = RequestFilePart.parseFrom(message.getData());
    if (r.getFile().getName().equals(remoteFile.getName()) &&
        r.getFile().getHash().equals(remoteFile.getHash())) {
      if (r.getFromByte() == progress) {
        byte[] fileData = r.getData().toByteArray();
        if (progress + fileData.length == r.getToByte()) {
          out.write(fileData);
          out.flush();
          progress += fileData.length;
          notifyAll();
          return true;
        }
      }
    }
    chunkMissMatch(r);
    return false;
  }
  
  public boolean active() {
    return out != null && !abort;
  }
  
  public double getPercentComplete() {
    return progress / (1.0*remoteFile.getSize());
  }
  
  public boolean wasSuccessFul() {
    return successFul && !abort;
  }
  public String getFullFileName() {
    return file.getAbsolutePath();
  }
  private void runTransfer() throws IOException, InterruptedException {
    int retries = 0;
    out = new FileOutputStream(file);
    while (!abort && progress < remoteFile.getSize() && retries < 2) {
      long currentProgress = progress;
      requestChunk();
      synchronized (this) {
        wait(5000);
      }
      if (DEBUG) {
        System.out.println("Progress \n" + remoteFile + "\nBytes " + progress + "/" + remoteFile.getSize());
      }
      if (progress == currentProgress) {
        retries++;
      }
    }
    if (DEBUG) {
      System.out.println("Transferred \n" + remoteFile + "\nBytes " + progress + "/" + remoteFile.getSize());
    }
    successFul = progress == remoteFile.getSize();
    out.close();
    out = null;
  }
  
  @Override
  public void run() {
    try {
      runTransfer();
    } catch (IOException | InterruptedException e) {
      successFul = false;
      try {
        out.close();
      } catch (IOException e1) {
        e1.printStackTrace();
      }
      out =  null;
      e.printStackTrace();
    }
  }

  public String toString() {
    return getFullFileName();
  }
  
  public void startTransfer() {
    file = new File(directory, remoteFile.getName());
    if (file.exists()) {
      file.delete();
    }
    try {
      file.createNewFile();
      new Thread(this).start();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
