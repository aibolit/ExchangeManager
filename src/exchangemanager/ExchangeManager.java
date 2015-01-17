/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package exchangemanager;

import ExchangeObjects.Exchange;
import ExchangeObjects.ExchangeException;
import Server.ExchangeServer;
import GUI.ExchangeStatus;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 *
 * @author Sasa
 */
public class ExchangeManager {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            try {
                Configurations.readCongfigs("settings.cfg");
            } catch (IOException ex) {
                System.out.println("Warning: Could not read configuration file; reverting to defaults");
            }
            
            final Exchange exchange = new Exchange();
            final ExchangeServer server = new ExchangeServer(exchange);
            final ExchangeStatus status = new ExchangeStatus(server);
            new Thread(exchange, "Exchange").start();
            new Thread(server, "Server").start();
            try {
                Thread.sleep(1000);
                java.awt.EventQueue.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        status.setVisible(true);
                    }
                });
            } catch (InterruptedException | InvocationTargetException ex) {
                ex.printStackTrace();
                System.exit(1);
            }
            status.run();
            
            System.out.println(exchange);
        } catch (ExchangeException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }
    
}
