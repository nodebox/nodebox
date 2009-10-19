package nodebox.client;

import nodebox.node.Parameter;
import nodebox.node.Port;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class PortRow extends JComponent {

    private static Border rowBorder = new RowBorder();

    private Port port;
    private JLabel label;
    private JComponent control;

    private static final int TOP_PADDING = 2;
    private static final int BOTTOM_PADDING = 2;

    public PortRow(Port port, JComponent control) {
        this.port = port;
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        label = new ShadowLabel(port.getName());
        label.setBorder(null);
        label.setPreferredSize(new Dimension(ParameterView.LABEL_WIDTH, 16));

        this.control = control;
        control.setBorder(BorderFactory.createEmptyBorder(TOP_PADDING, 0, BOTTOM_PADDING, 0));

        add(this.label);
        add(Box.createHorizontalStrut(10));
        add(this.control);
        add(Box.createHorizontalGlue());
        setBorder(rowBorder);
    }

}
