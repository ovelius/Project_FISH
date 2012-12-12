package fish.finder.gui;


import javax.swing.JFrame;
import javax.swing.JButton;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JFileChooser;
import javax.swing.SwingConstants;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.border.BevelBorder;
import javax.swing.AbstractListModel;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import fish.finder.Client;
import fish.finder.Connection;
import fish.finder.Index;
import fish.finder.Route;
import fish.finder.proto.Message.FileEntry;

import java.awt.Font;
import java.io.IOException;
import javax.swing.JCheckBox;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

public class GUI extends JFrame implements Runnable {

  private Client client;
  private JLabel statusLabel;
  private JCheckBox debugCheck;
  private DefaultListModel<String> shareList = new DefaultListModel<String>();
  private JList<FileEntry> searchResults;

  public GUI() {
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setBounds(100, 100, 646, 445);
    JPanel panel = new JPanel();
    panel.setToolTipText("");
    getContentPane().add(panel, BorderLayout.CENTER);
    panel.setLayout(null);

    final JButton connectionButton = new JButton("Join");
    connectionButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (client != null) {
          client.close();
          client = null;
          connectionButton.setText("Join");
        } else {
          try {
            int port = Integer.valueOf(clientPort.getText());
            Client c = new Client(port);
            connectionButton.setText("Leave");
            client = c;
          } catch (ClassNotFoundException e1) {
            JOptionPane.showMessageDialog(GUI.this, "Uknown error: " + e1.getMessage());
          } catch (IOException e1) {
            JOptionPane.showMessageDialog(GUI.this, "Uknown error: "+ e1.getMessage());
          }
        }
      }
    });
    
    statusLabel = new JLabel("Status: Disconnected");
    statusLabel.setFont(new Font("Tahoma", Font.BOLD, 14));
    statusLabel.setBounds(10, 11, 414, 14);
    panel.add(statusLabel);
    connectionButton.setBounds(10, 36, 89, 23);
    panel.add(connectionButton);
    
    JList<String> shareList = new JList<String>();
    shareList.setBounds(10, 122, 196, 256);
    panel.add(shareList);
    shareList.setModel(this.shareList);
    
    JButton shareButton = new JButton("Share...");
    shareButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new java.io.File("."));
        chooser.setDialogTitle("Choose a directory to share");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
          client.shareDirectory(chooser.getSelectedFile().getAbsolutePath());
        }
      }
    });
    shareButton.setBounds(10, 88, 89, 23);
    panel.add(shareButton);
    
    JSeparator separator = new JSeparator();
    separator.setBounds(10, 70, 621, 7);
    panel.add(separator);
    
    textField = new JTextField();
    textField.setBounds(297, 89, 196, 20);
    panel.add(textField);
    textField.setColumns(10);
    
    JLabel lblSearchFile = new JLabel("Search file:");
    lblSearchFile.setBounds(225, 92, 62, 14);
    panel.add(lblSearchFile);
    
    JButton searchButton = new JButton("Search");
    searchButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (client != null) {
          String q = textField.getText();
          client.remoteSearch(q);
          searchResults.setModel(client.getSearchResults());
        }
      }
    });
    searchButton.setBounds(503, 88, 89, 23);
    panel.add(searchButton);
    
    JLabel lblNewLabel_1 = new JLabel("Transfers:");
    lblNewLabel_1.setBounds(10, 389, 89, 14);
    panel.add(lblNewLabel_1);
    
    searchResults = new JList<FileEntry>();
    searchResults.setBounds(235, 122, 357, 256);
    panel.add(searchResults);
    
    JLabel lblPort = new JLabel("Port:");
    lblPort.setBounds(109, 40, 30, 14);
    panel.add(lblPort);
    
    clientPort = new JTextField();
    clientPort.setText("9119");
    clientPort.setBounds(131, 37, 62, 20);
    panel.add(clientPort);
    clientPort.setColumns(10);
    
    JLabel lblConnectManually = new JLabel("Connect manually:");
    lblConnectManually.setBounds(203, 40, 95, 14);
    panel.add(lblConnectManually);
    
    nodeName = new JTextField();
    nodeName.setBounds(297, 37, 86, 20);
    panel.add(nodeName);
    nodeName.setColumns(10);
    
    JButton connectBtn = new JButton("Connect");
    connectBtn.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (client != null) {
          try {
            client.addConnection(nodeName.getText());
          } catch (IOException e1) {
            JOptionPane.showMessageDialog(GUI.this, 
                "Error connecting to " + nodeName.getText());
          }
        }
      }
    });
    connectBtn.setBounds(404, 36, 89, 23);
    panel.add(connectBtn);
    
    debugCheck = new JCheckBox("debug");
    debugCheck.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent arg0) {
        boolean debug = debugCheck.isSelected();
        Client.DEBUG = debug;
        Connection.DEBUG = debug;
        Route.DEBUG = debug;
        Index.DEBUG = debug;
      }
    });
    debugCheck.setBounds(530, 2, 62, 23);
    panel.add(debugCheck);
    
    JButton btnNewButton = new JButton("Network topology");
    btnNewButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (client != null) {
          new TopologyMap(client).setVisible(true);
        }
      }
    });
    btnNewButton.setBounds(405, 2, 119, 23);
    panel.add(btnNewButton);

    addWindowListener(new java.awt.event.WindowAdapter() {
      public void windowClosing(java.awt.event.WindowEvent e) {
          if (client != null) {
            client.close();
            client = null;
          }
      }
  });
  }

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private JTextField textField;
  private JTextField clientPort;
  private JTextField nodeName;

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
      } else {
        this.statusLabel.setText("Status: Disconnected");
      }
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
      }
    }
  }

  public static void main(String[] arg) {
    GUI gui = new GUI();
    gui.setVisible(true);
    new Thread(gui).start();
  }
}
