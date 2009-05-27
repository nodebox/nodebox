package net.nodebox.client;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class Divider extends JComponent {

    public static Image paneHeaderDivider;

    static {
        try {
            paneHeaderDivider = ImageIO.read(new File("res/pane-header-divider.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Divider() {
        Dimension d = new Dimension(2, 19);
        setPreferredSize(d);
        setMinimumSize(d);
        setMaximumSize(d);
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.drawImage(paneHeaderDivider, 0, 0, null);

    }
}
