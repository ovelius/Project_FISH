package fish.finder.test;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Test;

import fish.finder.proto.Message.FileEntry;
import fish.finder.proto.Message.RequestFilePart;
import fish.finder.Index;

public class TestIndex {

  @Test
  public void testAddToIndex() {
    Index index = new Index();
    index.addDirectorySyncrhonous(".");

    ArrayList<FileEntry> results = index.search("client");
    assertTrue(results.size() > 0);
    System.out.println("Results: " + results.size());

    for (FileEntry e : results) {
      System.out.println(e.toString());
    }
  }

  @Test
  public void testGetFileChunk() {
    Index index = new Index();
    index.addDirectorySyncrhonous(".");

    ArrayList<FileEntry> results = index.search("client");
    assertTrue(results.size() > 0);
    System.out.println("Results: " + results.size());

    FileEntry e = results.get(0);
    RequestFilePart request = RequestFilePart.newBuilder()
        .setFile(e)
        .setFromByte(0)
        .setToByte(1)
        .build();
    RequestFilePart response = index.requestFileChunk(request);
    assertNotNull(response);
    assertTrue(response.getFile().getHash().equals(e.getHash()));
    assertEquals(0, response.getFromByte());
    assertEquals(1, response.getToByte());
    assertTrue(response.getData().size() == 1);
  }

}
