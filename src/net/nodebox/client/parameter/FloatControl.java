package net.nodebox.client.parameter;

import net.nodebox.client.DraggableNumber;
import net.nodebox.node.Parameter;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class FloatControl extends JComponent implements ChangeListener, ActionListener, ParameterControl {

    private Parameter parameter;
    private DraggableNumber draggable;

    public FloatControl(Parameter parameter) {
        setPreferredSize(new Dimension(100, 20));
        setLayout(new FlowLayout(FlowLayout.LEADING));
        this.parameter = parameter;
        draggable = new DraggableNumber();
        draggable.addChangeListener(this);
        draggable.addActionListener(this);
        draggable.setPreferredSize(new Dimension(100, 20));
        add(draggable);
        setValueForControl(parameter.getValue());
    }

    public Parameter getParameter() {
        return parameter;
    }

    public double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
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
        if (parameter.getMinimumValue() != null) {
            value = Math.max(parameter.getMinimumValue(), value);
        }
        if (parameter.getMaximumValue() != null) {
            value = Math.max(parameter.getMaximumValue(), value);
        }
        if (value != (Double) parameter.getValue()) {
            parameter.setValue(value);
        }
    }

}
