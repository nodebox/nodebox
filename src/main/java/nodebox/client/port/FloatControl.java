package nodebox.client.port;

import nodebox.node.Port;
import nodebox.ui.DraggableNumber;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class FloatControl extends AbstractPortControl implements ChangeListener, ActionListener {

    private DraggableNumber draggable;

    public FloatControl(String nodePath, Port port) {
        super(nodePath, port);
        setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        draggable = new DraggableNumber();
        draggable.addChangeListener(this);
        draggable.setMinimumValue(null);
        draggable.setMinimumValue(port.getMinimumValue());
        draggable.setMaximumValue(port.getMaximumValue());
        setPreferredSize(draggable.getPreferredSize());
        add(draggable);
        setValueForControl(port.getValue());
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        draggable.setEnabled(enabled);
    }

    public void setValueForControl(Object v) {
        if (v instanceof Float) {
            draggable.setValue((Float) v);
        } else if (v instanceof Double) {
            draggable.setValue(((Double) v).floatValue());
        } else if (v instanceof Integer) {
            draggable.setValue(((Integer) v).floatValue());
        } else {
            throw new IllegalArgumentException("Value " + v + " is not a number.");
        }
    }

    public void stateChanged(ChangeEvent e) {
        setValueFromControl();
    }

    public void actionPerformed(ActionEvent e) {
        setValueFromControl();
    }

    private void setValueFromControl() {
        double value = draggable.getValue();
        setPortValue(value);
    }

}
