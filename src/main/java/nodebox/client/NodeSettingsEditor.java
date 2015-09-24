package nodebox.client;

import nodebox.node.Port;
import nodebox.ui.Theme;
import nodebox.node.Node;
import nodebox.util.HumanizedObject;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Locale;

public class NodeSettingsEditor extends JPanel implements ActionListener, FocusListener {

    private JTextField categoryField;
    private JTextField descriptionField;
    private JTextField imageField;
    private JTextField outputTypeField;
    private JComboBox<HumanizedObject> outputRangeBox;
    private JTextField functionField;
    private JTextField handleField;

    private static HumanizedObject[] humanizedRanges;

    static {
        humanizedRanges = new HumanizedObject[Port.Range.values().length];
        for (int i = 0; i < Port.Range.values().length; i++) {
            humanizedRanges[i] = new HumanizedObject(Port.Range.values()[i]);
        }
    }

    private int y = 0;

    private NodeAttributesDialog nodeAttributesDialog;

    public NodeSettingsEditor(NodeAttributesDialog dialog) {
        this.nodeAttributesDialog = dialog;
        initPanel();
        updateValues();
    }

    private void addRow(String label, JComponent component) {
        JLabel l = new JLabel(label);
        l.setFont(Theme.SMALL_BOLD_FONT);
        l.setBounds(18, y, 400, 18);
        add(l);
        y += 18;
        int componentHeight = (int) component.getPreferredSize().getHeight();
        component.setBounds(16, y, 400, componentHeight);
        y += componentHeight;
        y += 2; // vertical gap
        add(component);
    }

    public void initPanel() {
        // The panel uses an absolute layout.
        setLayout(null);

        // Category
        categoryField = new JTextField(20);
        categoryField.addActionListener(this);
        categoryField.addFocusListener(this);
        addRow("Category", categoryField);

        // Description
        descriptionField = new JTextField(20);
        descriptionField.addActionListener(this);
        descriptionField.addFocusListener(this);
        addRow("Description", descriptionField);

        // Image
        imageField = new JTextField(20);
        imageField.addActionListener(this);
        imageField.addFocusListener(this);
        addRow("Image", imageField);

        // Output Type
        outputTypeField = new JTextField(20);
        outputTypeField.addActionListener(this);
        outputTypeField.addFocusListener(this);
        addRow("Output Type", outputTypeField);

        // Output Range
        outputRangeBox = new JComboBox<>(humanizedRanges);
        outputRangeBox.addActionListener(this);
        addRow("Output Range", outputRangeBox);

        // Function
        functionField = new JTextField(20);
        functionField.addActionListener(this);
        functionField.addFocusListener(this);
        addRow("Function", functionField);

        // Handle Function
        handleField = new JTextField(20);
        handleField.addActionListener(this);
        handleField.addFocusListener(this);
        addRow("Handle Function", handleField);
    }

    private Node getNode() {
        return nodeAttributesDialog.getNode();
    }

    public void updateValues() {
        Node node = getNode();
        categoryField.setText(node.getCategory());
        descriptionField.setText(node.getDescription());
        imageField.setText(node.getImage());
        outputTypeField.setText(node.getOutputType());
        outputRangeBox.setSelectedItem(getHumanizedRange(node.getOutputRange()));
        functionField.setText(node.getFunction());
        handleField.setText(node.getHandle());
    }

    private HumanizedObject getHumanizedRange(Port.Range range) {
        for (HumanizedObject humanizedRange : humanizedRanges) {
            if (humanizedRange.getObject() == range) return humanizedRange;
        }
        throw new AssertionError("Range is not in humanized range list.");
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == categoryField)
            setCategory();
        else if (e.getSource() == descriptionField)
            setDescription();
        else if (e.getSource() == imageField)
            setImage();
        else if (e.getSource() == outputTypeField)
            setOutputType();
        else if (e.getSource() == outputRangeBox)
            setOutputRange();
        else if (e.getSource() == functionField)
            setFunction();
        else if (e.getSource() == handleField)
            setHandle();
    }

    private void setCategory() {
        Node node = getNode();
        String newValue = categoryField.getText();
        if (node.getCategory() != null && node.getCategory().equals(newValue)) return;
        nodeAttributesDialog.setNodeCategory(newValue);
    }

    private void setDescription() {
        Node node = getNode();
        String newValue = descriptionField.getText();
        if (node.getDescription() != null && node.getDescription().equals(newValue)) return;
        nodeAttributesDialog.setNodeDescription(newValue);
    }

    private void setImage() {
        Node node = getNode();
        String newValue = imageField.getText();
        if (node.getImage() != null && node.getImage().equals(newValue)) return;
        nodeAttributesDialog.setNodeImage(newValue);
    }

    private void setOutputType() {
        Node node = getNode();
        String newValue = outputTypeField.getText().toLowerCase(Locale.US);
        if (node.getOutputType() != null && node.getOutputType().equals(newValue)) return;
        nodeAttributesDialog.setNodeOutputType(newValue);
    }

    private void setOutputRange() {
        Node node = getNode();
        HumanizedObject newRange = (HumanizedObject) outputRangeBox.getSelectedItem();
        if (node.getOutputRange() == newRange.getObject()) return;
        nodeAttributesDialog.setNodeOutputRange((Port.Range) newRange.getObject());
    }

    private void setFunction() {
        Node node = getNode();
        String newValue = functionField.getText();
        if (node.getFunction() != null && node.getFunction().equals(newValue)) return;
        nodeAttributesDialog.setNodeFunction(newValue);
    }

    private void setHandle() {
        Node node = getNode();
        String newValue = handleField.getText();
        if (node.getHandle() != null && node.getHandle().equals(newValue)) return;
        nodeAttributesDialog.setNodeHandle(newValue);
    }

    private void applyChanges() {
        setCategory();
        setDescription();
        setImage();
        setOutputType();
        setOutputRange();
        setFunction();
        setHandle();
    }

    public void focusGained(FocusEvent e) {
    }

    public void focusLost(FocusEvent e) {
        applyChanges();
    }


}
