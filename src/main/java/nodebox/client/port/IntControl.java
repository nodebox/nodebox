package nodebox.client.port;

import nodebox.node.Port;
import nodebox.ui.DraggableNumber;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.Locale;

public class IntControl extends AbstractPortControl implements ChangeListener, ActionListener {

    private DraggableNumber draggable;

    public IntControl(String nodePath, Port port) {
        super(nodePath, port);
        setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        draggable = new DraggableNumber();
        draggable.addChangeListener(this);
        NumberFormat intFormat = NumberFormat.getNumberInstance(Locale.US);
        intFormat.setMinimumFractionDigits(0);
        intFormat.setMaximumFractionDigits(0);
        draggable.setNumberFormat(intFormat);
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
        if (v instanceof Integer) {
            draggable.setValue((Integer) v);
        } else if (v instanceof Long) {
            draggable.setValue((Long) v);
        } else {
            throw new IllegalArgumentException("This function only accept integers or longs, not " + v);
        }
    }

    public void stateChanged(ChangeEvent e) {
        setValueFromControl();
    }

    public void actionPerformed(ActionEvent e) {
        setValueFromControl();
    }

    private void setValueFromControl() {
        double doubleValue = draggable.getValue();
        int intValue = (int) doubleValue;
        setPortValue(intValue);
    }

}
