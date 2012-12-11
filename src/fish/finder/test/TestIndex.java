package fish.finder.test;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Test;

import fish.finder.proto.Message.FileEntry;
import fish.finder.Index;

public class TestIndex {

  @Test
  public void testAddToIndex() {
    Index index = new Index();
    index.addDirectory(".");

    ArrayList<FileEntry> results = index.search("client");
    assertTrue(results.size() > 0);
    System.out.println("Results: " + results.size());

    for (FileEntry e : results) {
      System.out.println(e.toString());
    }
  }

}
