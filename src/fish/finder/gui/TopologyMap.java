package fish.finder.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import fish.finder.Client;
import fish.finder.Connection;
import fish.finder.Route;
import fish.finder.proto.Message.ConnectionData;

public class TopologyMap extends JFrame implements Runnable {

  private int basicScale = 70;
  
  private JPanel contentPane;
  private Route map;
  private Client client;
  private  BufferedImage bi;

  /**
   * Create the frame.
   */
  public TopologyMap(Client client) {
    this.client = client;
    this.map = client.getRoute();
    setBounds(100, 100, 450, 300);
    contentPane = new JPanel();
    contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
    contentPane.setLayout(new BorderLayout(0, 0));
    setContentPane(contentPane);
    bi = new BufferedImage(this.getWidth(), this.getHeight(),
        BufferedImage.TYPE_4BYTE_ABGR);
    addComponentListener(new ComponentListener() {

      @Override
      public void componentHidden(ComponentEvent arg0) {
      }

      @Override
      public void componentMoved(ComponentEvent arg0) {
      }

      @Override
      public void componentResized(ComponentEvent arg0) {
        bi = new BufferedImage(TopologyMap.this.getWidth(), 
            TopologyMap.this.getHeight(),
            BufferedImage.TYPE_4BYTE_ABGR);
        reDraw();
      }

      @Override
      public void componentShown(ComponentEvent arg0) {
      }

    });
  }

  @Override
  public void setVisible(boolean b) {
    super.setVisible(b);
    new Thread(this).start();
  }

  private void drawNode(Graphics2D g, int x, int y, int width, int height,
                        Color c, String id, String ip) {
    g.setColor(c);
    g.fillOval(x, y, width, height);
    g.setColor(Color.WHITE);
    g.setFont(new Font(Font.SANS_SERIF, 0, 10));
    int sx = x + width / 2 - g.getFontMetrics().stringWidth(id) / 2;
    int sy = y + height / 2;
    g.drawString(id, sx, sy);
    if (ip != null) {
      g.setFont(new Font(Font.SANS_SERIF, 0, 16));
      sx =  x + width / 2 - g.getFontMetrics().stringWidth(ip) / 2;
      g.drawString(ip, sx, sy + 16);
    }
  }
  
  private void drawArrow(Graphics2D g, int fx, int fy, int tx, int ty) {
    g.setColor(Color.WHITE);
    g.drawLine(fx + basicScale, fy + basicScale / 2, tx + basicScale, ty + basicScale/2);
  }

  private void drawPeersRecursive(Graphics2D g, Long peer, int x, int y) {
    Collection<Long> peers = client.getRoute().getViaPeers(peer);
    if (peers.size() == 0) return;
    double peerAngle = Math.PI*2/peers.size();
    int peerCount = 0;
    for (Long id : peers) {
      double angle = Math.PI/2 + peerAngle*peerCount;
      int py = y + (int)(basicScale*2*Math.sin(angle));
      int px = x + (int)(basicScale*4*Math.cos(angle));
      drawArrow(g, x, y, px, py);
      String cdata = client.getRoute().getConnectionData(id);
      drawNode(g, px, py, 2*basicScale, basicScale,
          new Color(125, 0, 0), ""+id, cdata);
      ++peerCount;
      drawPeersRecursive(g, id, px, py);
    }
  }

  private synchronized void reDraw() {
    Graphics2D g = (Graphics2D)bi.getGraphics();
    g.setColor(Color.BLACK);
    g.fillRect(0,  0, bi.getWidth(), bi.getHeight());
    int x = getWidth() / 2 - basicScale/2;
    int y = getHeight() / 2 - basicScale/2;
    drawNode(g, x, y, 2*basicScale, basicScale,
             new Color(0, 125, 0), ""+client.getLocalIdentity(),
             client.getListenningIP());
    Collection<Connection> directPeers = map.getDirectConnections();
    double peerAngle = Math.PI*2/directPeers.size();
    int peerCount = 0;
    for (Connection c : directPeers) {
      double angle = peerAngle*peerCount;
      int py = y + (int)(basicScale*2*Math.sin(angle));
      int px = x + (int)(basicScale*4*Math.cos(angle));
      drawArrow(g, x, y, px, py);
      drawNode(g, px, py, 2*basicScale, basicScale,
          new Color(0, 0, 125), ""+c.getRemoteIdentity(),
          c.getRemoteIP() + ":" + c.getRemotePort());
      ++peerCount;
      drawPeersRecursive(g, c.getRemoteIdentity(), px, py); 
    }
    getGraphics().drawImage(bi, 0, 0, null);
  }
  
  @Override
  public void run() {
    while (isVisible()) {
      reDraw();
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

}
