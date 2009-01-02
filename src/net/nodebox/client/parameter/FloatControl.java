package net.nodebox.client.parameter;

import net.nodebox.client.DraggableNumber;
import net.nodebox.node.Parameter;
import net.nodebox.node.ParameterDataListener;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class FloatControl extends JComponent implements ChangeListener, ActionListener, ParameterControl, ParameterDataListener {

    private Parameter parameter;
    private DraggableNumber draggable;

    public FloatControl(Parameter parameter) {
        setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        this.parameter = parameter;
        draggable = new DraggableNumber();
        draggable.addChangeListener(this);
        draggable.addActionListener(this);
        setPreferredSize(draggable.getPreferredSize());
        add(draggable);
        setValueForControl(parameter.getValue());
        parameter.addDataListener(this);
    }

    public Parameter getParameter() {
        return parameter;
    }

    public void setValueForControl(Object v) {
        Double value = (Double) v;
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
        if (parameter.getParameterType().getMinimumValue() != null) {
            value = Math.max(parameter.getParameterType().getMinimumValue(), value);
        }
        if (parameter.getParameterType().getMaximumValue() != null) {
            value = Math.max(parameter.getParameterType().getMaximumValue(), value);
        }
        if (value != (Double) parameter.getValue()) {
            parameter.setValue(value);
        }
    }

    public void valueChanged(Parameter source, Object newValue) {
        setValueForControl(newValue);
    }
}
