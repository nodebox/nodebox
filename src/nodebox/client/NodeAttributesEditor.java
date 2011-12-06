package nodebox.client;

import nodebox.Icons;
import nodebox.node.*;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

public class NodeAttributesEditor extends JPanel implements ListSelectionListener {

    private static final String NODE_SETTINGS = "Settings";

    private NodeAttributesDialog dialog;
    private Node node;

    private ParameterListModel parameterListModel;
    private Parameter selectedParameter = null;
    private Port selectedPort = null;
    private ParameterList parameterList;
    private JPanel editorPanel;

    private JButton removeButton;
    private JButton addButton;
    private JPanel leftPanel;

    public NodeAttributesEditor(NodeAttributesDialog dialog) {
        setLayout(new BorderLayout(0, 0));
        //library = new CoreNodeTypeLibrary("test", new Version(1, 0, 0));
        this.dialog = dialog;
        node = dialog.getNode();

        leftPanel = new JPanel(new BorderLayout(5, 0));

        parameterListModel = new ParameterListModel(node);
        ParameterCellRenderer parameterCellRenderer = new ParameterCellRenderer();
        parameterList = new ParameterList();
        reloadParameterList();
        //parameterList.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 5));
        addButton = new JButton(new Icons.PlusIcon());
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addEvent();
            }
        });
        removeButton = new JButton(new Icons.MinusIcon());
        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                removeEvent();
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

        //parameterList.getSelectionModel().addListSelectionListener(this);
        //parameterList.setCellRenderer(parameterCellRenderer);
        //parameterList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        leftPanel.add(parameterList, BorderLayout.CENTER);
        leftPanel.add(buttonPanel, BorderLayout.SOUTH);

        editorPanel = new JPanel(new BorderLayout());
        editorPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        JSplitPane split = new SingleLineSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, editorPanel);
        split.setDividerLocation(150);
        split.setResizeWeight(0.25);
        add(split, BorderLayout.CENTER);
        if (node.getParameterCount() > 0)
            parameterList.setSelectedIndex(0);
    }

    private void reloadParameterList() {
        parameterList.removeAll();
        // Add the Node metadata.

        parameterList.addHeader("NODE");
        parameterList.addNodeSettings();
        parameterList.addHeader("PORTS");
        for (Port p : node.getPorts()) {
            parameterList.addPort(p);
        }
        parameterList.addHeader("PARAMETERS");
        for (Parameter p : node.getParameters()) {
            parameterList.addParameter(p);
        }
        revalidate();
        leftPanel.repaint();
    }

    private void settingsSelected() {
        editorPanel.removeAll();
        NodeSettingsEditor editor = new NodeSettingsEditor(dialog);
        editorPanel.add(editor, BorderLayout.CENTER);
        editorPanel.revalidate();
        selectedPort = null;
        selectedParameter = null;
    }

    private void portSelected(Port port) {
        editorPanel.removeAll();
        PortAttributesEditor editor = new PortAttributesEditor(dialog, port);
        editorPanel.add(editor, BorderLayout.CENTER);
        editorPanel.revalidate();
        selectedPort = port;
        selectedParameter = null;
    }

    private void parameterSelected(Parameter parameter) {
        editorPanel.removeAll();
        ParameterAttributesEditor editor = new ParameterAttributesEditor(dialog, parameter);
        editorPanel.add(editor, BorderLayout.CENTER);
        editorPanel.revalidate();
        selectedParameter = parameter;
        selectedPort = null;
    }

    private void addEvent() {
        JMenuItem item;
        JPopupMenu menu = new JPopupMenu();
        item = menu.add("Parameter");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addParameter();
            }
        });
        item = menu.add("Port");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addPort();
            }
        });
        menu.show(addButton, 0, addButton.getHeight());
    }

    private void addParameter() {
        String parameterName = JOptionPane.showInputDialog("Enter parameter name");
        if (parameterName != null) {
            dialog.addParameter(node, parameterName);
            reloadParameterList();
            parameterList.setSelectedValue(node.getParameter(parameterName), true);
        }
    }

    private void addPort() {
        NewPortDialog d = new NewPortDialog();
        d.setVisible(true);

        String portName = d.portName;
        if (portName != null) {
            dialog.addPort(node, portName, d.portCardinality);
            reloadParameterList();
            parameterList.setSelectedValue(node.getPort(portName), true);
        }
    }

    private void removeEvent() {
        if (selectedParameter != null) {
            removeSelectedParameter();
        } else if (selectedPort != null) {
            removeSelectedPort();
        }
    }

    private void removeSelectedPort() {
        JOptionPane.showMessageDialog(this, "Sorry, removing ports is not implemented yet.");
    }

    private void removeSelectedParameter() {
        if (selectedParameter == null) return;
        dialog.removeParameter(node, selectedParameter.getName());
        reloadParameterList();
        editorPanel.removeAll();
        editorPanel.revalidate();
        editorPanel.repaint();
        selectedParameter = null;
    }

    private void moveDown() {
        if (selectedParameter == null) return;
        java.util.List<Parameter> parameters = node.getParameters();
        int index = parameters.indexOf(selectedParameter);
        assert (index >= 0);
        if (index >= parameters.size() - 1) return;
        parameters.remove(selectedParameter);
        parameters.add(index + 1, selectedParameter);
        reloadParameterList();
        parameterList.setSelectedIndex(index + 1);
    }

    private void moveUp() {
        if (selectedParameter == null) return;
        java.util.List<Parameter> parameters = node.getParameters();
        int index = parameters.indexOf(selectedParameter);
        assert (index >= 0);
        if (index == 0) return;
        parameters.remove(selectedParameter);
        parameters.add(index - 1, selectedParameter);
        reloadParameterList();
        parameterList.setSelectedIndex(index - 1);
    }

    public Node getNode() {
        return node;
    }

    public void valueChanged(ListSelectionEvent e) {
        if (selectedParameter == parameterList.getSelectedValue()) return;
        selectedParameter = (Parameter) parameterList.getSelectedValue();
        if (selectedParameter == null) {
            removeButton.setEnabled(false);
        } else {
            removeButton.setEnabled(true);
        }
        //parameterPanel.revalidate();
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

    private class SourceLabel extends JComponent {

        private String text;
        private Object source;
        private boolean selected;

        private SourceLabel(String text, Object source) {
            this.text = text;
            this.source = source;
            setMinimumSize(new Dimension(100, 25));
            setMaximumSize(new Dimension(500, 25));
            setPreferredSize(new Dimension(140, 25));
            setAlignmentX(JComponent.LEFT_ALIGNMENT);
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
            repaint();
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            if (selected) {
                Rectangle clip = g2.getClipBounds();
                g2.setColor(Theme.NODE_ATTRIBUTES_PARAMETER_COLOR);
                g2.fillRect(clip.x, clip.y, clip.width, clip.height);
            }
            g2.setFont(Theme.SMALL_FONT);
            if (selected) {
                g2.setColor(Color.WHITE);
            } else {
                g2.setColor(Color.BLACK);
            }
            g2.drawString(text, 15, 18);
        }
    }

    private class ParameterList extends JPanel {

        private SourceLabel selectedLabel;
        private Map<Object, SourceLabel> labelMap = new HashMap<Object, SourceLabel>();

        private ParameterList() {
            super(null);
            Dimension d = new Dimension(140, 500);
            setBackground(Theme.NODE_ATTRIBUTES_PARAMETER_LIST_BACGKGROUND_COLOR);
            setBorder(null);
            setOpaque(true);
            setPreferredSize(d);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        }

        public void addNodeSettings() {
            final SourceLabel label = new SourceLabel(NODE_SETTINGS, NODE_SETTINGS);
            label.addMouseListener(new MouseInputAdapter() {
                public void mouseClicked(MouseEvent e) {
                    setSelectedLabel(label);
                }
            });
            labelMap.put(NODE_SETTINGS, label);
            add(label);
        }

        public void addPort(final Port p) {
            final SourceLabel label = new SourceLabel(p.getName(), p);
            label.addMouseListener(new MouseInputAdapter() {
                public void mouseClicked(MouseEvent e) {
                    setSelectedLabel(label);
                }
            });
            labelMap.put(p, label);
            add(label);
        }

        public void addParameter(final Parameter p) {
            final SourceLabel label = new SourceLabel(p.getName(), p);
            label.addMouseListener(new MouseInputAdapter() {
                public void mouseClicked(MouseEvent e) {
                    setSelectedLabel(label);
                }
            });
            labelMap.put(p, label);
            add(label);
        }

        /**
         * Add a header label that cannot be selected.
         *
         * @param s the name of the header.
         */
        public void addHeader(String s) {
            JLabel header = new JLabel(s);
            header.setForeground(Theme.TEXT_HEADER_COLOR);
            header.setFont(Theme.SMALL_BOLD_FONT);
            header.setMinimumSize(new Dimension(100, 25));
            header.setMaximumSize(new Dimension(500, 25));
            header.setPreferredSize(new Dimension(140, 25));
            header.setAlignmentX(JComponent.LEFT_ALIGNMENT);
            header.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
            add(header);
        }

        public void setSelectedLabel(SourceLabel label) {
            if (selectedLabel != null)
                selectedLabel.setSelected(false);
            selectedLabel = label;
            if (selectedLabel != null) {
                selectedLabel.setSelected(true);
                if (label.source.equals(NODE_SETTINGS)) {
                    settingsSelected();
                } else if (label.source instanceof Parameter)
                    parameterSelected((Parameter) label.source);
                else if (label.source instanceof Port)
                    portSelected((Port) label.source);
                else
                    throw new AssertionError("Unknown label source " + label.source);
            }
        }

        public void setSelectedIndex(int i) {
            // TODO: Implement
        }

        public void setSelectedValue(Object value, boolean shouldScroll) {
            SourceLabel label = labelMap.get(value);
            assert label != null;
            setSelectedLabel(label);
        }

        public Object getSelectedValue() {
            return null;
        }
    }

    private class NewPortDialog extends JDialog {
        private String portName = null;
        private Port.Cardinality portCardinality = Port.Cardinality.SINGLE;

        public NewPortDialog() {
            setTitle("Add new port");
            setModal(true);
            setResizable(false);

            // Main
            setLayout(new BorderLayout(5, 5));

            JPanel mainPanel = new JPanel();
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
            mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            add(mainPanel, BorderLayout.CENTER);

            // name
            JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
            namePanel.add(new JLabel("Port name:  "));
            final JTextField nameField = new JTextField("", 20);
            namePanel.add(nameField);

            // cardinality
            JPanel cardinalityPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
            cardinalityPanel.add(new JLabel("Cardinality:  "));
            final JComboBox box = new JComboBox(Port.Cardinality.values());
            cardinalityPanel.add(box);

            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
            buttonPanel.add(Box.createHorizontalGlue());

            JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    dispose();
                }
            });
            buttonPanel.add(cancelButton);
            JButton okButton = new JButton("OK");
            okButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    portName = nameField.getText();
                    portCardinality = (Port.Cardinality) box.getSelectedItem();
                    dispose();
                }
            });
            buttonPanel.add(okButton);

            mainPanel.add(namePanel);
            mainPanel.add(Box.createVerticalStrut(10));
            mainPanel.add(cardinalityPanel);
            mainPanel.add(Box.createVerticalStrut(10));
            mainPanel.add(buttonPanel);
            pack();
            getRootPane().setDefaultButton(okButton);
            KeyStroke escapeStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
            getRootPane().registerKeyboardAction(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    dispose();
                }
            }, escapeStroke, JComponent.WHEN_IN_FOCUSED_WINDOW);

            SwingUtils.centerOnScreen(this);
        }
    }

    private static class AllControlsType extends Builtin {
        protected Node createInstance() {
            NodeLibrary library = new NodeLibrary("allcontrols");
            Node n = Node.ROOT_NODE.newInstance(library, "allcontrols", Canvas.class);
            n.addParameter("angle", Parameter.Type.FLOAT).setWidget(Parameter.Widget.ANGLE);
            n.addParameter("color", Parameter.Type.COLOR).setWidget(Parameter.Widget.COLOR);
            n.addParameter("file", Parameter.Type.STRING).setWidget(Parameter.Widget.FILE);
            n.addParameter("float", Parameter.Type.FLOAT).setWidget(Parameter.Widget.FLOAT);
            n.addParameter("font", Parameter.Type.STRING).setWidget(Parameter.Widget.FONT);
            n.addParameter("gradient", Parameter.Type.COLOR).setWidget(Parameter.Widget.GRADIENT);
            n.addParameter("image", Parameter.Type.STRING).setWidget(Parameter.Widget.IMAGE);
            n.addParameter("int", Parameter.Type.INT).setWidget(Parameter.Widget.INT);
            n.addParameter("menu", Parameter.Type.STRING).setWidget(Parameter.Widget.MENU);
            n.addParameter("seed", Parameter.Type.INT).setWidget(Parameter.Widget.SEED);
            n.addParameter("string", Parameter.Type.STRING).setWidget(Parameter.Widget.STRING);
            n.addParameter("text", Parameter.Type.STRING).setWidget(Parameter.Widget.TEXT);
            n.addParameter("toggle", Parameter.Type.INT).setWidget(Parameter.Widget.TOGGLE);
            n.addParameter("noderef", Parameter.Type.STRING).setWidget(Parameter.Widget.NODEREF);
            Parameter pMenu = n.getParameter("menu");
            pMenu.addMenuItem("red", "Red");
            pMenu.addMenuItem("green", "Green");
            pMenu.addMenuItem("blue", "Blue");
            pMenu.setValue("blue");
            return n;
        }

        @Override
        public Object cook(Node node, ProcessingContext context) {
            return null;
        }
    }

/*    public static void main(String[] args) {
        JFrame editorFrame = new JFrame();
        Node node = new AllControlsType().createInstance();
        node.addPort("shape");
        editorFrame.getContentPane().add(new NodeAttributesEditor(node));
        editorFrame.setSize(580, 710);
        editorFrame.setResizable(false);
        editorFrame.setLocationByPlatform(true);
        editorFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        editorFrame.setVisible(true);
    }  */
}
