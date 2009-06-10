package nodebox.client.parameter;

import nodebox.client.DraggableNumber;
import nodebox.node.Parameter;
import nodebox.node.ParameterValueListener;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;

public class IntControl extends JComponent implements ChangeListener, ActionListener, ParameterControl, ParameterValueListener {

    private Parameter parameter;
    private DraggableNumber draggable;

    public IntControl(Parameter parameter) {
        setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        this.parameter = parameter;
        draggable = new DraggableNumber();
        draggable.addChangeListener(this);
        NumberFormat intFormat = NumberFormat.getNumberInstance();
        intFormat.setMinimumFractionDigits(0);
        intFormat.setMaximumFractionDigits(0);
        draggable.setNumberFormat(intFormat);
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
        setPreferredSize(draggable.getPreferredSize());
        setValueForControl(parameter.getValue());
        parameter.getNode().addParameterValueListener(this);
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
        if (parameter.getMinimumValue() != null) {
            doubleValue = Math.max(parameter.getMinimumValue(), doubleValue);
        }
        if (parameter.getMaximumValue() != null) {
            doubleValue = Math.max(parameter.getMaximumValue(), doubleValue);
        }
        int intValue = (int) doubleValue;
        if (intValue != parameter.asInt()) {
            parameter.setValue(intValue);
        }
    }

    public void valueChanged(Parameter source) {
        if (parameter != source) return;
        setValueForControl(source.getValue());
    }
}
