package fish.finder;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.protobuf.ByteString;

import fish.finder.proto.Message.FileEntry;

public class Index {

  public static boolean DEBUG = false;

  private HashMap<String, FileEntry> fileNameToEntry = 
      new HashMap<String, FileEntry>();
  private TreeMap<String, ArrayList<String>> searchIndex = 
      new TreeMap<String, ArrayList<String>>();

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
  
  public void addDirectory(String dir) {
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
          addToIndex(in);
        }
      }
    } catch (Exception e) {
      System.err.println("Could not add: " + dir);
      e.printStackTrace();
    }
  }
}
