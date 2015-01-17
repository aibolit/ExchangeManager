/*
 * Picture.java
 *
 * Created on February 18, 2007, 3:45 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package GUI;

import java.awt.Graphics;

/**
 *
 * @author Sasa Tamarkin
 */
public class Picture extends javax.swing.JPanel {

    private java.awt.image.BufferedImage image;

    /**
     * Creates a new instance of Picture
     */
    public Picture() {
    }

    public void setImage(java.awt.image.BufferedImage image) {
        this.image = image;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        try {
            g.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);
        } catch (NullPointerException e) {
        } catch (java.lang.NoClassDefFoundError e) {
            System.out.println("null->no biggie");
        }
    }

    private java.awt.image.BufferedImage getImage(int width, int height) {
        return java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().createCompatibleImage(width, height);
    }

    public void setWidth(int w) {
        setSize(w, getHeight());
    }

    public void setHeight(int h) {
        setSize(getWidth(), h);
    }
}
