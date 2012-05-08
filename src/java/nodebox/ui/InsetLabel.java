package nodebox.ui;

import javax.swing.*;
import java.awt.*;

public class InsetLabel extends JLabel {


    public InsetLabel(String s) {
        super(s);
        setFont(Theme.MESSAGE_FONT);
        setForeground(Theme.TEXT_NORMAL_COLOR);
    }

    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setFont(getFont());
        g2.setColor(getForeground());
        SwingUtils.drawShadowText(g2, getText(), 0, 14);
    }

}
