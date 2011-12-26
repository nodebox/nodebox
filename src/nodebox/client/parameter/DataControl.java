package nodebox.client.parameter;

import nodebox.client.NodeBoxDocument;
import nodebox.client.TextWindow;
import nodebox.client.Theme;
import nodebox.node.Parameter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DataControl extends AbstractParameterControl implements ActionListener {

    private JButton showDataButton;
    private JButton clearDataButton;

    public DataControl(Parameter parameter) {
        super(parameter);
        setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        clearDataButton = new JButton("Clear");
        clearDataButton.putClientProperty("JComponent.sizeVariant", "small");
        clearDataButton.putClientProperty("JButton.buttonType", "gradient");
        clearDataButton.setFont(Theme.SMALL_BOLD_FONT);
        clearDataButton.addActionListener(this);
        add(clearDataButton);
        showDataButton = new JButton("Show Data...");
        showDataButton.putClientProperty("JComponent.sizeVariant", "small");
        showDataButton.putClientProperty("JButton.buttonType", "gradient");
        showDataButton.setFont(Theme.SMALL_BOLD_FONT);
        showDataButton.addActionListener(this);
        add(showDataButton);
    }

    public void setValueForControl(Object v) {
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == clearDataButton) {
            setParameterValue("");
        } else if (e.getSource() == showDataButton) {
            NodeBoxDocument doc = NodeBoxDocument.getCurrentDocument();
            if (doc == null) throw new RuntimeException("No current active document.");
            TextWindow window = new TextWindow(parameter);
            window.setLocationRelativeTo(this);
            window.setVisible(true);
            doc.addParameterEditor(window);
        }
    }
}
