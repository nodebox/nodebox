package nodebox.client.parameter;

import nodebox.client.PlatformUtils;
import nodebox.node.Parameter;
import nodebox.node.ParameterValueListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ContainerEvent;

public class TextControl extends AbstractParameterControl implements ActionListener {

    private JTextField textField;
    private JButton externalWindowButton;

    public TextControl(Parameter parameter) {
        super(parameter);
        setLayout(new BorderLayout(0, 0));
        textField = new JTextField();
        textField.putClientProperty("JComponent.sizeVariant", "small");
        textField.setFont(PlatformUtils.getSmallBoldFont());
        textField.addActionListener(this);
        externalWindowButton = new JButton("...");
        externalWindowButton.putClientProperty("JComponent.sizeVariant", "small");
        externalWindowButton.putClientProperty("JButton.buttonType", "gradient");
        externalWindowButton.setFont(PlatformUtils.getSmallBoldFont());
        externalWindowButton.addActionListener(this);
        add(textField, BorderLayout.CENTER);
        add(externalWindowButton, BorderLayout.EAST);
        setValueForControl(parameter.getValue());
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

}
