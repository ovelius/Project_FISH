package fish.finder;


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
import java.awt.Font;
import java.io.IOException;

public class GUI extends JFrame implements Runnable {

  private Client client;
  private JLabel statusLabel;
  private DefaultListModel<String> shareList = new DefaultListModel<String>();

  public GUI() {
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    
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
            Client c = new Client();
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
      }
    });
    searchButton.setBounds(503, 88, 89, 23);
    panel.add(searchButton);
    
    JLabel lblNewLabel_1 = new JLabel("Transfers:");
    lblNewLabel_1.setBounds(10, 389, 89, 14);
    panel.add(lblNewLabel_1);
    
    JList list_1 = new JList();
    list_1.setBounds(235, 122, 357, 256);
    panel.add(list_1);
  }

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private JTextField textField;

  @Override
  public void run() {
    while (this.isVisible()) {
      Client c = client;
      shareList.removeAllElements();
      if (c != null) {
        this.statusLabel.setText("Status: Connected to " + c.getConnectionCount() + " peers");
        for (String dir : c.getSharedDirectories()) {
          shareList.addElement(dir);
        }
      } else {
        this.statusLabel.setText("Status: Disconnected");
      }
      try {
        Thread.sleep(1000);
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
