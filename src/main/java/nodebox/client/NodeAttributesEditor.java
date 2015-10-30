package nodebox.client;

import nodebox.Icons;
import nodebox.node.Node;
import nodebox.node.Port;
import nodebox.ui.SingleLineSplitPane;
import nodebox.ui.SwingUtils;
import nodebox.ui.Theme;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class NodeAttributesEditor extends JPanel implements ListSelectionListener {

    private static final String NODE_SETTINGS = "Settings";

    private NodeAttributesDialog dialog;

    private Port selectedPort = null;
    private PortList portList;
    private JPanel editorPanel;

    private JButton removeButton;
    private JPanel leftPanel;

    public NodeAttributesEditor(NodeAttributesDialog dialog) {
        setLayout(new BorderLayout(0, 0));
        this.dialog = dialog;

        leftPanel = new JPanel(new BorderLayout(5, 0));

        portList = new PortList();
        reloadPortList();
        //portList.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 5));
        JButton addButton = new JButton(new Icons.PlusIcon());
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addPort();
            }
        });
        removeButton = new JButton(new Icons.MinusIcon());
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                removeSelectedPort();
            }
        });

        JButton upButton = new JButton(new Icons.ArrowIcon(Icons.ArrowIcon.NORTH));
        upButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveUp();
            }
        });
        JButton downButton = new JButton(new Icons.ArrowIcon(Icons.ArrowIcon.SOUTH));
        downButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveDown();
            }
        });
        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        //buttonPanel.add(upButton);
        //buttonPanel.add(downButton);

        //portList.getSelectionModel().addListSelectionListener(this);
        //portList.setCellRenderer(parameterCellRenderer);
        //portList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        leftPanel.add(portList, BorderLayout.CENTER);
        leftPanel.add(buttonPanel, BorderLayout.SOUTH);

        editorPanel = new JPanel(new BorderLayout());
        editorPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        JSplitPane split = new SingleLineSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, editorPanel);
        split.setDividerLocation(150);
        split.setResizeWeight(0.25);
        add(split, BorderLayout.CENTER);
        if (getNode().getInputs().size() > 0)
            portList.setSelectedIndex(0);
    }

    private void reloadPortList() {
        portList.removeAll();
        // Add the Node metadata.

        portList.addHeader("NODE");
        portList.addNodeSettings();
        portList.addHeader("PORTS");
        for (Port p : getNode().getInputs()) {
            portList.addPort(p);
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
    }

    private void portSelected(Port port) {
        editorPanel.removeAll();
        PortAttributesEditor editor = new PortAttributesEditor(dialog, port.getName());
        editorPanel.add(editor, BorderLayout.CENTER);
        editorPanel.revalidate();
        selectedPort = port;
    }

    private void addPort() {
        NewPortDialog d = new NewPortDialog();
        d.setVisible(true);

        String portName = d.portName;
        if (portName != null) {
            dialog.addPort(getNode(), portName, d.portType);
            reloadPortList();
            portList.setSelectedValue(getNode().getInput(portName), true);
        }
    }

    private void removeSelectedPort() {
        if (selectedPort == null) return;
        dialog.removePort(getNode(), selectedPort.getName());
        reloadPortList();
        editorPanel.removeAll();
        editorPanel.revalidate();
        editorPanel.repaint();
        selectedPort = null;
    }

    private void moveDown() {
    }

    private void moveUp() {
    }

    public Node getNode() {
        return dialog.getNode();
    }

    public void valueChanged(ListSelectionEvent e) {
        if (selectedPort == portList.getSelectedValue()) return;
        selectedPort = (Port) portList.getSelectedValue();
        if (selectedPort == null) {
            removeButton.setEnabled(false);
        } else {
            removeButton.setEnabled(true);
        }
        //parameterPanel.revalidate();
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

    private class PortList extends JPanel {

        private SourceLabel selectedLabel;
        private Map<Object, SourceLabel> labelMap = new HashMap<>();

        private PortList() {
            super(null);
            Dimension d = new Dimension(140, 500);
            setBackground(Theme.NODE_ATTRIBUTES_PARAMETER_LIST_BACKGROUND_COLOR);
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
                } else if (label.source instanceof Port)
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
        private String portType = null;

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

            // type
            JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
            typePanel.add(new JLabel("Type:  "));
            String[] types = new String[Port.STANDARD_TYPES.size() + 1];
            Port.STANDARD_TYPES.toArray(types);
            types[types.length - 1] = "custom";
            final JComboBox<String> box = new JComboBox<>(types);
            typePanel.add(box);

            // custom type
            JPanel customTypePanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
            customTypePanel.add(new JLabel("Custom Type:  "));
            final JTextField customTypeField = new JTextField("", 20);
            customTypeField.setEnabled(false);
            customTypePanel.add(customTypeField);

            box.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    customTypeField.setEnabled(box.getSelectedItem().equals("custom"));
                }
            });

            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
            buttonPanel.add(Box.createHorizontalGlue());

            JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dispose();
                }
            });
            buttonPanel.add(cancelButton);
            JButton okButton = new JButton("OK");
            okButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    portName = nameField.getText();
                    String selectedType = (String) box.getSelectedItem();
                    portType = selectedType.equals("custom") ? customTypeField.getText().toLowerCase(Locale.US) : selectedType;
                    dispose();
                }
            });
            buttonPanel.add(okButton);

            mainPanel.add(namePanel);
            mainPanel.add(Box.createVerticalStrut(10));
            mainPanel.add(typePanel);
            mainPanel.add(Box.createVerticalStrut(10));
            mainPanel.add(customTypePanel);
            mainPanel.add(Box.createVerticalStrut(10));
            mainPanel.add(buttonPanel);
            pack();
            getRootPane().setDefaultButton(okButton);
            KeyStroke escapeStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
            getRootPane().registerKeyboardAction(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dispose();
                }
            }, escapeStroke, JComponent.WHEN_IN_FOCUSED_WINDOW);

            SwingUtils.centerOnScreen(this);
        }
    }

    /*private static class AllControlsType extends Builtin {
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
    } */

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
