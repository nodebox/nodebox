package nodebox.client.parameter;

import nodebox.client.PlatformUtils;
import nodebox.node.Parameter;
import nodebox.node.ParameterValueListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class StringControl extends JComponent implements ParameterControl, ActionListener, ParameterValueListener {

    private Parameter parameter;
    private JTextField textField;

    public StringControl(Parameter parameter) {
        this.parameter = parameter;
        setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        textField = new JTextField();
        textField.putClientProperty("Jcomponent.sizeVariant", "small");
        textField.setFont(PlatformUtils.getSmallFont());
        textField.setPreferredSize(new Dimension(150, 19));
        textField.addActionListener(this);
        add(textField);
        setPreferredSize(new Dimension(150, 30));
        setValueForControl(parameter.getValue());
        parameter.getNode().addParameterValueListener(this);
    }

    public Parameter getParameter() {
        return parameter;
    }

    public void setValueForControl(Object v) {
        if (v == null) return;
        textField.setText(v.toString());
    }

    public void actionPerformed(ActionEvent e) {
        String newValue = textField.getText();
        if (!newValue.equals(parameter.asString())) {
            parameter.set(newValue);
        }
    }

    public void valueChanged(Parameter source) {
        setValueForControl(source.getValue());
    }
}
