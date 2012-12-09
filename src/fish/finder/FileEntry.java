package fish.finder;

public class FileEntry {
  public FileEntry(String fileName, String hash, long size) {
    this.fileName = fileName;
    this.hash = hash;
    this.size = size;
  }
  public final String fileName;
  public final String hash;
  public final long size;

  public String toString() {
    return fileName+ " s:"+size;
  }
}