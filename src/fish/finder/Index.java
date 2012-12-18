package fish.finder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.protobuf.ByteString;

import fish.finder.proto.Message.FileEntry;
import fish.finder.proto.Message.RequestFilePart;

public class Index implements Runnable {

  public static boolean DEBUG = false;

  private long totalFileSize = 0;
  private HashMap<String, FileEntry> fileNameToEntry = 
      new HashMap<String, FileEntry>();
  private HashMap<String, String> fileHashNameToPath =
      new HashMap<String, String>();
  private TreeMap<String, ArrayList<String>> searchIndex = 
      new TreeMap<String, ArrayList<String>>();

  private ArrayList<String> indexQueue = new ArrayList<String>();

  public static String getFileKey(FileEntry e) {
    return e.getSize() + "" + e.getHash();
  }
  
  public ArrayList<FileEntry> search(String q) {
    ArrayList<FileEntry> results = new ArrayList<FileEntry>();
    q = q.toUpperCase();
    SortedMap<String, ArrayList<String>> search = searchIndex.tailMap(q);
    for (String key : search.keySet()) {
      if (!key.startsWith(q)) break;
      for (String fileName : search.get(key)) {
        results.add(fileNameToEntry.get(fileName));
      }
      if (results.size() > 10) break;
    }
    return results;
  }

  private void addToIndex(File f) {
    String name = f.getName().toUpperCase();
    if (!name.startsWith(".") && name.contains(".")) {
      name = name.substring(0, name.lastIndexOf("."));
    } else if(name.startsWith(".")) {
      name = name.substring(1);
    }
    ArrayList<String> current = searchIndex.get(name);
    if (current == null) {
      current = new ArrayList<String>();
      searchIndex.put(name, current);
    }
    current.add(f.getAbsolutePath());
  }
  
  private ByteString addFile(File f) {
    try {
      if (f.exists() && f.canRead()) {
        MessageDigest md = MessageDigest.getInstance("MD5");
        InputStream is = new FileInputStream(f);
        try {
          is = new DigestInputStream(is, md);
        }
        finally {
          is.close();
        }
        return ByteString.copyFrom(md.digest());
      }
    } catch (Exception e) {
      System.err.println("Could not add file: " + f.getAbsolutePath());
      e.printStackTrace();
    }
    return null;
  }

  public int getFileCount() {
    return fileNameToEntry.size();
  }
  
  public static final double KiB = 1024;
  public static final double MiB = KiB*1024;
  public static final double GiB = MiB*1024;
  public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");

  public static String sizeToUnit(long size) {
    if (size / GiB >= 2) {
      return DECIMAL_FORMAT.format(size / GiB) + " GiB";
    } else if (size / MiB >= 2) {
      return DECIMAL_FORMAT.format(size / MiB) + " MiB";
    } else if (size / KiB >= 2) {
      return DECIMAL_FORMAT.format(size / KiB) + " KiB";
    }
    return size + " B";
  }

  public void addDirectorySyncrhonous(String dir) {
    addDirectory(dir);
  }
  public void addDirectoryAsynchronous(String dir) {
    synchronized (indexQueue) {
      indexQueue.add(dir);
      if (indexQueue.size() == 1) {
        new Thread(this).start();
      }
    }
  }

  public long getTotalFileSize() {
    return totalFileSize;
  }

  public RequestFilePart requestFileChunk(RequestFilePart request) {
    try {
      return requestFileChunkInternal(request);
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }
  private RequestFilePart requestFileChunkInternal(RequestFilePart request) throws IOException {
    FileEntry localFile = request.getFile();
    int length = (int)(request.getToByte() - request.getFromByte());
    String absPath = fileHashNameToPath.get(getFileKey(localFile));
    if (absPath != null) {
      RandomAccessFile in = new RandomAccessFile(absPath, "r");
      in.seek(request.getFromByte());
      byte[] buf = new byte[length];
      int read = 0;
      int currentSize = 0;
      do {
        currentSize = in.read(buf, read, length - read);
        if (currentSize > 0) {
          read += currentSize;
        }
      } while (currentSize > 0 && read < length);

      if (read > 0) {
        RequestFilePart response = RequestFilePart.newBuilder(request)
            .setToByte(request.getFromByte() + read)
            .setData(ByteString.copyFrom(buf, 0, read))
            .build();
        in.close();
        return response;
      }
      in.close();
    }
    return null;
  }
  
  private void addDirectory(String dir) {
    try {
      File f = new File(dir);
      File[] list = f.listFiles();
      for (File in : list) {
        if (in.isDirectory()) {
          addDirectory(in.getAbsolutePath());
        } else {
          ByteString hash = addFile(in);
          FileEntry entry = FileEntry.newBuilder()
              .setSize(in.length())
              .setName(in.getName())
              .setHash(hash)
              .build();
          fileNameToEntry.put(in.getAbsolutePath(), entry);
          fileHashNameToPath.put(getFileKey(entry), in.getAbsolutePath());
          totalFileSize += in.length();
          addToIndex(in);
        }
      }
    } catch (Exception e) {
      System.err.println("Could not add: " + dir);
      e.printStackTrace();
    }
  }

  @Override
  public void run() {
    while (true) {
      String dir = null;
      synchronized (indexQueue) {
        if (indexQueue.size() > 0) {
          dir = indexQueue.remove(0);
        }
      }
      if (dir != null) {
        addDirectory(dir);
      } else {
        break;
      }
    }
  }
}
