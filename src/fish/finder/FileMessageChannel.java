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

  private String directory;
  private FileEntry remoteFile;
  private Client client;
  private long progress;

  private FileOutputStream out;
  private boolean successFul = false;
  private boolean abort = false;
  
  public static final int MAX_CHUNK = 32000;

  public FileMessageChannel(Client client,
      FileEntry remoteFile, String directory) {
    this.client = client;
  }

  private void requestChunk() {
    RequestFilePart fileRequest = 
        RequestFilePart.newBuilder().setFile(remoteFile)
            .setFromByte(progress)
            .setToByte(progress + MAX_CHUNK).build();
    Request r = client.createRequest(MessageType.REQUEST_FILE_PART)
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
      return receiveChunkInternal(message);
    } catch (IOException e) {
      e.printStackTrace();
      abort = true;
      return false;
    }
  }
  
  private boolean receiveChunkInternal(Request message) throws IOException {
    RequestFilePart r = RequestFilePart.parseFrom(message.getData());
    if (r.getFile().getName().equals(remoteFile.getName()) &&
        r.getFile().getHash().equals(remoteFile.getHash())) {
      if (r.getFromByte() == progress) {
        byte[] fileData = r.getData().toByteArray();
        if (progress + fileData.length == r.getToByte()) {
          out.write(fileData);
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
  
  private void runTransfer() throws IOException, InterruptedException {
    int retries = 0;
    out = new FileOutputStream(new File(directory, remoteFile.getName()));
    while (!abort && progress < remoteFile.getSize() && retries < 2) {
      long currentProgress = progress;
      requestChunk();
      wait(10000);
      if (progress == currentProgress) {
        retries++;
      }
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
    }
  }
  
  public void startTransfer() {
    new Thread(this).start();
  }
}
