package net.nodebox.client;

import java.awt.*;

public class SwingUtils {
    public static void centerOnScreen(Window w) {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension screenSize = toolkit.getScreenSize();
        int x = (screenSize.width - w.getWidth()) / 2;
        int y = (screenSize.height - w.getHeight()) / 2;
        w.setLocation(x, y);

    }
}
