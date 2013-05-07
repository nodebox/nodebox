package nodebox.client.port;

import nodebox.node.Port;
import nodebox.ui.Theme;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public class PasswordControl extends StringControl {

    public PasswordControl(String nodePath, Port port) {
        super(nodePath, port);
    }

    @Override
    protected JTextComponent createField() {
        JPasswordField field = new JPasswordField();
        field.putClientProperty("JComponent.sizeVariant", "small");
        field.setFont(Theme.SMALL_BOLD_FONT);
        field.addActionListener(this);
        field.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                commitTextFieldValue();
            }
        });
        return field;
    }

}
