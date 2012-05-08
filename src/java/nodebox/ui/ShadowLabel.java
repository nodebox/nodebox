package nodebox.ui;

import javax.swing.*;
import java.awt.*;

public class ShadowLabel extends JLabel {
    public ShadowLabel(String text) {
        super(text);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        if (isEnabled()) {
            g2.setColor(Theme.TEXT_NORMAL_COLOR);
        } else {
            g2.setColor(Theme.TEXT_DISABLED_COLOR);
        }
        g2.setFont(Theme.SMALL_BOLD_FONT);
        int textX = Theme.LABEL_WIDTH - g2.getFontMetrics().stringWidth(getText()) - 10;
        // Add some padding to align it to 30px high components.
        int textY = (getHeight() - g2.getFont().getSize()) / 2 + 10;
        SwingUtils.drawShadowText(g2, getText(), textX, textY, Theme.DEFAULT_SHADOW_COLOR, 1);
    }
}
