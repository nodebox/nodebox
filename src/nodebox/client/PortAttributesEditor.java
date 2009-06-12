package nodebox.client;

import nodebox.node.Parameter;
import nodebox.node.Port;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class PortAttributesEditor extends JPanel implements ActionListener, FocusListener {

    private JTextField nameField;
    private JTextField dataClassField;
    private JComboBox cardinalityBox;

    private Port port;

    public PortAttributesEditor(Port port) {
        super(null);
        this.port = port;
        initPanel();
        updateValues();
    }

    public void initPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JPanel contentPanel = new JPanel(new GridLayout(3, 2, 10, 5));
        // Name
        contentPanel.add(new JLabel("Name"));
        nameField = new JTextField(20);
        nameField.setEnabled(false);
        contentPanel.add(nameField);
        // Data Class
        contentPanel.add(new JLabel("Data Class"));
        dataClassField = new JTextField(20);
        dataClassField.addActionListener(this);
        dataClassField.addFocusListener(this);
        dataClassField.setEnabled(false);
        contentPanel.add(dataClassField);
        // Cardinality
        contentPanel.add(new JLabel("Cardinality"));
        cardinalityBox = new JComboBox(Port.Cardinality.values());
        cardinalityBox.setEnabled(false);
        contentPanel.add(cardinalityBox);
        add(contentPanel);
        Dimension fillDimension = new Dimension(0, Integer.MAX_VALUE);
        add(new Box.Filler(fillDimension, fillDimension, fillDimension));
    }

    public void updateValues() {
        nameField.setText(port.getName());
        dataClassField.setText(port.getDataClass().getName());
        cardinalityBox.setSelectedItem(port.getCardinality());
        revalidate();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == nameField) {
            //port.setName(nameField.getText());
        } else if (e.getSource() == dataClassField) {
            //port.setDataClass(dataClassField.getText());
        } else if (e.getSource() == cardinalityBox) {
            Port.Cardinality cardinality = Port.Cardinality.valueOf(cardinalityBox.getSelectedItem().toString().toUpperCase());
            //port.setCardinality(cardinality);
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
