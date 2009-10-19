package nodebox.client;

import javax.swing.border.Border;
import java.awt.*;

class RowBorder implements Border {
    private static Color labelUp = new Color(140, 140, 140);
    private static Color labelDown = new Color(166, 166, 166);
    private static Color parameterUp = new Color(179, 179, 179);
    private static Color parameterDown = new Color(213, 213, 213);

    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        int labelWidth = ParameterView.LABEL_WIDTH;
        // Draw border on the side of the label
        g.setColor(labelUp);
        g.fillRect(x, y + height - 2, labelWidth - 2, 1);
        g.setColor(labelDown);
        g.fillRect(x, y + height - 1, labelWidth - 2, 1);
        // Draw border on parameter side
        g.setColor(parameterUp);
        g.fillRect(x + labelWidth + 1, y + height - 2, width - labelWidth - 1, 1);
        g.setColor(parameterDown);
        g.fillRect(x + labelWidth + 1, y + height - 1, width - labelWidth - 1, 1);
    }

    public Insets getBorderInsets(Component c) {
        return new Insets(4, 0, 4, 0);
    }

    public boolean isBorderOpaque() {
        return true;
    }
}
