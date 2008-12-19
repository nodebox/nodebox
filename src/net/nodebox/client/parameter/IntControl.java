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
import java.text.NumberFormat;

public class IntControl extends JComponent implements ChangeListener, ActionListener, ParameterControl, ParameterDataListener {

    private Parameter parameter;
    private DraggableNumber draggable;

    public IntControl(Parameter parameter) {
        setPreferredSize(new Dimension(100, 20));
        setLayout(new FlowLayout(FlowLayout.LEADING));
        this.parameter = parameter;
        draggable = new DraggableNumber();
        draggable.addChangeListener(this);
        draggable.addActionListener(this);
        draggable.setPreferredSize(new Dimension(100, 20));
        draggable.setMinimumSize(new Dimension(100, 20));
        NumberFormat intFormat = NumberFormat.getNumberInstance();
        intFormat.setMinimumFractionDigits(0);
        intFormat.setMaximumFractionDigits(0);
        draggable.setNumberFormat(intFormat);
        add(draggable);
        setValueForControl(parameter.getValue());
        parameter.addDataListener(this);
    }

    public Parameter getParameter() {
        return parameter;
    }

    public void setValueForControl(Object v) {
        int value = (Integer) v;
        draggable.setValue(value);
    }

    public void stateChanged(ChangeEvent e) {
        setValueFromControl();
    }

    public void actionPerformed(ActionEvent e) {
        setValueFromControl();
    }

    private void setValueFromControl() {
        double doubleValue = draggable.getValue();
        if (parameter.getParameterType().getMinimumValue() != null) {
            doubleValue = Math.max(parameter.getParameterType().getMinimumValue(), doubleValue);
        }
        if (parameter.getParameterType().getMaximumValue() != null) {
            doubleValue = Math.max(parameter.getParameterType().getMaximumValue(), doubleValue);
        }
        int intValue = (int) doubleValue;
        if (intValue != parameter.asInt()) {
            parameter.setValue(intValue);
        }
    }

    public void valueChanged(Parameter source, Object newValue) {
        setValueForControl(newValue);
    }
}
