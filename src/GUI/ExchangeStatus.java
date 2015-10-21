/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI;

import ExchangeObjects.Exchange;
import ExchangeObjects.ExchangeException;
import ExchangeObjects.Order;
import ExchangeObjects.OrderType;
import ExchangeObjects.Security;
import Server.ExchangeServer;
import exchangemanager.Configurations;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 *
 * @author Sasa
 */
public class ExchangeStatus extends javax.swing.JFrame {

    private static final long serialVersionUID = 1L;
    private final ExchangeServer exchangeServer;
    private final Map<Security, Double[]> securityHistory = new ConcurrentSkipListMap<>(new Comparator<Security>() {
        @Override
        public int compare(Security t0, Security t1) {
            return t0.getTicker().compareTo(t1.getTicker());
        }
    });
    private final int HISTORY_LENGTH = 60;

    /**
     * Creates new form ExchangeStatus
     *
     * @param exchangeServer
     */
    public ExchangeStatus(ExchangeServer exchangeServer) {
        this.exchangeServer = exchangeServer;
        for (Security security : Configurations.getSecurities()) {
            securityHistory.put(security, new Double[HISTORY_LENGTH]);
        }
        initComponents();
    }

    public void run() {
        new Thread() {
            @Override
            public void run() {
                int tick = 0;
                while (true) {
                    try {
                        final int tickVal = tick;
                        java.awt.EventQueue.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                refreshData(tickVal);
                            }
                        });
                    } catch (InterruptedException | InvocationTargetException ex) {
                        ex.printStackTrace();
                    }
                    if (exchangeServer.getExchange().isRunning()) {
                        if (exchangeServer.getExchange().isRunning()) {
                            tick++;
                        }
                    }

                    try {
                        Thread.sleep(1000 - ((System.currentTimeMillis() + 255) % 1000));
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }

        }.start();
    }

    private void refreshData(int tick) {
        class UserScore implements Comparable<UserScore> {

            private final String user;
            private final double score;

            UserScore(String user, double score) {
                this.user = user;
                this.score = score;
            }

            public String getUser() {
                return user;
            }

            public double getScore() {
                return score;
            }

            @Override
            public int compareTo(UserScore t) {
                double rv = t.getScore() - this.getScore();
                return rv == 0 ? this.getUser().compareTo(t.getUser()) : rv > 0 ? 1 : -1;
            }

            @Override
            public String toString() {
                return user + " " + score;
            }
        }

        @SuppressWarnings("unchecked")
        List<UserScore> userScores = new ArrayList<>();
        Map<Security, Map<OrderType, Double>> bestOrders = new ConcurrentHashMap<>();
        synchronized (exchangeServer.getExchange()) {
            for (String user : Configurations.getUsers()) {
                if (user.equals(Configurations.EXCHANGE_USER)) {
                    continue;
                }
                userScores.add(new UserScore(user, exchangeServer.getExchange().getUserValue(user)));
            }
            Collections.sort(userScores);
            for (Map.Entry<Security, Double[]> entry : securityHistory.entrySet()) {
                Security security = entry.getKey();
                entry.getValue()[tick % entry.getValue().length] = entry.getKey().getNetWorth();
                bestOrders.put(entry.getKey(), new EnumMap<OrderType, Double>(OrderType.class));
                try {
                    for (Map.Entry<OrderType, SortedSet<Order>> orderEntry : exchangeServer.getExchange().getOrderStatus(security.getTicker()).entrySet()) {
                        if (!orderEntry.getValue().isEmpty()) {
                            bestOrders.get(security).put(orderEntry.getKey(), orderEntry.getValue().first().getPrice());
                        }
                    }
                } catch (ExchangeException ex) {
                    ex.printStackTrace();
                }
            }
        }
        BufferedImage bi = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_ARGB);

        Graphics2D cg = (Graphics2D) bi.getGraphics();
        cg.setBackground(Color.BLACK);
        cg.setColor(Color.BLACK);
        cg.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        
        int userDnum = userScores.size() / 2 + userScores.size() % 2;
        int lSize = Math.max(userDnum, Configurations.getSecurities().size());
        cg.transform(AffineTransform.getScaleInstance(1.0 * canvas.getWidth() / 764, 1.0 * canvas.getHeight() / (450 + 18 * lSize)));
        //cg.transform(AffineTransform.getScaleInstance(2,2.0));

        AffineTransform root = cg.getTransform();

        DecimalFormat timeFormat = new DecimalFormat();
        timeFormat.setMaximumIntegerDigits(2);
        timeFormat.setMinimumIntegerDigits(2);
        timeFormat.setMaximumFractionDigits(0);
        cg.setFont(new Font("Monospaced", Font.PLAIN, 24));
        cg.setColor(Color.WHITE);
        long ticksRemaining = exchangeServer.getExchange().getTicksRemaining();
        cg.drawString(timeFormat.format(ticksRemaining / 3600) + ":" + timeFormat.format((ticksRemaining / 60) % 60) + ":" + timeFormat.format(ticksRemaining % 60), 4, 28);

        cg.transform(AffineTransform.getTranslateInstance(180, 50));
        cg.transform(AffineTransform.getScaleInstance(1.5, 1.5));

        cg.setFont(new Font("Ebrima", Font.BOLD, 50));
        cg.setColor(Color.DARK_GRAY);
        cg.drawString("Bloomberg", 34, 48);
        cg.setColor(Color.WHITE);
        cg.drawString("Bloomberg", 30, 44);
        cg.transform(AffineTransform.getTranslateInstance(0, 60));

        AffineTransform local = cg.getTransform();
        cg.setColor(Color.getHSBColor((float) ((hsbColor++ % 360) / 360.0), 1, 1));
        cg.setFont(new Font("Segoe Script", Font.PLAIN, 32));
        cg.transform(AffineTransform.getRotateInstance(Math.PI / -4));
        cg.drawString("Hack", 10, 10);
        cg.setTransform(local);

        cg.setFont(new Font("Segoe Script", Font.PLAIN, 40));
        cg.drawString("@", 75, -50);

        cg.setTransform(root);
        cg.transform(AffineTransform.getTranslateInstance(0, 150));
        AffineTransform gridUsersTx = cg.getTransform();
        cg.transform(AffineTransform.getScaleInstance(.5, 1));

        cg.setColor(Color.DARK_GRAY);
        cg.fillRect(0, 0, 300, 22);

        cg.setColor(Color.WHITE);
        cg.setFont(new Font("Arial", Font.BOLD, 18));
        cg.drawString("Team Name", 8, 18);
        cg.drawString("Net Worth", 205, 18);

        cg.transform(AffineTransform.getTranslateInstance(0, 40));
        cg.setColor(AMBER);
        cg.setFont(new Font("Arial", Font.PLAIN, 16));
        DecimalFormat scoreFormat = new DecimalFormat();
        scoreFormat.setRoundingMode(RoundingMode.HALF_UP);
        scoreFormat.setMinimumFractionDigits(2);
        scoreFormat.setMaximumFractionDigits(2);
        for (int i = lSize - 1; i >= 0; i--) {
            if ((i % 2) == 1) {
                cg.setColor(VERY_DARK_GRAY);
                cg.fillRect(0, 18 * (i - 1), 300, 18);
            }
            if (i < userScores.size()) {
                cg.setFont(new Font("Arial", Font.PLAIN, 18));
                cg.setColor(AMBER);
                cg.drawString(userScores.get(i).user, 8, 18 * i);
                cg.setFont(new Font("Monospaced", Font.PLAIN, 18));
                cg.setColor(Color.WHITE);
                cg.drawString(String.format("%12s", scoreFormat.format(userScores.get(i).score)), 160, 18 * i);
            }
        }
        cg.setColor(Color.DARK_GRAY);
        cg.drawRect(0, -40, 300, 26 + 18 * lSize);

        cg.setTransform(gridUsersTx);
        cg.transform(AffineTransform.getTranslateInstance(160, 0));
        cg.transform(AffineTransform.getScaleInstance(.5, 1));

        cg.setColor(Color.DARK_GRAY);
        cg.fillRect(0, 0, 300, 22);

        cg.setColor(Color.WHITE);
        cg.setFont(new Font("Arial", Font.BOLD, 18));
        cg.drawString("Team Name", 8, 18);
        cg.drawString("Net Worth", 205, 18);

        cg.transform(AffineTransform.getTranslateInstance(0, 40));
        cg.setColor(AMBER);
        cg.setFont(new Font("Arial", Font.PLAIN, 16));
        for (int i = lSize - 1; i >= 0; i--) {
            int j = lSize  + i;
            if ((i % 2) == 1) {
                cg.setColor(VERY_DARK_GRAY);
                cg.fillRect(0, 18 * (i - 1), 300, 18);
            }
            if (j < userScores.size()) {
                cg.setFont(new Font("Arial", Font.PLAIN, 18));
                cg.setColor(AMBER);
                cg.drawString(userScores.get(j).user, 8, 18 * i);
                cg.setFont(new Font("Monospaced", Font.PLAIN, 18));
                cg.setColor(Color.WHITE);
                cg.drawString(String.format("%12s", scoreFormat.format(userScores.get(j).score)), 160, 18 * i);
            }
        }
        cg.setColor(Color.DARK_GRAY);
        cg.drawRect(0, -40, 300, 26 + 18 * lSize);

        cg.setTransform(gridUsersTx);
        cg.transform(AffineTransform.getTranslateInstance(320, 0));
        cg.setColor(Color.DARK_GRAY);
        cg.fillRect(0, 0, 440, 22);

        cg.setColor(Color.WHITE);
        cg.setFont(new Font("Arial", Font.BOLD, 18));
        cg.drawString("Stocks", 8, 18);
        cg.drawString("Net Worth", 120, 18);
        cg.drawString("Bid", 285, 18);
        cg.drawString("Ask", 390, 18);

        cg.transform(AffineTransform.getTranslateInstance(0, 40));
        cg.setColor(AMBER);
        List<Security> secs = new ArrayList<>(Configurations.getSecurities());
        DecimalFormat netWorthFormat = new DecimalFormat();
        netWorthFormat.setRoundingMode(RoundingMode.HALF_UP);
        netWorthFormat.setMinimumFractionDigits(0);
        netWorthFormat.setMaximumFractionDigits(0);
        DecimalFormat bidAskFormat = new DecimalFormat();
        bidAskFormat.setRoundingMode(RoundingMode.HALF_UP);
        bidAskFormat.setMinimumFractionDigits(3);
        bidAskFormat.setMaximumFractionDigits(3);
        for (int i = lSize - 1; i >= 0; i--) {
            if ((i % 2) == 1) {
                cg.setColor(VERY_DARK_GRAY);
                cg.fillRect(0, 18 * (i - 1), 440, 18);
            }
            if (i < secs.size()) {
                Security security = secs.get(i);
                cg.setFont(new Font("Arial", Font.PLAIN, 18));
                cg.setColor(AMBER);
                cg.drawString(secs.get(i).getTicker(), 8, 18 * i);

                double currentValue = securityHistory.get(security)[tick % HISTORY_LENGTH];
                Double lastValue = securityHistory.get(security)[(tick + HISTORY_LENGTH - 1) % HISTORY_LENGTH];
                if (lastValue == null) {
                    lastValue = currentValue;
                }

                cg.setColor(currentValue == lastValue ? Color.WHITE : currentValue > lastValue ? Color.GREEN : Color.RED);
                cg.setFont(new Font("Monospaced", Font.PLAIN, 18));
                cg.drawString(String.format("%10s", netWorthFormat.format(currentValue)), 100, 18 * i);

                cg.setColor(Color.WHITE);
                if (bestOrders.containsKey(security) && bestOrders.get(security).containsKey(OrderType.BID)) {
                    cg.drawString(String.format("%9s", bidAskFormat.format(bestOrders.get(security).get(OrderType.BID))), 220, 18 * i);
                } else {
                    cg.drawString("     N.A.", 220, 18 * i);
                }
                if (bestOrders.containsKey(security) && bestOrders.get(security).containsKey(OrderType.ASK)) {
                    cg.drawString(String.format("%9s", bidAskFormat.format(bestOrders.get(security).get(OrderType.ASK))), 330, 18 * i);
                } else {
                    cg.drawString("     N.A.", 330, 18 * i);
                }
            }
        }

        cg.setColor(Color.DARK_GRAY);
        cg.drawRect(0, -40, 440, 26 + 18 * lSize);

        cg.setTransform(gridUsersTx);
        cg.transform(AffineTransform.getTranslateInstance(0, 18 * lSize + 40));
        AffineTransform graphTx = cg.getTransform();

        for (int i = 0; i < secs.size(); i++) {
            int x = i % 5;
            int y = i / 5;
            cg.transform(AffineTransform.getTranslateInstance(152 * x + 2, y * 127));
            Security security = secs.get(i);
            Double min = securityHistory.get(security)[0];
            if (min == null) {
                min = 0.0;
            }
            double max = min;
            for (int j = 0; j < HISTORY_LENGTH; j++) {
                Double val = securityHistory.get(security)[j];
                if (val != null) {
                    max = Math.max(max, val);
                    min = Math.min(min, val);
                }
            }

            cg.setFont(new Font("Arial", Font.PLAIN, 18));
            cg.setColor(AMBER);
            cg.drawString(security.getTicker(), 50, 18);

            AffineTransform ctx = cg.getTransform();

            cg.setColor(Color.WHITE);
//            cg.transform(AffineTransform.getTranslateInstance(150, -min ));
//            cg.fillOval(-2, -2 +  min.intValue(), 4, 4);
//            cg.transform(AffineTransform.getScaleInstance(600.0 / 160, 100.0 / (max - min) ));
            double cval = securityHistory.get(security)[tick % HISTORY_LENGTH];
            for (int idx = 1; idx < HISTORY_LENGTH; idx++) {
                Double nextVal = securityHistory.get(security)[(HISTORY_LENGTH + tick - idx) % HISTORY_LENGTH];
                if (nextVal == null) {
                    break;
                }
                cg.drawLine(150 - idx * 150 / HISTORY_LENGTH, 125 - (int) ((cval - min) / (max - min) * 100), 150 - (idx + 1) * 150 / HISTORY_LENGTH, 125 - (int) ((nextVal - min) / (max - min) * 100));
                cval = nextVal;
            }

            cg.setTransform(ctx);
            cg.setColor(Color.DARK_GRAY);
            cg.drawRect(0, 0, 150, 125);
            cg.setTransform(graphTx);
        }
        canvas.getGraphics().drawImage(bi, 0, 0, null);

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        disclaimerLabel = new javax.swing.JLabel();
        canvas = new GUI.Picture();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Exchange Server Leaderboard");

        disclaimerLabel.setFont(new java.awt.Font("Tahoma", 0, 8)); // NOI18N
        disclaimerLabel.setText("Exchange Server Version 1.0.0 -- Developed By Aleks Tamarkin");

        javax.swing.GroupLayout canvasLayout = new javax.swing.GroupLayout(canvas);
        canvas.setLayout(canvasLayout);
        canvasLayout.setHorizontalGroup(
            canvasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        canvasLayout.setVerticalGroup(
            canvasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 339, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(canvas, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addGap(0, 239, Short.MAX_VALUE)
                .addComponent(disclaimerLabel))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(canvas, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(disclaimerLabel))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(ExchangeStatus.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ExchangeStatus.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ExchangeStatus.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ExchangeStatus.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    new ExchangeStatus(new ExchangeServer(new Exchange())).setVisible(true);
                } catch (ExchangeException ex) {
                }
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private GUI.Picture canvas;
    private javax.swing.JLabel disclaimerLabel;
    // End of variables declaration//GEN-END:variables
    private static final Color AMBER = new Color(255, 126, 0);
    private static final Color VERY_DARK_GRAY = new Color(24, 24, 24);
    private int hsbColor = 0;
}
