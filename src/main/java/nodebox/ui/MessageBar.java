package nodebox.ui;

import javax.swing.*;
import java.awt.*;

public class MessageBar extends JLabel {

    public MessageBar(String text) {
        super(text);

        setAlignmentX(JComponent.LEFT_ALIGNMENT);
        setBorder(Borders.topBottom(1, new Color(235, 164, 69), 1, new Color(187, 125, 37)));
        setOpaque(true);
        setFont(Theme.SMALL_FONT);
        setPreferredSize(new Dimension(9999, 25));
        setMinimumSize(new Dimension(100, 25));
        setMaximumSize(new Dimension(9999, 25));
        setBackground(new Color(226, 136, 10));
        setForeground(Color.WHITE);
    }

}
