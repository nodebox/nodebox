package nodebox.ui;

import javax.swing.*;
import java.awt.*;

public class MessageBar extends JLabel {

    public enum Type {INFO, WARNING}

    public MessageBar(String text) {
        this(text, Type.WARNING);
    }

    public MessageBar(String text, Type type) {
        super(text);
        setAlignmentX(JComponent.LEFT_ALIGNMENT);
        if (type == Type.WARNING) {
            setBorder(Borders.topBottom(1, new Color(235, 164, 69), 1, new Color(187, 125, 37)));
        } else {
            setBorder(Borders.topBottom(1, new Color(194, 204, 193), 1, new Color(136, 136, 136)));
        }
        setOpaque(true);
        setFont(Theme.SMALL_FONT);
        setPreferredSize(new Dimension(9999, 25));
        setMinimumSize(new Dimension(100, 25));
        setMaximumSize(new Dimension(9999, 25));
        if (type == Type.WARNING) {
            setBackground(new Color(226, 136, 10));
            setForeground(Color.WHITE);

        } else {
            setBackground(new Color(201, 201, 201));
            setForeground(new Color(52, 52, 52));
        }
    }


}
