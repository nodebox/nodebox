package nodebox.client;

import com.google.common.collect.ImmutableList;
import nodebox.Icons;
import nodebox.node.MenuItem;
import nodebox.node.Port;
import nodebox.ui.Theme;
import nodebox.util.HumanizedObject;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;

public class PortAttributesEditor extends JPanel implements ActionListener, FocusListener {

    // TODO: Don't update immediately, use save/cancel buttons.

    private static Map<String, HumanizedObject[]> humanizedWidgetsMap;
    private static HumanizedObject[] humanizedRanges;

    static {
        humanizedWidgetsMap = new HashMap<>();
        for (String key : Port.WIDGET_MAPPING.keySet()) {
            ImmutableList<Port.Widget> widgets = Port.WIDGET_MAPPING.get(key);
            HumanizedObject[] humanizedWidgets = new HumanizedObject[widgets.size()];
            for (int i = 0; i < widgets.size(); i++) {
                humanizedWidgets[i] = new HumanizedObject(widgets.get(i));
            }
            humanizedWidgetsMap.put(key, humanizedWidgets);
        }
        humanizedRanges = new HumanizedObject[Port.Range.values().length];
        for (int i = 0; i < Port.Range.values().length; i++) {
            humanizedRanges[i] = new HumanizedObject(Port.Range.values()[i]);
        }
    }

    private JTextField nameField;
    private JTextField labelField;
    private JTextField typeField;
    private JTextField descriptionField;
    private JComboBox<HumanizedObject> widgetBox;
    private JComboBox<HumanizedObject> rangeBox;
    private JTextField valueField;
    private JTextField minimumValueField;
    private JCheckBox minimumValueCheck;
    private JTextField maximumValueField;
    private JCheckBox maximumValueCheck;
    private JTable menuItemsTable;
    private JButton addButton;
    private JButton removeButton;
    private JButton upButton;
    private JButton downButton;
    private int y = 0;

    private boolean focusLostEvents = true;

    private NodeAttributesDialog nodeAttributesDialog;
    private String portName;

    public PortAttributesEditor(NodeAttributesDialog dialog, String portName) {
        this.nodeAttributesDialog = dialog;
        this.portName = portName;
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
        nameField.setEnabled(false);
        addRow("Name", nameField);

        // Label
        labelField = new JTextField(20);
        labelField.addActionListener(this);
        addRow("Label", labelField);

        // Type
        typeField = new JTextField(20);
        typeField.setEnabled(false);
        addRow("Type", typeField);

        // Description
        descriptionField = new JTextField(20);
        descriptionField.addActionListener(this);
        addRow("Description", descriptionField);

        // Widget
        if (getPort().isStandardType()) {
            widgetBox = new JComboBox<>(humanizedWidgetsMap.get(getPort().getType()));
            widgetBox.addActionListener(this);
            addRow("Widget", widgetBox);
        }

        rangeBox = new JComboBox<>(humanizedRanges);
        rangeBox.addActionListener(this);
        addRow("Range", rangeBox);

        // Value
        valueField = new JTextField(20);
        valueField.addActionListener(this);
        valueField.addFocusListener(this);
        addRow("Value", valueField);

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

    private Port getPort() {
        return nodeAttributesDialog.getNode().getInput(portName);
    }

    public void updateValues() {
        Port port = getPort();
        nameField.setText(port.getName());
        labelField.setText(port.getDisplayLabel());
        typeField.setText(port.getType());
        descriptionField.setText(port.getDescription());
        rangeBox.setSelectedItem(getHumanizedRange(port.getRange()));
        if (port.isStandardType()) {
            widgetBox.setSelectedItem(getHumanizedWidget(port.getWidget()));
            valueField.setText(port.getValue().toString());
        } else
            valueField.setEnabled(false);
        Object minimumValue = port.getMinimumValue();
        String minimumValueString = minimumValue == null ? "" : minimumValue.toString();
        minimumValueCheck.setSelected(minimumValue != null);
        minimumValueField.setText(minimumValueString);
        minimumValueField.setEnabled(minimumValue != null);
        Object maximumValue = port.getMaximumValue();
        String maximumValueString = maximumValue == null ? "" : maximumValue.toString();
        maximumValueCheck.setSelected(maximumValue != null);
        maximumValueField.setText(maximumValueString);
        maximumValueField.setEnabled(maximumValue != null);
        menuItemsTable.tableChanged(new TableModelEvent(menuItemsTable.getModel()));
        revalidate();
    }

    public void actionPerformed(ActionEvent e) {
        Port port = getPort();
        if (e.getSource() == labelField) {
            if (labelField.getText().equals(port.getDisplayLabel())) return;
            nodeAttributesDialog.setPortLabel(portName, labelField.getText());
        } else if (e.getSource() == descriptionField) {
            nodeAttributesDialog.setPortDescription(portName, descriptionField.getText());
        } else if (e.getSource() == widgetBox) {
            HumanizedObject newWidget = (HumanizedObject) widgetBox.getSelectedItem();
            if (port.getWidget() == newWidget.getObject()) return;
            nodeAttributesDialog.setPortWidget(portName, (Port.Widget) newWidget.getObject());
        } else if (e.getSource() == rangeBox) {
            HumanizedObject newRange = (HumanizedObject) rangeBox.getSelectedItem();
            if (port.getRange() == newRange.getObject()) return;
            nodeAttributesDialog.setPortRange(portName, (Port.Range) newRange.getObject());
        } else if (e.getSource() == valueField) {
            String newValue = valueField.getText();
            if (port.getValue() != null && port.getValue().toString().equals(newValue)) return;
            try {
                nodeAttributesDialog.setPortValue(portName, Port.parseValue(port.getType(), valueField.getText()));
            } catch (IllegalArgumentException e1) {
                showError("Value " + valueField.getText() + " is invalid: " + e1.getMessage());
            }
        } else if (e.getSource() == minimumValueCheck) {
            if (minimumValueCheck.isSelected() && port.getMinimumValue() != null) return;
            nodeAttributesDialog.setPortMinimumValue(portName, minimumValueCheck.isSelected() ? (double) 0f : null);
        } else if (e.getSource() == minimumValueField) {
            try {
                float v = Float.parseFloat(minimumValueField.getText());
                if (v == port.getMinimumValue()) return;
                nodeAttributesDialog.setPortMinimumValue(portName, (double) v);
            } catch (Exception e1) {
                showError("Value " + minimumValueField.getText() + " is invalid: " + e1.getMessage());
            }
        } else if (e.getSource() == maximumValueCheck) {
            if (maximumValueCheck.isSelected() && port.getMaximumValue() != null) return;
            nodeAttributesDialog.setPortMaximumValue(portName, maximumValueCheck.isSelected() ? (double) 0f : null);
        } else if (e.getSource() == maximumValueField) {
            try {
                float v = Float.parseFloat(maximumValueField.getText());
                if (v == port.getMaximumValue()) return;
                nodeAttributesDialog.setPortMaximumValue(portName, (double) v);
            } catch (Exception e1) {
                showError("Value " + maximumValueField.getText() + " is invalid: " + e1.getMessage());
            }
        } else if (e.getSource() == addButton) {
            MenuItemDialog dialog = new MenuItemDialog((Dialog) SwingUtilities.getRoot(this));
            dialog.setVisible(true);
            if (dialog.isSuccessful()) {
                nodeAttributesDialog.addPortMenuItem(portName, dialog.getKey(), dialog.getLabel());
                menuItemsTable.tableChanged(new TableModelEvent(menuItemsTable.getModel()));
            }
        } else if (e.getSource() == removeButton) {
            MenuItem item = port.getMenuItems().get(menuItemsTable.getSelectedRow());
            nodeAttributesDialog.removePortMenuItem(portName, item);
        } else if (e.getSource() == upButton) {
            moveMenuItemUp();
        } else if (e.getSource() == downButton) {
            moveMenuItemDown();
        } else {
            throw new AssertionError("Unknown source " + e.getSource());
        }
        updateValues();
    }

    private HumanizedObject getHumanizedWidget(Port.Widget widget) {
        for (HumanizedObject humanizedWidget : humanizedWidgetsMap.get(getPort().getType())) {
            if (humanizedWidget.getObject() == widget) return humanizedWidget;
        }
        throw new AssertionError("Widget is not in humanized widget list.");
    }

    private HumanizedObject getHumanizedRange(Port.Range range) {
        for (HumanizedObject humanizedRange : humanizedRanges) {
            if (humanizedRange.getObject() == range) return humanizedRange;
        }
        throw new AssertionError("Range is not in humanized range list.");
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
        java.util.List<MenuItem> items = getPort().getMenuItems();
        // Return if the last item is selected.
        if (index >= items.size() - 1) return;
        nodeAttributesDialog.movePortMenuItemDown(portName, index);
        // TODO: Changing the selection doesn't have any effect on Mac.
        menuItemsTable.changeSelection(index + 1, 1, false, false);
    }

    private void moveMenuItemUp() {
        int index = menuItemsTable.getSelectedRow();
        // Return if nothing was selected.
        if (index == -1) return;
        // Return if the first item is selected.
        if (index == 0) return;
        nodeAttributesDialog.movePortMenuItemUp(portName, index);
        // TODO: Changing the selection doesn't have any effect on Mac.
        menuItemsTable.changeSelection(index - 1, 1, false, false);
    }

    private void updateMenuItem() {
        int index = menuItemsTable.getSelectedRow();
        if (index == -1) return;
        MenuItem item = getPort().getMenuItems().get(index);
        MenuItemDialog dialog = new MenuItemDialog((Dialog) SwingUtilities.getRoot(this), item);
        dialog.setVisible(true);
        if (dialog.isSuccessful()) {
            nodeAttributesDialog.updatePortMenuItem(portName, index, dialog.getKey(), dialog.getLabel());
            updateValues();
        }
    }

    private class MenuItemsModel extends AbstractTableModel {
        public int getRowCount() {
            return getPort().getMenuItems().size();
        }

        public int getColumnCount() {
            return 2;
        }

        public Object getValueAt(int row, int column) {
            MenuItem item = getPort().getMenuItems().get(row);
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
            this(dialog, new MenuItem("", ""));
        }

        private MenuItemDialog(Dialog dialog, MenuItem item) {
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
                @Override
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
