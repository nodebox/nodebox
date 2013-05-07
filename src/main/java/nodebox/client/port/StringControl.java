package nodebox.client.port;

import nodebox.node.Port;
import nodebox.ui.Theme;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public class StringControl extends AbstractPortControl implements ActionListener {

    private JTextComponent field;

    public StringControl(String nodePath, Port port) {
        super(nodePath, port);
        setLayout(new BorderLayout());
        field = createField();
        add(field, BorderLayout.CENTER);
        setValueForControl(port.getValue());
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        field.setEnabled(enabled);
    }

    public void setValueForControl(Object v) {
        if (v == null) return;
        field.setText(v.toString());
    }

    public void actionPerformed(ActionEvent e) {
        commitTextFieldValue();
    }

    protected void commitTextFieldValue() {
        setPortValue(field.getText());
    }

    protected JTextComponent createField() {
        JTextField textField = new JTextField();
        textField.putClientProperty("JComponent.sizeVariant", "small");
        textField.setFont(Theme.SMALL_BOLD_FONT);
        textField.addActionListener(this);
        textField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                commitTextFieldValue();
            }
        });
        return textField;
    }

}
