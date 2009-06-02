package net.nodebox.client.parameter;

import net.nodebox.client.DraggableNumber;
import net.nodebox.node.Parameter;
import net.nodebox.node.ParameterValueListener;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class FloatControl extends JComponent implements ChangeListener, ActionListener, ParameterControl, ParameterValueListener {

    private Parameter parameter;
    private DraggableNumber draggable;

    public FloatControl(Parameter parameter) {
        setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        this.parameter = parameter;
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
        parameter.getNode().addParameterValueListener(this);
    }

    public Parameter getParameter() {
        return parameter;
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

    public void valueChanged(Parameter source) {
        setValueForControl(source.getValue());
    }
}
