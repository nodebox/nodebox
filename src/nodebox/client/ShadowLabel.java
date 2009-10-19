package nodebox.client;

import javax.swing.*;
import java.awt.*;

class ShadowLabel extends JLabel {
    public ShadowLabel(String text) {
        super(text);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(SwingUtils.COLOR_NORMAL);
        g2.setFont(SwingUtils.FONT_BOLD);
        int textX = ParameterView.LABEL_WIDTH - g2.getFontMetrics().stringWidth(getText()) - 10;
        // Add some padding to align it to 30px high components.
        int textY = (getHeight() - g2.getFont().getSize()) / 2 + 10;
        SwingUtils.drawShadowText(g2, getText(), textX, textY, new Color(176, 176, 176), 1);
    }
}
