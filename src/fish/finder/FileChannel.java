package fish.finder;

import fish.finder.proto.Message.FileEntry;
import fish.finder.proto.Message.FishMessage;

public abstract class FileChannel implements Runnable {
  protected String directory;
  protected FileEntry remoteFile;
  protected Client client;

  public static final int MAX_CHUNK_SIZE = 32000;

  public FileChannel(Client client,
      FileEntry remoteFile, String directory) {
    this.client = client;
    this.remoteFile = remoteFile;
    this.directory = directory;
  }

  public String getDirectory() {
    return directory;
  }

  public FileEntry getRemoteFile() {
    return remoteFile;
  }

  public Client getClient() {
    return client;
  }
  
  public abstract boolean receiveChunk(FishMessage message);
  public abstract boolean active();
  public abstract double getPercentComplete();
  public abstract boolean wasSuccessFul();
  public abstract String getFullLocalFileName();
  public abstract void startTransfer();
}
