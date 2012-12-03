package nodebox.client.port;

import nodebox.node.Port;
import nodebox.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public class StringControl extends AbstractPortControl implements ActionListener {

    private JTextField textField;

    public StringControl(Port port) {
        super(port);
        setLayout(new BorderLayout());
        textField = new JTextField();
        textField.putClientProperty("JComponent.sizeVariant", "small");
        textField.setFont(Theme.SMALL_BOLD_FONT);
        textField.addActionListener(this);
        textField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                commitTextFieldValue();
            }
        });
        add(textField, BorderLayout.CENTER);
        setValueForControl(port.getValue());
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
        commitTextFieldValue();
    }

    private void commitTextFieldValue() {
        setPortValue(textField.getText());

    }
}
