package nodebox.client.parameter;

import nodebox.client.DraggableNumber;
import nodebox.node.Parameter;
import nodebox.node.ParameterValueListener;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;

public class FloatControl extends AbstractParameterControl implements ChangeListener, ActionListener {

    private DraggableNumber draggable;

    public FloatControl(Parameter parameter) {
        super(parameter);
        setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        draggable = new DraggableNumber();
        draggable.addChangeListener(this);
        setPreferredSize(draggable.getPreferredSize());
        // Set bounding
        if (parameter.getBoundingMethod() == Parameter.BoundingMethod.HARD) {
            Float minimumValue = parameter.getMinimumValue();
            if (minimumValue != null)
                draggable.setMinimumValue(minimumValue);
            Float maximumValue = parameter.getMaximumValue();
            if (maximumValue != null)
                draggable.setMaximumValue(maximumValue);
        }
        add(draggable);
        setValueForControl(parameter.getValue());
    }
    public void setValueForControl(Object v) {
        Float value = (Float) v;
        draggable.setValue(value);
    }

    public void stateChanged(ChangeEvent e) {
        setValueFromControl();
    }

    public void actionPerformed(ActionEvent e) {
        setValueFromControl();
    }

    private void setValueFromControl() {
        double value = draggable.getValue();
        if (parameter.getMinimumValue() != null) {
            value = Math.max(parameter.getMinimumValue(), value);
        }
        if (parameter.getMaximumValue() != null) {
            value = Math.max(parameter.getMaximumValue(), value);
        }
        if (value != (Float) parameter.getValue()) {
            parameter.setValue((float) value);
        }
    }

}
