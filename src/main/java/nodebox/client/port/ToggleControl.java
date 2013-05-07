package nodebox.client.port;

import nodebox.node.Port;
import nodebox.ui.NButton;

import java.awt.*;

public class ToggleControl extends AbstractPortControl {

    private NButton checkBox;

    public ToggleControl(String nodePath, Port port) {
        super(nodePath, port);
        setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        checkBox = new NButton(NButton.Mode.CHECK, port.getDisplayLabel());
        checkBox.setActionMethod(this, "toggle");
        add(checkBox);
        setValueForControl(port.getValue());
        setPreferredSize(new Dimension(120, 30));
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        checkBox.setEnabled(enabled);
    }

    public void setValueForControl(Object v) {
        if (v == null) return;
        boolean value = (Boolean) v;
        checkBox.setChecked(value);
    }

    public void toggle() {
        setPortValue(checkBox.isChecked());
    }

}
