package nodebox.client.port;

import nodebox.node.Port;
import nodebox.ui.Theme;

import javax.swing.*;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DataControl extends AbstractPortControl implements ActionListener {

    //private JButton showDataButton;
    private JButton clearDataButton;

    public DataControl(String nodePath, Port port) {
        super(nodePath, port);
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        clearDataButton = new JButton("Clear");
        clearDataButton.setMargin(new Insets(1, 0, 0, 0));
        clearDataButton.putClientProperty("JButton.buttonType", "textured");
        clearDataButton.putClientProperty("JComponent.sizeVariant", "small");
        clearDataButton.setFont(Theme.SMALL_BOLD_FONT);
        clearDataButton.setForeground(Theme.TEXT_NORMAL_COLOR);
        clearDataButton.addActionListener(this);
        add(clearDataButton);
        /*showDataButton = new JButton("Show Data...");
        showDataButton.setMargin(new Insets(1, 0, 0, 0));
        showDataButton.putClientProperty("JButton.buttonType", "textured");
        showDataButton.putClientProperty("JComponent.sizeVariant", "small");
        showDataButton.setFont(Theme.SMALL_BOLD_FONT);
        showDataButton.setForeground(Theme.TEXT_NORMAL_COLOR);
        showDataButton.addActionListener(this);
        add(showDataButton);*/
        add(Box.createHorizontalGlue());
    }

    public void setValueForControl(Object v) {
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == clearDataButton) {
            setPortValue("");
        } /*else if (e.getSource() == showDataButton) {
            NodeBoxDocument doc = NodeBoxDocument.getCurrentDocument();
            if (doc == null) throw new RuntimeException("No current active document.");
            TextWindow window = new TextWindow(parameter);
            window.setLocationRelativeTo(this);
            window.setVisible(true);
            doc.addParameterEditor(window);
        } */
    }
}