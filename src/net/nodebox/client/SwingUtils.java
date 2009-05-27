package net.nodebox.client;

import java.awt.*;

public class SwingUtils {

    public static Color normalColor = new Color(60, 60, 60);
    public static Color armedColor = new Color(0, 0, 0);
    public static Color shadowColor = new Color(255, 255, 255);
    public static Font boldFont = new Font("Lucida Grande", Font.BOLD, 11);

    public static void centerOnScreen(Window w) {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension screenSize = toolkit.getScreenSize();
        int x = (screenSize.width - w.getWidth()) / 2;
        int y = (screenSize.height - w.getHeight()) / 2;
        w.setLocation(x, y);
    }

    public static void drawShadowText(Graphics2D g2, String s, int x, int y) {
        Color c = g2.getColor();
        g2.setColor(shadowColor);
        g2.drawString(s, x, y + 1);
        g2.setColor(c);
        g2.drawString(s, x, y);
    }

}
