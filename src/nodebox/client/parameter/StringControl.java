package nodebox.client.parameter;

import nodebox.client.NodeBoxDocument;
import nodebox.client.Theme;
import nodebox.node.Parameter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class StringControl extends AbstractParameterControl implements ActionListener {

    private JTextField textField;

    public StringControl(NodeBoxDocument document, Parameter parameter) {
        super(document, parameter);
        setLayout(new BorderLayout());
        textField = new JTextField();
        textField.putClientProperty("JComponent.sizeVariant", "small");
        textField.setFont(Theme.SMALL_BOLD_FONT);
        textField.addActionListener(this);
        add(textField, BorderLayout.CENTER);
        setValueForControl(parameter.getValue());
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        textField.setEnabled(enabled);
    }

    public void setValueForControl(Object v) {
        if (v == null) return;
        textField.setText(v.toString());
    }

    public void actionPerformed(ActionEvent e) {
        String newValue = textField.getText();
        if (!newValue.equals(parameter.asString())) {
            setParameterValue(newValue);
        }
    }

}
