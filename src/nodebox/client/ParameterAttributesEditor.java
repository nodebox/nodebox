package nodebox.client;

import nodebox.Icons;
import nodebox.node.Parameter;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.*;

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
    private JTable menuItemsTable;
    private JButton addButton;
    private JButton removeButton;
    private JButton upButton;
    private JButton downButton;

    private boolean focusLostEvents = true;

    private Parameter parameter;

    public ParameterAttributesEditor(Parameter parameter) {
        this.parameter = parameter;
        initPanel();
        updateValues();
    }

    public void initPanel() {
        //setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setLayout(new BorderLayout(0, 0));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        FormPanel form = new FormPanel();

        //contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.PAGE_AXIS));
        // Name
        //contentPanel.add(new JLabel("Name"));
        nameField = new JFormattedTextField(20);
        nameField.setEditable(false);
        form.addRow("Name", nameField);
        //contentPanel.add(nameField);
        // Label
        //contentPanel.add(new JLabel("Label"));
        labelField = new JTextField(20);
        labelField.addActionListener(this);
        labelField.addFocusListener(this);
        form.addRow("Label", labelField);
        //contentPanel.add(labelField);
        // Help Text
        //contentPanel.add(new JLabel("Help Text"));
        helpTextField = new JTextField(20);
        helpTextField.addActionListener(this);
        helpTextField.addFocusListener(this);
        //contentPanel.add(helpTextField);
        form.addRow("Help Text", helpTextField);
        // Type
        //contentPanel.add(new JLabel("Type"));
        typeBox = new JComboBox(Parameter.Type.values());
        typeBox.addActionListener(this);
        //contentPanel.add(typeBox);
        form.addRow("Type", typeBox);
        // Widget
        //contentPanel.add(new JLabel("Widget"));
        widgetBox = new JComboBox(Parameter.Widget.values());
        widgetBox.addActionListener(this);
        //contentPanel.add(widgetBox);
        form.addRow("Widget", widgetBox);
        // Value
        //contentPanel.add(new JLabel("Value"));
        valueField = new JTextField(20);
        valueField.addActionListener(this);
        valueField.addFocusListener(this);
        //contentPanel.add(valueField);
        form.addRow("Value", valueField);
        // Bounding Method
        //contentPanel.add(new JLabel("Bounding"));
        boundingMethodBox = new JComboBox(new String[]{"none", "soft", "hard"});
        boundingMethodBox.addActionListener(this);
        //contentPanel.add(boundingMethodBox);
        form.addRow("Bounding", boundingMethodBox);
        // Minimum Value
        //contentPanel.add(new JLabel("Minimum"));
        minimumValueCheck = new JCheckBox();
        minimumValueCheck.addActionListener(this);
        minimumValueField = new JTextField(10);
        minimumValueField.addActionListener(this);
        minimumValueField.addFocusListener(this);
        JPanel minimumValuePanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 0));
        minimumValuePanel.add(minimumValueCheck);
        minimumValuePanel.add(minimumValueField);
        //contentPanel.add(minimumValuePanel);
        form.addRow("Minimum", minimumValuePanel);
        // Maximum Value
        //contentPanel.add(new JLabel("Maximum"));
        maximumValueCheck = new JCheckBox();
        maximumValueCheck.addActionListener(this);
        maximumValueField = new JTextField(10);
        maximumValueField.addActionListener(this);
        maximumValueField.addFocusListener(this);
        JPanel maximumValuePanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 0));
        maximumValuePanel.add(maximumValueCheck);
        maximumValuePanel.add(maximumValueField);
        //contentPanel.add(maximumValuePanel);
        form.addRow("Maximum", maximumValuePanel);
        // Display Level
        //contentPanel.add(new JLabel("Display Level"));
        displayLevelBox = new JComboBox(new String[]{"hud", "detail", "hidden"});
        displayLevelBox.addActionListener(this);
        //contentPanel.add(displayLevelBox);
        form.addRow("Display Level", displayLevelBox);
        // Menu Items
        //contentPanel.add(new JLabel("Menu Items"));
        menuItemsTable = new JTable(new MenuItemsModel());
        menuItemsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel tablePanel = new JPanel(new BorderLayout(5, 5));
        JScrollPane tableScroll = new JScrollPane(menuItemsTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        tableScroll.setSize(200, 200);
        tableScroll.setPreferredSize(new Dimension(200, 200));
        tableScroll.setMaximumSize(new Dimension(200, 200));
        tableScroll.setMinimumSize(new Dimension(200, 200));
        tablePanel.add(tableScroll, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 5));
        addButton = new JButton(new Icons.PlusIcon());
        addButton.addActionListener(this);
        removeButton = new JButton(new Icons.MinusIcon());
        removeButton.addActionListener(this);
        upButton = new JButton(new Icons.ArrowIcon(Icons.ArrowIcon.NORTH));
        upButton.addActionListener(this);
        downButton = new JButton(new Icons.ArrowIcon(Icons.ArrowIcon.SOUTH));
        downButton.addActionListener(this);
        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(upButton);
        buttonPanel.add(downButton);
        tablePanel.add(buttonPanel, BorderLayout.SOUTH);
        form.addRow("Menu Items", tablePanel);
        add(form, BorderLayout.CENTER);

        //Dimension fillDimension = new Dimension(0, Integer.MAX_VALUE);
        //add(Box.createVerticalGlue());
        //add(new Box.Filler(fillDimension, fillDimension, fillDimension));
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
        menuItemsTable.tableChanged(new TableModelEvent(menuItemsTable.getModel()));
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
        } else if (e.getSource() == addButton) {
            MenuItemDialog dialog = new MenuItemDialog((Frame) SwingUtilities.getRoot(this));
            dialog.setVisible(true);
            if (dialog.isSuccessful()) {
                parameter.addMenuItem(dialog.getKey(), dialog.getLabel());
                menuItemsTable.tableChanged(new TableModelEvent(menuItemsTable.getModel()));
            }
        } else if (e.getSource() == removeButton) {
            Parameter.MenuItem item = parameter.getMenuItems().get(menuItemsTable.getSelectedRow());
            parameter.removeMenuItem(item);
        } else if (e.getSource() == upButton) {
            moveItemUp();
        } else if (e.getSource() == downButton) {
            moveItemDown();
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

    private void moveItemDown() {
        int index = menuItemsTable.getSelectedRow();
        // Return if nothing was selected.
        if (index == -1) return;
        java.util.List<Parameter.MenuItem> items = parameter.getMenuItems();
        // Return if the last item is selected.
        if (index >= items.size() - 1) return;
        Parameter.MenuItem selectedItem = items.get(index);
        items.remove(selectedItem);
        items.add(index + 1, selectedItem);
        parameter.fireAttributeChanged();
        // TODO: Changing the selection doesn't have any effect on Mac.
        menuItemsTable.changeSelection(index + 1, 1, false, false);
    }

    private void moveItemUp() {
        int index = menuItemsTable.getSelectedRow();
        // Return if nothing was selected.
        if (index == -1) return;
        // Return if the first item is selected.
        if (index == 0) return;
        java.util.List<Parameter.MenuItem> items = parameter.getMenuItems();
        Parameter.MenuItem selectedItem = items.get(index);
        items.remove(selectedItem);
        items.add(index - 1, selectedItem);
        parameter.fireAttributeChanged();
        // TODO: Changing the selection doesn't have any effect on Mac.
        menuItemsTable.changeSelection(index - 1, 1, false, false);
    }

    private class MenuItemsModel extends AbstractTableModel {
        public int getRowCount() {
            return parameter.getMenuItems().size();
        }

        public int getColumnCount() {
            return 2;
        }

        public Object getValueAt(int row, int column) {
            Parameter.MenuItem item = parameter.getMenuItems().get(row);
            if (column == 0) {
                return item.getKey();
            } else {
                return item.getLabel();
            }
        }

        @Override
        public String getColumnName(int column) {
            if (column == 0) {
                return "Key";
            } else {
                return "Label";
            }
        }
    }

    private class FormPanel extends JPanel {
        GridBagLayout layout = new GridBagLayout();
        private int rowCount = 0;

        private FormPanel() {
            setLayout(layout);
        }

        public void addRow(String label, JComponent component) {
            GridBagConstraints labelConstraints = new GridBagConstraints();
            labelConstraints.gridx = 0;
            labelConstraints.gridy = rowCount;
            labelConstraints.insets = new Insets(0, 0, 5, 5);
            labelConstraints.anchor = GridBagConstraints.LINE_END;

            GridBagConstraints componentConstraints = new GridBagConstraints();
            componentConstraints.gridx = 1;
            componentConstraints.gridy = rowCount;
            componentConstraints.gridwidth = GridBagConstraints.REMAINDER;
            componentConstraints.fill = GridBagConstraints.HORIZONTAL;
            componentConstraints.insets = new Insets(0, 0, 5, 0);
            componentConstraints.anchor = GridBagConstraints.LINE_START;

            JLabel l = new JLabel(label + ":");
            add(l, labelConstraints);
            add(component, componentConstraints);
            rowCount++;

            // Add another column/row that takes up all available space.
            // This moves the layout to the top-left corner.
            layout.columnWidths = new int[]{0, 0, 0};
            layout.columnWeights = new double[]{0.0, 0.0, 1.0E-4};
            layout.rowHeights = new int[rowCount + 1];
            layout.rowWeights = new double[rowCount + 1];
            layout.rowWeights[rowCount] = 1.0E-4;
        }
    }

    private class MenuItemDialog extends JDialog {
        private boolean successful = false;
        private JTextField keyField;
        private JTextField labelField;
        private JButton okButton, cancelButton;

        private MenuItemDialog(Frame frame) {
            this(frame, new Parameter.MenuItem("", ""));
        }

        private MenuItemDialog(Frame frame, Parameter.MenuItem item) {
            super(frame, "Menu Item", true);
            setResizable(false);
            setLocationByPlatform(true);
            JPanel content = new JPanel(new BorderLayout());
            FormPanel form = new FormPanel();
            keyField = new JTextField(item.getKey());
            labelField = new JTextField(item.getLabel());
            form.addRow("Key", keyField);
            form.addRow("Label", labelField);
            content.add(form, BorderLayout.CENTER);
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 5, 0));
            okButton = new JButton("OK");
            okButton.addActionListener(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    // Commit key and label.
                    successful = true;
                    MenuItemDialog.this.setVisible(false);
                }
            });
            cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    MenuItemDialog.this.setVisible(false);
                }
            });
            buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
            buttonPanel.add(cancelButton);
            buttonPanel.add(okButton);
            content.add(buttonPanel, BorderLayout.SOUTH);
            setContentPane(content);
            content.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 20));
            getRootPane().setDefaultButton(okButton);
            // Close window when escape key is pressed.
            getRootPane().registerKeyboardAction(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setVisible(false);
                }
            }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
            pack();
        }

        public String getKey() {
            return keyField.getText();
        }

        public String getLabel() {
            return labelField.getText();
        }

        public boolean isSuccessful() {
            return successful;
        }
    }

}
