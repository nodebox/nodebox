package nodebox.client;

import nodebox.node.Parameter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class ParameterAttributesEditor extends JPanel implements ActionListener, FocusListener {

    private JTextField nameField;
    private JTextField labelField;
    private JTextField helpTextField;
    private JComboBox typeBox;
    private JComboBox widgetBox;
    private JTextField valueField;
    private JComboBox boundingMethodBox;
    private JTextField minimumValueField;
    private JCheckBox minimumValueCheck;
    private JTextField maximumValueField;
    private JCheckBox maximumValueCheck;
    private JComboBox displayLevelBox;

    private boolean focusLostEvents = true;

    private Parameter parameter;

    public ParameterAttributesEditor(Parameter parameter) {
        this.parameter = parameter;
        initPanel();
        updateValues();
    }

    public void initPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JPanel contentPanel = new JPanel(new GridLayout(10, 2, 10, 5));
        // Name
        contentPanel.add(new JLabel("Name"));
        nameField = new JFormattedTextField(20);
        nameField.setEditable(false);
        contentPanel.add(nameField);
        // Label
        contentPanel.add(new JLabel("Label"));
        labelField = new JTextField(20);
        labelField.addActionListener(this);
        labelField.addFocusListener(this);
        contentPanel.add(labelField);
        // Help Text
        contentPanel.add(new JLabel("Help Text"));
        helpTextField = new JTextField(20);
        helpTextField.addActionListener(this);
        helpTextField.addFocusListener(this);
        contentPanel.add(helpTextField);
        // Type
        contentPanel.add(new JLabel("Type"));
        typeBox = new JComboBox(Parameter.Type.values());
        typeBox.addActionListener(this);
        contentPanel.add(typeBox);
        // Widget
        contentPanel.add(new JLabel("Widget"));
        widgetBox = new JComboBox(Parameter.Widget.values());
        widgetBox.addActionListener(this);
        contentPanel.add(widgetBox);
        // Value
        contentPanel.add(new JLabel("Value"));
        valueField = new JTextField(20);
        valueField.addActionListener(this);
        valueField.addFocusListener(this);
        contentPanel.add(valueField);
        // Bounding Method
        contentPanel.add(new JLabel("Bounding"));
        boundingMethodBox = new JComboBox(new String[]{"none", "soft", "hard"});
        boundingMethodBox.addActionListener(this);
        contentPanel.add(boundingMethodBox);
        contentPanel.add(new JLabel("Minimum"));
        minimumValueCheck = new JCheckBox();
        minimumValueCheck.addActionListener(this);
        minimumValueField = new JTextField(10);
        minimumValueField.addActionListener(this);
        minimumValueField.addFocusListener(this);
        JPanel minimumValuePanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 0));
        minimumValuePanel.add(minimumValueCheck);
        minimumValuePanel.add(minimumValueField);
        contentPanel.add(minimumValuePanel);
        contentPanel.add(new JLabel("Maximum"));
        maximumValueCheck = new JCheckBox();
        maximumValueCheck.addActionListener(this);
        maximumValueField = new JTextField(10);
        maximumValueField.addActionListener(this);
        maximumValueField.addFocusListener(this);
        JPanel maximumValuePanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 0));
        maximumValuePanel.add(maximumValueCheck);
        maximumValuePanel.add(maximumValueField);
        contentPanel.add(maximumValuePanel);
        contentPanel.add(new JLabel("Display Level"));
        displayLevelBox = new JComboBox(new String[]{"hud", "detail", "hidden"});
        displayLevelBox.addActionListener(this);
        contentPanel.add(displayLevelBox);
        add(contentPanel);
        Dimension fillDimension = new Dimension(0, Integer.MAX_VALUE);
        add(new Box.Filler(fillDimension, fillDimension, fillDimension));
    }

    public void updateValues() {
        nameField.setText(parameter.getName());
        labelField.setText(parameter.getLabel());
        helpTextField.setText(parameter.getHelpText());
        typeBox.setSelectedItem(parameter.getType());
        widgetBox.setSelectedItem(parameter.getWidget());
        valueField.setText(parameter.getValue().toString());
        Parameter.BoundingMethod boundingMethod = parameter.getBoundingMethod();
        boundingMethodBox.setSelectedItem(boundingMethod.toString().toLowerCase());
        Object minimumValue = parameter.getMinimumValue();
        String minimumValueString = minimumValue == null ? "" : minimumValue.toString();
        minimumValueCheck.setSelected(minimumValue != null);
        minimumValueField.setText(minimumValueString);
        minimumValueField.setEnabled(minimumValue != null);
        Object maximumValue = parameter.getMaximumValue();
        String maximumValueString = maximumValue == null ? "" : maximumValue.toString();
        maximumValueCheck.setSelected(maximumValue != null);
        maximumValueField.setText(maximumValueString);
        maximumValueField.setEnabled(maximumValue != null);
        displayLevelBox.setSelectedItem(parameter.getDisplayLevel().toString().toLowerCase());
        revalidate();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == labelField) {
            String newLabel = labelField.getText();
            if (newLabel.equals(parameter.getLabel())) return;
            parameter.setLabel(labelField.getText());
        } else if (e.getSource() == helpTextField) {
            String newHelpText = helpTextField.getText();
            if (newHelpText.equals(parameter.getHelpText())) return;
            parameter.setHelpText(helpTextField.getText());
        } else if (e.getSource() == typeBox) {
            Parameter.Type newType = (Parameter.Type) typeBox.getSelectedItem();
            if (parameter.getType().equals(newType)) return;
            parameter.setType(newType);
        } else if (e.getSource() == widgetBox) {
            Parameter.Widget newWidget = (Parameter.Widget) widgetBox.getSelectedItem();
            if (parameter.getWidget().equals(newWidget)) return;
            parameter.setWidget(newWidget);
        } else if (e.getSource() == valueField) {
            String newValue = valueField.getText();
            if (parameter.getValue() != null && parameter.getValue().toString().equals(newValue)) return;
            try {
                parameter.setValue(parameter.parseValue(valueField.getText()));
            } catch (IllegalArgumentException e1) {
                showError("Value " + valueField.getText() + " is invalid: " + e1.getMessage());
            }
        } else if (e.getSource() == boundingMethodBox) {
            Parameter.BoundingMethod newMethod = Parameter.BoundingMethod.valueOf(boundingMethodBox.getSelectedItem().toString().toUpperCase());
            if (parameter.getBoundingMethod().equals(newMethod)) return;
            parameter.setBoundingMethod(newMethod);
        } else if (e.getSource() == minimumValueCheck) {
            if (minimumValueCheck.isSelected() && parameter.getMinimumValue() != null) return;
            parameter.setMinimumValue(minimumValueCheck.isSelected() ? 0f : null);
        } else if (e.getSource() == minimumValueField) {
            try {
                float v = Float.parseFloat(minimumValueField.getText());
                if (v == parameter.getMinimumValue()) return;
                parameter.setMinimumValue(v);
            } catch (Exception e1) {
                showError("Value " + minimumValueField.getText() + " is invalid: " + e1.getMessage());
            }
        } else if (e.getSource() == maximumValueCheck) {
            if (maximumValueCheck.isSelected() && parameter.getMaximumValue() != null) return;
            parameter.setMaximumValue(maximumValueCheck.isSelected() ? 0f : null);
        } else if (e.getSource() == maximumValueField) {
            try {
                float v = Float.parseFloat(maximumValueField.getText());
                if (v == parameter.getMaximumValue()) return;
                parameter.setMaximumValue(v);
            } catch (Exception e1) {
                showError("Value " + maximumValueField.getText() + " is invalid: " + e1.getMessage());
            }
        } else if (e.getSource() == displayLevelBox) {
            Parameter.DisplayLevel newDisplayLevel = Parameter.DisplayLevel.valueOf(displayLevelBox.getSelectedItem().toString().toUpperCase());
            if (parameter.getDisplayLevel() == newDisplayLevel) return;
            parameter.setDisplayLevel(newDisplayLevel);
        } else {
            throw new AssertionError("Unknown source " + e.getSource());
        }
        updateValues();
    }

    private void showError(String msg) {
        // The message dialog popup will cause a focus lost event to be thrown,
        // which will cause another action event, throwing up a second dialog popup.
        // We temporarily disable focus lost events.
        focusLostEvents = false;
        JOptionPane.showMessageDialog(this, msg, "NodeBox", JOptionPane.ERROR_MESSAGE);
        focusLostEvents = true;
    }


    public void focusGained(FocusEvent e) {
        // Do nothing.
    }

    public void focusLost(FocusEvent e) {
        if (!focusLostEvents) return;
        actionPerformed(new ActionEvent(e.getSource(), 0, "focusLost"));
    }
}
