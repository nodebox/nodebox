package nodebox.client.parameter;

import nodebox.client.NodeBoxDocument;
import nodebox.client.PlatformUtils;
import nodebox.client.TextWindow;
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
            NodeBoxDocument doc = NodeBoxDocument.getCurrentDocument();
            if (doc == null) throw new RuntimeException("No current active document.");
            TextWindow window = new TextWindow(parameter);
            window.setLocationRelativeTo(this);
            window.setVisible(true);
            doc.addParameterEditor(window);
        }
    }

}
