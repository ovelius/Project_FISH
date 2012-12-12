package fish.finder.gui;

import java.util.ArrayList;

import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import fish.finder.proto.Message.FileEntry;

public class SearchResultModel implements ListModel<FileEntry> {

  private ArrayList<FileEntry> searchResults = new ArrayList<FileEntry>();
  private ArrayList<ListDataListener> listeners = 
      new ArrayList<ListDataListener>();
  @Override
  public void addListDataListener(ListDataListener arg0) {
    listeners.add(arg0);
  }

  public void addResult(FileEntry result) {
    searchResults.add(result);
    for (ListDataListener l : listeners) {
      l.contentsChanged(new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, 
                                          searchResults.size() - 1,
                                          searchResults.size() - 1));
    }
  }
  
  public void clear() {
    for (ListDataListener l : listeners) {
      l.contentsChanged(new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, 
                                          0,
                                          searchResults.size() - 1));
    }
    searchResults.clear();
  }
  
  @Override
  public FileEntry getElementAt(int arg0) {
    return searchResults.get(arg0);
  }

  @Override
  public int getSize() {
    return searchResults.size();
  }

  @Override
  public void removeListDataListener(ListDataListener arg0) {
    listeners.remove(arg0);
  }

  public int size() {
    return searchResults.size();
  }

}
