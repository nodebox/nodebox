package nodebox.client;

import javax.swing.*;
import java.awt.*;

public class DeviceRow extends JComponent {
    private JComponent control;

    public DeviceRow(JComponent control) {
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        this.control = control;
        add(this.control);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(400, control.getPreferredSize().height);
    }

}
