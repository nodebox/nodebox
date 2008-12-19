package net.nodebox.client.parameter;

import net.nodebox.client.PlatformUtils;
import net.nodebox.node.Parameter;
import net.nodebox.node.ParameterDataListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TextControl extends JComponent implements ParameterControl, ActionListener, ParameterDataListener {

    private Parameter parameter;
    private JTextField textField;
    private JButton externalWindowButton;

    public TextControl(Parameter parameter) {
        this.parameter = parameter;
        setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        textField = new JTextField();
        textField.putClientProperty("Jcomponent.sizeVariant", "small");
        textField.setPreferredSize(new Dimension(150, 19));
        textField.setEditable(true);
        textField.addActionListener(this);
        textField.setFont(PlatformUtils.getSmallFont());
        externalWindowButton = new JButton("...");
        externalWindowButton.putClientProperty("JButton.buttonType", "gradient");
        externalWindowButton.setPreferredSize(new Dimension(30, 27));
        externalWindowButton.addActionListener(this);
        add(textField);
        add(externalWindowButton);
        setValueForControl(parameter.getValue());
        parameter.addDataListener(this);
    }

    public Parameter getParameter() {
        return parameter;
    }

    public void setValueForControl(Object v) {
        textField.setText(v.toString());
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == textField) {
            parameter.setValue(textField.getText());
        } else if (e.getSource() == externalWindowButton) {
            // TODO: Implement
        }
    }

    public void valueChanged(Parameter source, Object newValue) {
        setValueForControl(newValue);
    }

}
