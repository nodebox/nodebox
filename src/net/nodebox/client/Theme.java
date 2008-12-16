package net.nodebox.client;

import javax.swing.*;
import java.awt.*;

public class Theme {

    private static Theme instance = new Theme();

    public static Theme getInstance() {
        return instance;
    }

    private Color backgroundColor = new Color(178, 178, 178);
    private Color foregroundColor = new Color(136, 136, 136);
    private Color textColor = new Color(80, 80, 80);
    private Color actionColor = new Color(0, 116, 168);
    private Color borderColor = new Color(48, 48, 48);
    private Color borderHighlightColor = new Color(120, 120, 120);
    private Color viewBackgroundColor = new Color(144, 152, 160);
    private ArrowIcon arrowIcon = new ArrowIcon();

    class ArrowIcon implements Icon {
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(Theme.getInstance().getForegroundColor());
            g.drawLine(x + 1, y, x + 1, y);
            g.drawLine(x + 1, y + 1, x + 2, y + 1);
            g.drawLine(x + 1, y + 2, x + 3, y + 2);
            g.drawLine(x + 1, y + 3, x + 4, y + 3);
            g.drawLine(x + 1, y + 4, x + 3, y + 4);
            g.drawLine(x + 1, y + 5, x + 2, y + 5);
            g.drawLine(x + 1, y + 6, x + 1, y + 6);
        }

        public int getIconWidth() {
            return 6;
        }

        public int getIconHeight() {
            return 8;
        }
    }


    private Theme() {
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public Color getForegroundColor() {
        return foregroundColor;
    }

    public Color getTextColor() {
        return textColor;
    }

    public Color getActionColor() {
        return actionColor;
    }

    public Color getBorderColor() {
        return borderColor;
    }

    public Color getBorderHighlightColor() {
        return borderHighlightColor;
    }

    public Color getViewBackgroundColor() {
        return viewBackgroundColor;
    }

    public ArrowIcon getArrowIcon() {
        return arrowIcon;
    }
}
