package com.github.sunnybat.paxchecker.status;

import com.github.sunnybat.paxchecker.PAXChecker;
import com.github.sunnybat.paxchecker.resources.ResourceLoader;
import java.awt.Image;
import java.io.IOException;
import java.util.Calendar;

/**
 *
 * @author SunnyBat
 */
public class Tickets extends com.github.sunnybat.commoncode.javax.swing.JFrame {

  static {
    try {
      myIcon = javax.imageio.ImageIO.read(ResourceLoader.loadResource("Alert.png"));
    } catch (IOException iOE) {
      iOE.printStackTrace();
      System.out.println("Failed to load ImageIcon for Tickets window -- program will still function normally.");
    }
  }

  private static Image myIcon;

  public Tickets(final String link) {
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        initComponents();
        setIconImage(myIcon);
        String newline = System.getProperty("line.separator");
        jTextArea1.append(newline + newline + "Tickets found at " + currentTime());
        jTextArea1.append(newline + newline + "URL found: " + link);
        jTextArea1.append(newline + newline + "Version: " + PAXChecker.VERSION);
        setAlwaysOnTop(true);
        setVisible(true);
        toFront();
        requestFocus();
      }
    });
  }

  private static String currentTime() {
    StringBuilder ret = new StringBuilder();
    Calendar myCal = Calendar.getInstance();
    int hours = myCal.get(Calendar.HOUR);
    if (hours == 0) {
      hours = 12; // noon+midnight are 0, not 12, so fix it
    }
    ret.append(hours).append(":");
    int minutes = myCal.get(Calendar.MINUTE);
    ret.append(minutes).append(":");
    int seconds = myCal.get(Calendar.SECOND);
    ret.append(seconds).append(" ");
    int ampm = myCal.get(Calendar.AM_PM);
    if (ampm == Calendar.AM) {
      ret.append("AM");
    } else if (ampm == Calendar.PM) {
      ret.append("PM");
    } else {
      ret.append("[AM/PM?]");
    }
    return ret.toString();
  }

  /**
   * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    jScrollPane1 = new javax.swing.JScrollPane();
    jTextArea1 = new javax.swing.JTextArea();

    setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
    setTitle("PAX Update Found!");
    setResizable(false);

    jTextArea1.setEditable(false);
    jTextArea1.setColumns(20);
    jTextArea1.setFont(new java.awt.Font("Monospaced", 1, 18)); // NOI18N
    jTextArea1.setForeground(new java.awt.Color(255, 51, 0));
    jTextArea1.setLineWrap(true);
    jTextArea1.setRows(5);
    jTextArea1.setText("An update has been found!\n\nAn attempt to open the link found in your default browser has been made. If it did not open, you will have to do so manually.\n\nGood luck buying tickets!\n- /u/SunnyBat (Sunnybat@yahoo.com)\n\nIf this was a false positive, feel free to post on /r/PAXChecker or PM /u/SunnyBat individually.");
    jTextArea1.setWrapStyleWord(true);
    jScrollPane1.setViewportView(jTextArea1);

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 579, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 414, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );

    pack();
  }// </editor-fold>//GEN-END:initComponents
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JScrollPane jScrollPane1;
  private javax.swing.JTextArea jTextArea1;
  // End of variables declaration//GEN-END:variables
}
