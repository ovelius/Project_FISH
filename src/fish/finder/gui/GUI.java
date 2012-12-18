package fish.finder.gui;


import javax.swing.JFrame;
import javax.swing.JButton;
import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JFileChooser;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.border.BevelBorder;
import javax.swing.AbstractListModel;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import fish.finder.Client;
import fish.finder.Connection;
import fish.finder.FileChannel;
import fish.finder.FileMessageChannel;
import fish.finder.Index;
import fish.finder.Route;
import fish.finder.proto.Message.FileEntry;

import java.awt.Font;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;

import javax.swing.JCheckBox;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.JTabbedPane;
import java.awt.GridLayout;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JProgressBar;
import java.awt.SystemColor;
import java.awt.FlowLayout;
import java.awt.CardLayout;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.factories.FormFactory;
import javax.swing.JTextPane;

public class GUI extends JFrame implements Runnable {

  private Client client;
  private JLabel statusLabel;
  private ImageIcon loader;
  private JLabel shareStatus;
  private JPanel transferPanel;
  private JScrollPane transferPanelHost;
  private JTabbedPane tabbedPane;
  private DefaultListModel<String> shareList = new DefaultListModel<String>();

  public GUI() {
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setBounds(100, 100, 448, 426);
    JPanel panel = new JPanel();
    panel.setToolTipText("");
    getContentPane().add(panel, BorderLayout.CENTER);
    panel.setLayout(null);
    
    statusLabel = new JLabel("Status: Disconnected");
    statusLabel.setFont(new Font("Tahoma", Font.BOLD, 14));
    statusLabel.setBounds(10, 11, 275, 14);
    panel.add(statusLabel);
    
    tabbedPane = new JTabbedPane(JTabbedPane.TOP);
    tabbedPane.setBounds(10, 36, 414, 331);
    panel.add(tabbedPane);
    
    JPanel connection = new JPanel();
    connection.setToolTipText("");
    tabbedPane.addTab("Connection", null, connection, null);
    connection.setLayout(null);
    
    JPanel filePanel = new JPanel();
    tabbedPane.addTab("Share files", null, filePanel, null);
    filePanel.setLayout(null);
    
    JPanel searchPanel = new JPanel();
    tabbedPane.addTab("Search", null, searchPanel, null);
    searchPanel.setLayout(null);
    
    final JLabel fileNameLabel = new JLabel("");
    fileNameLabel.setFont(new Font("Tahoma", Font.BOLD, 11));
    fileNameLabel.setBounds(308, 38, 91, 14);
    searchPanel.add(fileNameLabel);
    
    JLabel lblSize = new JLabel("Size:");
    lblSize.setBounds(252, 63, 46, 14);
    searchPanel.add(lblSize);
    
    final JLabel fileSizeLabel = new JLabel("");
    fileSizeLabel.setFont(new Font("Tahoma", Font.BOLD, 11));
    fileSizeLabel.setBounds(308, 63, 91, 14);
    searchPanel.add(fileSizeLabel);
    
    JButton btnShareFolder = new JButton("Share folder...");
    btnShareFolder.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String dir = selectDir("Select a directory to share");
        if (dir != null) {
          client.shareDirectory(dir);
        } 
      }
    });
    btnShareFolder.setBounds(10, 11, 133, 23);
    filePanel.add(btnShareFolder);
    
    JList<String> shareGUIList = new JList<String>();
    shareGUIList.setBounds(10, 45, 389, 247);
    shareGUIList.setModel(shareList);
    filePanel.add(shareGUIList);
    
    shareStatus = new JLabel("");
    shareStatus.setFont(new Font("Tahoma", Font.BOLD, 11));
    shareStatus.setBounds(153, 11, 246, 14);
    filePanel.add(shareStatus);

    final JList<String> searchResultList = new JList<String>();
    searchResultList.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent arg0) {
        int index = searchResultList.getSelectedIndex();
        FileEntry e = 
              client.getSearchResults().getFileEntry(index);
        fileNameLabel.setText(e.getName());
        fileSizeLabel.setText(Index.sizeToUnit(e.getSize()));
      }
    });
    searchResultList.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent arg0) {
        if (arg0.getFirstIndex() == arg0.getLastIndex()) {
          FileEntry e = 
              client.getSearchResults().getFileEntry(arg0.getFirstIndex());
          fileNameLabel.setText(e.getName());
          fileSizeLabel.setText(Index.sizeToUnit(e.getSize()));
        }
      }
    });
    searchResultList.setBounds(10, 37, 232, 255);
    searchPanel.add(searchResultList);
    
    final JButton joinBtn = new JButton("Join");
    joinBtn.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (client == null) {
          int port = Integer.valueOf(listenPort.getText());
          try {
            client = new Client(port);
            searchResultList.setModel(client.getSearchResults());
            joinBtn.setText("Leave");
          } catch (ClassNotFoundException | IOException  e1) {
            JOptionPane.showMessageDialog(GUI.this, 
                "Unable to join: " + e1.toString());
          }
        } else {
          client.close();
          client = null;
          joinBtn.setText("Join");
        }
      }
    });
    joinBtn.setBounds(10, 9, 103, 31);
    connection.add(joinBtn);
    
    JLabel label = new JLabel("Port:");
    label.setBounds(323, 9, 24, 14);
    connection.add(label);
    
    listenPort = new JTextField();
    listenPort.setBounds(357, 9, 42, 20);
    listenPort.setText("9119");
    listenPort.setColumns(10);
    connection.add(listenPort);
    
    JLabel lblConnectManuallyTo = new JLabel("Connect manually to node:");
    lblConnectManuallyTo.setBounds(10, 273, 135, 14);
    connection.add(lblConnectManuallyTo);
    
    nodeName = new JTextField();
    nodeName.setText("localhost");
    nodeName.setColumns(10);
    nodeName.setBounds(165, 270, 135, 20);
    connection.add(nodeName);
    
    JButton connectBtn = new JButton("Connect");
    connectBtn.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (client != null) {
          try {
            client.addConnection(nodeName.getText());
          } catch (IOException e1) {
            e1.printStackTrace();
          }
        }
      }
    });
    connectBtn.setBounds(310, 269, 89, 23);
    connection.add(connectBtn);
    
    
    searchQuery = new JTextField();
    searchQuery.setBounds(10, 6, 232, 20);
    searchQuery.setColumns(10);
    searchPanel.add(searchQuery);
    
    JButton button_2 = new JButton("Search");
    button_2.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (client != null) {
          String q = searchQuery.getText();
          client.remoteSearch(q);
        }
      }
    });
    button_2.setBounds(255, 5, 144, 23);
    searchPanel.add(button_2);
    
    JLabel lblName = new JLabel("Name:");
    lblName.setBounds(252, 38, 46, 14);
    searchPanel.add(lblName);

    JButton downloadBtn = new JButton("Download");
    downloadBtn.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (client != null) {
          FileEntry remoteFile = 
              client.getSearchResults()
                  .getFileEntry(searchResultList.getSelectedIndex());
          String dir = selectDir("Select where you want to save the file");
          if (dir != null) {
            client.downloadFile(remoteFile, dir);
            tabbedPane.setSelectedComponent(transferPanelHost);
          } 
        }
      }
    });
    downloadBtn.setBounds(252, 269, 147, 23);
    searchPanel.add(downloadBtn);
    
    transferPanel = new JPanel();
    transferPanelHost = new JScrollPane(transferPanel);  
    tabbedPane.addTab("Transfers", null, transferPanelHost, null);
    transferPanel.setLayout(null);
    
    JPanel miscPanel = new JPanel();
    tabbedPane.addTab("Misc", null, miscPanel, null);
    
    final JCheckBox debugCheck = new JCheckBox("debug");
    debugCheck.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent arg0) {
        boolean debug = debugCheck.isSelected();
        Route.DEBUG = debug;
        Connection.DEBUG = debug;
        Client.DEBUG = debug;
        Index.DEBUG = debug;
      }
    });
    miscPanel.add(debugCheck);
    
    JButton button_3 = new JButton("Network topology...");
    button_3.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        new TopologyMap(client).setVisible(true);
      }
    });
    miscPanel.add(button_3);
    
    JPanel workingPanel = new JPanel();
    workingPanel.setBounds(298, 11, 126, 40);
    panel.add(workingPanel);
    workingPanel.setLayout(null);
    
    JLabel operationLabel = new JLabel("");
    operationLabel.setBounds(0, 0, 126, 40);
    workingPanel.add(operationLabel);

    addWindowListener(new java.awt.event.WindowAdapter() {
      public void windowClosing(java.awt.event.WindowEvent e) {
          if (client != null) {
            client.close();
            client = null;
          }
      }
    });
  }

  private JPanel createNewTransferPanel() {
    JPanel sampleTransfer = new JPanel();
    sampleTransfer.setLayout(null);
    
    JProgressBar progressBar = new JProgressBar();
    progressBar.setBounds(10, 54, 369, 14);
    sampleTransfer.add(progressBar);
    
    JLabel fileTransferNameLabel = new JLabel("Name:");
    fileTransferNameLabel.setBounds(10, 11, 200, 14);
    sampleTransfer.add(fileTransferNameLabel);
    
    JLabel transferSpeedLabel = new JLabel("Statistics");
    transferSpeedLabel.setBounds(208, 11, 200, 14);
    sampleTransfer.add(transferSpeedLabel);
    
    JLabel transferDestionation = new JLabel("Destination:");
    transferDestionation.setBounds(10, 36, 300, 14);
    sampleTransfer.add(transferDestionation);
    return sampleTransfer;
  }
  
  private HashMap<String, JPanel> transfers = new HashMap<String, JPanel>();


  private void modelFileChannels(HashMap<String, FileChannel> channels) {
    for (String fileKey : channels.keySet()) {
      FileChannel ch = channels.get(fileKey);
      JPanel transfer = transfers.get(fileKey);
      if (transfer == null) {
        transfer = createNewTransferPanel();
        transfers.put(fileKey, transfer);
        int top = 10 + transferPanel.getComponentCount()*80;
        transfer.setBounds(10, top, 389, 80);
        transferPanel.add(transfer);
      }
      JLabel dst = (JLabel) transfer.getComponent(3);
      dst.setText("Destination folder:" + ch.getDirectory());

      JLabel stats = (JLabel) transfer.getComponent(2);
      stats.setText("Precent complete:" + Index.DECIMAL_FORMAT.format(100*ch.getPercentComplete()) + "%");

      JLabel name = (JLabel) transfer.getComponent(1);
      name.setText("Name: " + ch.getRemoteFile().getName());

      JProgressBar progress = (JProgressBar) transfer.getComponent(0);
      progress.setValue((int)(ch.getPercentComplete()*100));
    }
  }
  
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private JTextField listenPort;
  private JTextField nodeName;
  private JTextField searchQuery;

  @Override
  public void run() {
    while (this.isVisible()) {
      Client c = client;
      shareList.removeAllElements();
      if (c != null) {
        int directPeers = c.getRoute().directConnections();
        int peers = c.getRoute().reachableNodes();
        this.statusLabel.setText("Status: Connected to " + peers + 
                                 " peers, " + directPeers + " direct");
        for (String dir : c.getSharedDirectories()) {
          shareList.addElement(dir);
        }
        shareStatus.setText("Sharing " + client.getSharedFileCount() + 
                            " files, total " + 
                            Index.sizeToUnit(client.getShareFileSize()));
        modelFileChannels(client.getFileChannels());
      } else {
        this.statusLabel.setText("Status: Disconnected");
      }
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
      }
    }
  }

  public static String selectDir(String title) {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle(title);
    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    fileChooser.setAcceptAllFileFilterUsed(false);

    if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
      return fileChooser.getSelectedFile().getAbsolutePath();
    }
    return null;
  }
  
  public static void main(String[] arg) {
    GUI gui = new GUI();
    gui.setVisible(true);
    new Thread(gui).start();
  }
}
