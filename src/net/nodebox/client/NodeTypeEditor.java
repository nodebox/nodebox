package net.nodebox.client;

import net.nodebox.Icons;
import net.nodebox.node.Node;
import net.nodebox.node.Parameter;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class NodeTypeEditor extends JPanel implements ListSelectionListener, ActionListener {

    private Node node;

    private ParameterListModel parameterListModel;
    private Parameter selectedParameter = null;
    private JList parameterList;

    private JButton removeButton;

    private JPanel parameterPanel;
    private JTextField nameField;
    private JTextField labelField;
    private JTextField descriptionField;
    private JComboBox typeBox;
    private JTextField valueField;
    private JCheckBox nullAllowedCheck;
    private JComboBox boundingMethodBox;
    private JTextField minimumValueField;
    private JCheckBox minimumValueCheck;
    private JTextField maximumValueField;
    private JCheckBox maximumValueCheck;
    private JComboBox displayLevelBox;

    public NodeTypeEditor(Node node) {
        setLayout(new BorderLayout(0, 0));
        //library = new CoreNodeTypeLibrary("test", new Version(1, 0, 0));
        this.node = node;
        parameterListModel = new ParameterListModel(node);
        ParameterCellRenderer parameterCellRenderer = new ParameterCellRenderer();
        parameterList = new JList(parameterListModel);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 5));
        JButton addButton = new JButton(new Icons.PlusIcon());
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addParameter();
            }
        });
        removeButton = new JButton(new Icons.MinusIcon());
        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                removeSelectedParameter();
            }
        });

        JButton upButton = new JButton(new Icons.ArrowIcon(Icons.ArrowIcon.NORTH));
        upButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                moveUp();
            }
        });
        JButton downButton = new JButton(new Icons.ArrowIcon(Icons.ArrowIcon.SOUTH));
        downButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                moveDown();
            }
        });
        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(upButton);
        buttonPanel.add(downButton);
        parameterList.getSelectionModel().addListSelectionListener(this);
        parameterList.setCellRenderer(parameterCellRenderer);
        parameterList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel leftPanel = new JPanel(new BorderLayout(5, 0));
        leftPanel.add(parameterList, BorderLayout.CENTER);
        leftPanel.add(buttonPanel, BorderLayout.SOUTH);
        initParameterPanel();
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, parameterPanel);
        split.setDividerLocation(0.5);
        split.setResizeWeight(1.0);
        add(split, BorderLayout.CENTER);
        clearForm();
        setFormEnabled(false);
        if (node.getParameterCount() > 0)
            parameterList.setSelectedIndex(0);
    }

    private void addParameter() {
        String parameterName = JOptionPane.showInputDialog("Enter parameter name");
        if (parameterName != null) {
            Parameter parameter = node.addParameter(parameterName, Parameter.Type.FLOAT);
            reloadParameterList();
            parameterList.setSelectedValue(parameter, true);
        }
    }

    private void removeSelectedParameter() {
        if (selectedParameter == null) return;
        boolean success = node.removeParameter(selectedParameter.getName());
        System.out.println("success = " + success);
        reloadParameterList();
        if (node.getParameterCount() > 0) {
            parameterList.setSelectedIndex(0);
        } else {
            parameterList.setSelectedValue(null, false);
        }
    }

    private void moveDown() {
        if (selectedParameter == null) return;
        java.util.List<Parameter> parameterTypes = node.getParameters();
        int index = parameterTypes.indexOf(selectedParameter);
        assert (index >= 0);
        if (index >= parameterTypes.size() - 1) return;
        parameterTypes.remove(selectedParameter);
        parameterTypes.add(index + 1, selectedParameter);
        reloadParameterList();
        parameterList.setSelectedIndex(index + 1);
    }

    private void moveUp() {
        if (selectedParameter == null) return;
        java.util.List<Parameter> parameterTypes = node.getParameters();
        int index = parameterTypes.indexOf(selectedParameter);
        assert (index >= 0);
        if (index == 0) return;
        parameterTypes.remove(selectedParameter);
        parameterTypes.add(index - 1, selectedParameter);
        reloadParameterList();
        parameterList.setSelectedIndex(index - 1);
    }

    private void reloadParameterList() {
        parameterList.setModel(parameterListModel);
        parameterList.repaint();
    }

    public Node getNode() {
        return node;
    }

    public void initParameterPanel() {
        parameterPanel = new JPanel();
        parameterPanel.setLayout(new BoxLayout(parameterPanel, BoxLayout.Y_AXIS));
        JPanel contentPanel = new JPanel(new GridLayout(10, 2, 10, 5));
        contentPanel.add(new JLabel("Name"));
        nameField = new JTextField(20);
        nameField.setEditable(false);
        contentPanel.add(nameField);
        contentPanel.add(new JLabel("Label"));
        labelField = new JTextField(20);
        labelField.addActionListener(this);
        contentPanel.add(labelField);
        contentPanel.add(new JLabel("Description"));
        descriptionField = new JTextField(20);
        descriptionField.addActionListener(this);
        contentPanel.add(descriptionField);
        contentPanel.add(new JLabel("Type"));
        typeBox = new JComboBox(new String[]{"angle", "color", "file", "float", "font", "gradient", "image",
                "int", "menu", "seed", "string", "text", "toggle", "noderef", "grob_canvas", "grob_vector", "grob_image"});
        typeBox.addActionListener(this);
        contentPanel.add(typeBox);
        contentPanel.add(new JLabel("Default Value"));
        valueField = new JTextField(20);
        valueField.addActionListener(this);
        contentPanel.add(valueField);
        contentPanel.add(new JLabel(""));
        nullAllowedCheck = new JCheckBox("Null allowed");
        nullAllowedCheck.addActionListener(this);
        contentPanel.add(nullAllowedCheck);
        contentPanel.add(new JLabel("Bounding"));
        boundingMethodBox = new JComboBox(new String[]{"none", "soft", "hard"});
        boundingMethodBox.addActionListener(this);
        contentPanel.add(boundingMethodBox);
        contentPanel.add(new JLabel("Minimum"));
        minimumValueCheck = new JCheckBox();
        minimumValueCheck.addActionListener(this);
        minimumValueField = new JTextField(10);
        minimumValueField.addActionListener(this);
        JPanel minimumValuePanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 0));
        minimumValuePanel.add(minimumValueCheck);
        minimumValuePanel.add(minimumValueField);
        contentPanel.add(minimumValuePanel);
        contentPanel.add(new JLabel("Maximum"));
        maximumValueCheck = new JCheckBox();
        maximumValueCheck.addActionListener(this);
        maximumValueField = new JTextField(10);
        maximumValueField.addActionListener(this);
        JPanel maximumValuePanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 0));
        maximumValuePanel.add(maximumValueCheck);
        maximumValuePanel.add(maximumValueField);
        contentPanel.add(maximumValuePanel);
        contentPanel.add(new JLabel("Display Level"));
        displayLevelBox = new JComboBox(new String[]{"hud", "detail", "hidden"});
        displayLevelBox.addActionListener(this);
        contentPanel.add(displayLevelBox);
        parameterPanel.add(contentPanel);
        Dimension fillDimension = new Dimension(0, Integer.MAX_VALUE);
        parameterPanel.add(new Box.Filler(fillDimension, fillDimension, fillDimension));
    }

    public void setFormEnabled(boolean enabled) {
        labelField.setEnabled(enabled);
        descriptionField.setEnabled(enabled);
        typeBox.setEnabled(enabled);
        valueField.setEnabled(enabled);
        nullAllowedCheck.setEnabled(enabled);
        boundingMethodBox.setEnabled(enabled);
        minimumValueCheck.setEnabled(enabled);
        minimumValueField.setEnabled(enabled);
        maximumValueCheck.setEnabled(enabled);
        maximumValueField.setEnabled(enabled);
        displayLevelBox.setEnabled(enabled);
    }

    public void clearForm() {
        nameField.setText("");
        labelField.setText("");
        descriptionField.setText("");
        typeBox.setSelectedIndex(0);
        valueField.setText("");
        nullAllowedCheck.setSelected(false);
        boundingMethodBox.setSelectedIndex(0);
        minimumValueCheck.setSelected(false);
        minimumValueField.setText("");
        maximumValueCheck.setSelected(false);
        maximumValueField.setText("");
        displayLevelBox.setSelectedIndex(0);
    }

    public void valueChanged(ListSelectionEvent e) {
        if (selectedParameter == parameterList.getSelectedValue()) return;
        selectedParameter = (Parameter) parameterList.getSelectedValue();
        if (selectedParameter == null) {
            setFormEnabled(false);
            clearForm();
            removeButton.setEnabled(false);
        } else {
            setFormEnabled(true);
            removeButton.setEnabled(true);
            nameField.setText(selectedParameter.getName());
            labelField.setText(selectedParameter.getLabel());
            //descriptionField.setText(selectedParameter.getDescription());
            typeBox.setSelectedItem(selectedParameter.getType().toString().toLowerCase());
            valueField.setText(selectedParameter.getDefaultValue().toString());
            //nullAllowedCheck.setSelected(selectedParameter.isNullAllowed());
            Parameter.BoundingMethod boundingMethod = selectedParameter.getBoundingMethod();
            boundingMethodBox.setSelectedItem(boundingMethod.toString().toLowerCase());
            Object minimumValue = selectedParameter.getMinimumValue();
            String minimumValueString = minimumValue == null ? "" : minimumValue.toString();
            minimumValueCheck.setSelected(minimumValue != null);
            minimumValueField.setText(minimumValueString);
            minimumValueField.setEnabled(minimumValue != null);
            Object maximumValue = selectedParameter.getMaximumValue();
            String maximumValueString = maximumValue == null ? "" : maximumValue.toString();
            maximumValueCheck.setSelected(maximumValue != null);
            maximumValueField.setText(maximumValueString);
            maximumValueField.setEnabled(maximumValue != null);
            displayLevelBox.setSelectedItem(selectedParameter.getDisplayLevel().toString().toLowerCase());
        }
        parameterPanel.revalidate();
    }

    public void actionPerformed(ActionEvent e) {
        if (selectedParameter == null) return;
        if (e.getSource() == labelField) {
            selectedParameter.setLabel(labelField.getText());
            parameterList.setModel(parameterListModel);
            parameterList.revalidate();
            //} else if (e.getSource() == descriptionField) {
            //selectedParameter.setDescription(descriptionField.getText());
        } else if (e.getSource() == typeBox) {
            selectedParameter.setType(Parameter.Type.valueOf(typeBox.getSelectedItem().toString().toUpperCase()));
        } else if (e.getSource() == valueField) {
            selectedParameter.setValue(selectedParameter.parseValue(valueField.getText()));
            //} else if (e.getSource() == nullAllowedCheck) {
            //selectedParameter.setNullAllowed(nullAllowedCheck.isEnabled());
        } else if (e.getSource() == boundingMethodBox) {
            Parameter.BoundingMethod method = Parameter.BoundingMethod.valueOf(boundingMethodBox.getSelectedItem().toString().toUpperCase());
            selectedParameter.setBoundingMethod(method);
        } else if (e.getSource() == minimumValueCheck) {
            boolean checked = minimumValueCheck.isSelected();
            minimumValueField.setText(checked ? "0.0" : "");
            minimumValueField.setEnabled(checked);
        } else if (e.getSource() == maximumValueCheck) {
            boolean checked = maximumValueCheck.isSelected();
            maximumValueField.setText(checked ? "0.0" : "");
            maximumValueField.setEnabled(checked);
        } else if (e.getSource() == displayLevelBox) {
            Parameter.DisplayLevel displayLevel = Parameter.DisplayLevel.valueOf(displayLevelBox.getSelectedItem().toString().toUpperCase());
            selectedParameter.setDisplayLevel(displayLevel);
        } else {
            throw new AssertionError("Unknown source " + e.getSource());
        }
    }


    private class ParameterListModel implements ListModel {
        private java.util.List<Parameter> parameters;

        public ParameterListModel(Node node) {
            parameters = node.getParameters();
        }

        public int getSize() {
            return parameters.size();
        }

        public Object getElementAt(int index) {
            return parameters.get(index);
        }

        public void addListDataListener(ListDataListener l) {
            // Not implemented
        }

        public void removeListDataListener(ListDataListener l) {
            // Not implemented
        }
    }

    private class ParameterCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Parameter parameter = (Parameter) value;
            String displayValue = parameter.getLabel() + " (" + parameter.getName() + ")";
            return super.getListCellRendererComponent(list, displayValue, index, isSelected, cellHasFocus);
        }
    }

    public static void main(String[] args) {
        JFrame editorFrame = new JFrame();
        Node node = new NodeBoxDocument.AllControlsType().createInstance();
        editorFrame.getContentPane().add(new NodeTypeEditor(node));
        editorFrame.setSize(800, 800);
        editorFrame.setLocationByPlatform(true);
        editorFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        editorFrame.setVisible(true);
    }
}
