package nodebox.client;

import nodebox.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class DeviceRow extends JComponent {
    private static final ImageIcon minusIcon = new ImageIcon(CodeLibrariesDialog.class.getResource("/action-minus.png"));

    private JComponent control;

    public DeviceRow(JComponent control, ActionListener removeDeviceListener) {
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        this.control = control;
        JButton removeDeviceButton = new JButton(minusIcon);
        removeDeviceButton.addActionListener(removeDeviceListener);
        removeDeviceButton.setBorder(null);
        add(this.control);
        add(Box.createHorizontalGlue());
        add(removeDeviceButton);
        add(Box.createHorizontalStrut(26));
        setBorder(new Theme.BottomBorder(new Color(136, 136, 136)));
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(400, control.getPreferredSize().height + 20);
    }

}
