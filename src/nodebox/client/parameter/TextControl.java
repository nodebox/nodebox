package nodebox.client.parameter;

import nodebox.client.NodeBoxDocument;
import nodebox.client.PlatformUtils;
import nodebox.client.TextWindow;
import nodebox.client.Theme;
import nodebox.node.Parameter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TextControl extends AbstractParameterControl implements ActionListener {

    private JTextField textField;
    private JButton externalWindowButton;

    public TextControl(Parameter parameter) {
        super(parameter);
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
        setValueForControl(parameter.getValue());
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
            parameter.setValue(textField.getText());
        } else if (e.getSource() == externalWindowButton) {
            NodeBoxDocument doc = NodeBoxDocument.getCurrentDocument();
            if (doc == null) throw new RuntimeException("No current active document.");
            TextWindow window = new TextWindow(parameter);
            window.setLocationRelativeTo(this);
            window.setVisible(true);
            doc.addParameterEditor(window);
        }
    }

}
