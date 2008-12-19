package net.nodebox.client.parameter;

import net.nodebox.node.Parameter;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;

public class StringControl extends JComponent implements ParameterControl, VetoableChangeListener {

    private Parameter parameter;
    private JTextField textField;

    public StringControl(Parameter parameter) {
        this.parameter = parameter;
        setLayout(new FlowLayout(FlowLayout.LEADING));
        textField = new JTextField();
        textField.setPreferredSize(new Dimension(100, 20));
        textField.addVetoableChangeListener(this);
        add(textField);
    }

    public Parameter getParameter() {
        return parameter;
    }

    public void setValueForControl(Object v) {
        if (v == null) return;
        textField.setText(v.toString());
    }

    public void vetoableChange(PropertyChangeEvent evt) throws PropertyVetoException {
        String newValue = textField.getText();
        if (!newValue.equals(parameter.asString())) {
            parameter.set(newValue);
        }
    }
}
