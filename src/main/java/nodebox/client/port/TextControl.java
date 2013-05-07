package nodebox.client.port;

import nodebox.client.NodeBoxDocument;
import nodebox.node.Port;
import nodebox.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TextControl extends AbstractPortControl implements ActionListener {

    private JTextField textField;
    private JButton externalWindowButton;

    public TextControl(String nodePath, Port port) {
        super(nodePath, port);
        setLayout(new BorderLayout(0, 0));
        textField = new JTextField();
        textField.putClientProperty("JComponent.sizeVariant", "small");
        textField.setFont(Theme.SMALL_BOLD_FONT);
        textField.addActionListener(this);
        externalWindowButton = new JButton("...");
        externalWindowButton.putClientProperty("JComponent.sizeVariant", "small");
        externalWindowButton.putClientProperty("JButton.buttonType", "gradient");
        externalWindowButton.setFont(Theme.SMALL_BOLD_FONT);
        externalWindowButton.addActionListener(this);
        add(textField, BorderLayout.CENTER);
        add(externalWindowButton, BorderLayout.EAST);
        setValueForControl(port.getValue());
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        textField.setEnabled(enabled);
        externalWindowButton.setEnabled(enabled);
    }

    public void setValueForControl(Object v) {
        textField.setText(v.toString());
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == textField) {
            setPortValue(textField.getText());
        } else if (e.getSource() == externalWindowButton) {
            NodeBoxDocument doc = NodeBoxDocument.getCurrentDocument();
            if (doc == null) throw new RuntimeException("No current active document.");
//            TextWindow window = new TextWindow(port);
//            window.setLocationRelativeTo(this);
//            window.setVisible(true);
//            doc.addPortEditor(window);
        }
    }

}
