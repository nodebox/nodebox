package nodebox.client;

import nodebox.Icons;
import nodebox.node.Parameter;
import nodebox.util.StringUtils;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.Locale;

public class ParameterAttributesEditor extends JPanel implements ActionListener, FocusListener {

    // TODO: Decouple from the Parameter.
    // TODO: Don't update immediately, use save/cancel buttons.

    private JTextField nameField;
    private JTextField labelField;
    private JTextField helpTextField;
    private JComboBox widgetBox;
    private JTextField valueField;
    private JTextField enableIfField;
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

    private static HumanizedWidget[] humanizedWidgets;

    private static class HumanizedWidget {
        private Parameter.Widget widget;

        private HumanizedWidget(Parameter.Widget widget) {
            this.widget = widget;
        }

        public Parameter.Widget getWidget() {
            return widget;
        }

        @Override
        public String toString() {
            return StringUtils.humanizeConstant(widget.toString());
        }
    }

    static {
        Parameter.Widget[] widgets = Parameter.Widget.values();
        humanizedWidgets = new HumanizedWidget[widgets.length];
        for (int i = 0; i < widgets.length; i++) {
            humanizedWidgets[i] = new HumanizedWidget(widgets[i]);
        }
    }

    private int y = 0;

    private boolean focusLostEvents = true;

    private NodeAttributesDialog nodeAttributesDialog;
    private Parameter parameter;

    public ParameterAttributesEditor(NodeAttributesDialog dialog, Parameter parameter) {
        this.nodeAttributesDialog = dialog;
        this.parameter = parameter;
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

        // Name
        nameField = new JFormattedTextField(20);
        nameField.setEditable(false);
        addRow("Name", nameField);

        // Label
        labelField = new JTextField(20);
        labelField.addActionListener(this);
        labelField.addFocusListener(this);
        addRow("Label", labelField);

        // Help Text
        helpTextField = new JTextField(20);
        helpTextField.addActionListener(this);
        helpTextField.addFocusListener(this);
        addRow("Help Text", helpTextField);

        // Widget
        widgetBox = new JComboBox(humanizedWidgets);
        widgetBox.addActionListener(this);
        addRow("Type", widgetBox);

        // Value
        valueField = new JTextField(20);
        valueField.addActionListener(this);
        valueField.addFocusListener(this);
        addRow("Value", valueField);

        // Enable If
        enableIfField = new JTextField(20);
        enableIfField.addActionListener(this);
        enableIfField.addFocusListener(this);
        addRow("Enable If", enableIfField);

        // Bounding Method
        boundingMethodBox = new JComboBox(new String[]{"none", "soft", "hard"});
        boundingMethodBox.addActionListener(this);
        addRow("Bounding", boundingMethodBox);

        // Minimum Value
        minimumValueCheck = new JCheckBox();
        minimumValueCheck.addActionListener(this);
        minimumValueField = new JTextField(10);
        minimumValueField.addActionListener(this);
        minimumValueField.addFocusListener(this);
        JPanel minimumValuePanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 0));
        minimumValuePanel.add(minimumValueCheck);
        minimumValuePanel.add(minimumValueField);
        addRow("Minimum", minimumValuePanel);

        // Maximum Value
        maximumValueCheck = new JCheckBox();
        maximumValueCheck.addActionListener(this);
        maximumValueField = new JTextField(10);
        maximumValueField.addActionListener(this);
        maximumValueField.addFocusListener(this);
        JPanel maximumValuePanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 0));
        maximumValuePanel.add(maximumValueCheck);
        maximumValuePanel.add(maximumValueField);
        addRow("Maximum", maximumValuePanel);

        // Display Level
        displayLevelBox = new JComboBox(new String[]{"hud", "detail", "hidden"});
        displayLevelBox.addActionListener(this);
        addRow("Display Level", displayLevelBox);

        // Menu Items
        menuItemsTable = new JTable(new MenuItemsModel());
        menuItemsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        menuItemsTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2)
                    updateMenuItem();
            }
        });
        JPanel tablePanel = new JPanel(new BorderLayout(5, 5));
        JScrollPane tableScroll = new JScrollPane(menuItemsTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        tableScroll.setSize(200, 170);
        tableScroll.setPreferredSize(new Dimension(200, 170));
        tableScroll.setMaximumSize(new Dimension(200, 170));
        tableScroll.setMinimumSize(new Dimension(200, 170));
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
        addRow("Menu Items", tablePanel);
    }

    public void updateValues() {
        nameField.setText(parameter.getName());
        labelField.setText(parameter.getLabel());
        helpTextField.setText(parameter.getHelpText());
        widgetBox.setSelectedItem(getHumanizedWidget(parameter.getWidget()));
        valueField.setText(parameter.getValue().toString());
        enableIfField.setText(parameter.getEnableExpression());
        Parameter.BoundingMethod boundingMethod = parameter.getBoundingMethod();
        boundingMethodBox.setSelectedItem(boundingMethod.toString().toLowerCase(Locale.US));
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
        displayLevelBox.setSelectedItem(parameter.getDisplayLevel().toString().toLowerCase(Locale.US));
        menuItemsTable.tableChanged(new TableModelEvent(menuItemsTable.getModel()));
        revalidate();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == labelField) {
            String newLabel = labelField.getText();
            if (newLabel.equals(parameter.getLabel())) return;
            nodeAttributesDialog.setParameterLabel(parameter, labelField.getText());
        } else if (e.getSource() == helpTextField) {
            String newHelpText = helpTextField.getText();
            if (newHelpText.equals(parameter.getHelpText())) return;
            nodeAttributesDialog.setParameterHelpText(parameter, helpTextField.getText());
        } else if (e.getSource() == widgetBox) {
            HumanizedWidget newWidget = (HumanizedWidget) widgetBox.getSelectedItem();
            if (parameter.getWidget() == newWidget.getWidget()) return;
            nodeAttributesDialog.setParameterWidget(parameter, newWidget.getWidget());
        } else if (e.getSource() == valueField) {
            String newValue = valueField.getText();
            if (parameter.getValue() != null && parameter.getValue().toString().equals(newValue)) return;
            try {
                nodeAttributesDialog.setParameterValue(parameter, parameter.parseValue(valueField.getText()));
            } catch (IllegalArgumentException e1) {
                showError("Value " + valueField.getText() + " is invalid: " + e1.getMessage());
            }
        } else if (e.getSource() == enableIfField) {
            String newEnableExpression = enableIfField.getText();
            if (newEnableExpression.equals(parameter.getEnableExpression())) return;
            nodeAttributesDialog.setParameterEnableExpression(parameter, newEnableExpression);
        } else if (e.getSource() == boundingMethodBox) {
            Parameter.BoundingMethod newMethod = Parameter.BoundingMethod.valueOf(boundingMethodBox.getSelectedItem().toString().toUpperCase(Locale.US));
            if (parameter.getBoundingMethod().equals(newMethod)) return;
            nodeAttributesDialog.setParameterBoundingMethod(parameter, newMethod);
        } else if (e.getSource() == minimumValueCheck) {
            if (minimumValueCheck.isSelected() && parameter.getMinimumValue() != null) return;
            nodeAttributesDialog.setParameterMinimumValue(parameter, minimumValueCheck.isSelected() ? 0f : null);
        } else if (e.getSource() == minimumValueField) {
            try {
                float v = Float.parseFloat(minimumValueField.getText());
                if (v == parameter.getMinimumValue()) return;
                nodeAttributesDialog.setParameterMinimumValue(parameter, v);
            } catch (Exception e1) {
                showError("Value " + minimumValueField.getText() + " is invalid: " + e1.getMessage());
            }
        } else if (e.getSource() == maximumValueCheck) {
            if (maximumValueCheck.isSelected() && parameter.getMaximumValue() != null) return;
            nodeAttributesDialog.setParameterMaximumValue(parameter, maximumValueCheck.isSelected() ? 0f : null);
        } else if (e.getSource() == maximumValueField) {
            try {
                float v = Float.parseFloat(maximumValueField.getText());
                if (v == parameter.getMaximumValue()) return;
                nodeAttributesDialog.setParameterMaximumValue(parameter, v);
            } catch (Exception e1) {
                showError("Value " + maximumValueField.getText() + " is invalid: " + e1.getMessage());
            }
        } else if (e.getSource() == displayLevelBox) {
            Parameter.DisplayLevel newDisplayLevel = Parameter.DisplayLevel.valueOf(displayLevelBox.getSelectedItem().toString().toUpperCase(Locale.US));
            if (parameter.getDisplayLevel() == newDisplayLevel) return;
            nodeAttributesDialog.setParameterDisplayLevel(parameter, newDisplayLevel);
        } else if (e.getSource() == addButton) {
            MenuItemDialog dialog = new MenuItemDialog((Dialog) SwingUtilities.getRoot(this));
            dialog.setVisible(true);
            if (dialog.isSuccessful()) {
                nodeAttributesDialog.addParameterMenuItem(parameter, dialog.getKey(), dialog.getLabel());
                menuItemsTable.tableChanged(new TableModelEvent(menuItemsTable.getModel()));
            }
        } else if (e.getSource() == removeButton) {
            Parameter.MenuItem item = parameter.getMenuItems().get(menuItemsTable.getSelectedRow());
            nodeAttributesDialog.removeParameterMenuItem(parameter, item);
        } else if (e.getSource() == upButton) {
            moveMenuItemUp();
        } else if (e.getSource() == downButton) {
            moveMenuItemDown();
        } else {
            throw new AssertionError("Unknown source " + e.getSource());
        }
        updateValues();
    }

    private HumanizedWidget getHumanizedWidget(Parameter.Widget widget) {
        for (HumanizedWidget humanizedWidget : humanizedWidgets) {
            if (humanizedWidget.getWidget() == widget) return humanizedWidget;
        }
        throw new AssertionError("Widget is not in humanized widget list.");
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

    private void moveMenuItemDown() {
        int index = menuItemsTable.getSelectedRow();
        // Return if nothing was selected.
        if (index == -1) return;
        java.util.List<Parameter.MenuItem> items = parameter.getMenuItems();
        // Return if the last item is selected.
        if (index >= items.size() - 1) return;
        nodeAttributesDialog.moveParameterMenuItemDown(parameter, index);
        // TODO: Changing the selection doesn't have any effect on Mac.
        menuItemsTable.changeSelection(index + 1, 1, false, false);
    }

    private void moveMenuItemUp() {
        int index = menuItemsTable.getSelectedRow();
        // Return if nothing was selected.
        if (index == -1) return;
        // Return if the first item is selected.
        if (index == 0) return;
        nodeAttributesDialog.moveParameterMenuItemUp(parameter, index);
        // TODO: Changing the selection doesn't have any effect on Mac.
        menuItemsTable.changeSelection(index - 1, 1, false, false);
    }
    
    private void updateMenuItem() {
        int index = menuItemsTable.getSelectedRow();
        if (index == -1) return;
        Parameter.MenuItem item = parameter.getMenuItems().get(index);
        MenuItemDialog dialog = new MenuItemDialog((Dialog) SwingUtilities.getRoot(this), item);
        dialog.setVisible(true);
        if (dialog.isSuccessful()) {
            nodeAttributesDialog.updateParameterMenuItem(parameter, index, dialog.getKey(), dialog.getLabel());
            updateValues();
        }
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

        private MenuItemDialog(Dialog dialog) {
            this(dialog, new Parameter.MenuItem("", ""));
        }

        private MenuItemDialog(Dialog dialog, Parameter.MenuItem item) {
            super(dialog, "Menu Item", true);
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
