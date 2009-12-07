package nodebox.client;

import nodebox.node.Node;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class NodeSettingsEditor extends JPanel implements ActionListener, FocusListener {

    private Node node;
    private JCheckBox exportBox;

    public NodeSettingsEditor(Node node) {
        this.node = node;
        initPanel();
        updateValues();
    }

    private void initPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        // Exported
        exportBox = new JCheckBox("Exported (available to users of this library)");
        exportBox.addActionListener(this);
        contentPanel.add(exportBox);
        contentPanel.add(Box.createVerticalGlue());
        add(contentPanel);
    }

    public void updateValues() {
        exportBox.setSelected(node.isExported());
        revalidate();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == exportBox) {
            node.setExported(exportBox.isSelected());
        } else {
            throw new AssertionError("Unknown source " + e.getSource());
        }
        updateValues();
    }

    public void focusGained(FocusEvent e) {
        // Do nothing.
    }

    public void focusLost(FocusEvent e) {
        actionPerformed(new ActionEvent(e.getSource(), 0, "focusLost"));
    }

}
