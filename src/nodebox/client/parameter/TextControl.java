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
