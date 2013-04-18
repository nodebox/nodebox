package nodebox.client.port;

import nodebox.node.Port;

import javax.swing.*;

public abstract class AbstractPortControl extends JComponent implements PortControl {

    protected String nodePath;
    protected Port port;
    private String displayName;
    private OnValueChangeListener onValueChangeListener;

    protected AbstractPortControl(String nodePath, Port port) {
        this.nodePath = nodePath;
        this.port = port;
        displayName = port.getName();
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Port getPort() {
        return port;
    }

    public void setPortValue(Object value) {
        if (onValueChangeListener != null)
            onValueChangeListener.onValueChange(nodePath, port.getName(), value);
    }

    public void setValueChangeListener(OnValueChangeListener l) {
        onValueChangeListener = l;
    }

    public OnValueChangeListener getValueChangeListener() {
        return onValueChangeListener;
    }
}
