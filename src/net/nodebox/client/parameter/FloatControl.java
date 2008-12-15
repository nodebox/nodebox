package net.nodebox.client.parameter;

import net.nodebox.node.Parameter;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;

public class FloatControl extends JComponent implements ChangeListener, VetoableChangeListener, ParameterControl {

    private Parameter parameter;
    private JTextField textField;
    private JSlider slider;

    public FloatControl(Parameter parameter) {
        setLayout(new FlowLayout(FlowLayout.LEADING));
        this.parameter = parameter;
        textField = new JTextField();
        textField.setPreferredSize(new Dimension(50, 19));
        textField.putClientProperty("JComponent.sizeVariant", "small");
        textField.addVetoableChangeListener(this);
        slider = new JSlider(0, 1000);
        slider.putClientProperty("JComponent.sizeVariant", "small");
        slider.addChangeListener(this);
        add(textField);
        add(slider);
        setValue(parameter.getValue());
    }

    public Parameter getParameter() {
        return parameter;
    }

    public double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public void setValue(Object v) {
        Double value = (Double) v;
        double minimumValue = parameter.getMinimumValue() == null ? 0 : parameter.getMinimumValue();
        double maximumValue = parameter.getMaximumValue() == null ? 10 : parameter.getMaximumValue();
        double range = maximumValue - minimumValue;
        value = clamp(value, minimumValue, maximumValue);
        int sliderValue = (int) (value / range * 1000);
        textField.setText(value.toString());
        slider.setValue(sliderValue);
    }

    public void stateChanged(ChangeEvent e) {
        System.out.println("stateChanged " + slider.getValue());
        // The slider ranges from 0-1000.
        // The first conversion moves from 0.0 - 1.0
        double v = slider.getValue() / 1000.0;
        double minimumValue = parameter.getMinimumValue() == null ? 0 : parameter.getMinimumValue();
        double maximumValue = parameter.getMaximumValue() == null ? 10 : parameter.getMaximumValue();
        double delta = maximumValue - minimumValue;
        // Now multiply the delta with the 0.0 - 1.0 range to get the abs delta and add it to the min.
        double finalValue = minimumValue + delta * v;
        if (finalValue != (Double) parameter.getValue()) {
            parameter.setValue(finalValue);
        }
    }

    public void vetoableChange(PropertyChangeEvent evt) throws PropertyVetoException {
        System.out.println("vetoableChange " + evt.getNewValue());
        String s = evt.getNewValue().toString();
        double v = Double.parseDouble(s);
        if (v != (Double) parameter.getValue()) {
            parameter.setValue(v);
        }
    }
}
