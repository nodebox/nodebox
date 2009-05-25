package net.nodebox.client;

import net.nodebox.client.parameter.*;
import net.nodebox.node.*;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ParameterView extends JComponent {

    private static Logger logger = Logger.getLogger("net.nodebox.client.ParameterView");

    private static final Map<Parameter.Widget, Class> CONTROL_MAP;
    private JPanel controlPanel;

    static {
        CONTROL_MAP = new HashMap<Parameter.Widget, Class>();
        CONTROL_MAP.put(Parameter.Widget.ANGLE, FloatControl.class);
        CONTROL_MAP.put(Parameter.Widget.COLOR, ColorControl.class);
        CONTROL_MAP.put(Parameter.Widget.FILE, FileControl.class);
        CONTROL_MAP.put(Parameter.Widget.FLOAT, FloatControl.class);
        CONTROL_MAP.put(Parameter.Widget.FONT, FontControl.class);
        CONTROL_MAP.put(Parameter.Widget.GRADIENT, null);
        CONTROL_MAP.put(Parameter.Widget.IMAGE, ImageControl.class);
        CONTROL_MAP.put(Parameter.Widget.INT, IntControl.class);
        CONTROL_MAP.put(Parameter.Widget.MENU, MenuControl.class);
        CONTROL_MAP.put(Parameter.Widget.SEED, IntControl.class);
        CONTROL_MAP.put(Parameter.Widget.STRING, StringControl.class);
        CONTROL_MAP.put(Parameter.Widget.TEXT, TextControl.class);
        CONTROL_MAP.put(Parameter.Widget.TOGGLE, ToggleControl.class);
        CONTROL_MAP.put(Parameter.Widget.NODEREF, NoderefControl.class);
    }

    private Node node;
    private NodeEventHandler handler = new NodeEventHandler();

    public ParameterView() {
        setLayout(new BorderLayout());
        controlPanel = new JPanel(new GridBagLayout());
        controlPanel.setBackground(Theme.getInstance().getParameterViewBackgroundColor());
        JScrollPane scrollPane = new JScrollPane(controlPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, BorderLayout.CENTER);
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        Node oldNode = this.node;
        if (oldNode != null)
            oldNode.removeNodeAttributeListener(handler);
        this.node = node;
        if (node != null)
            node.addNodeAttributeListener(handler);
        rebuildInterface();
        validate();
        repaint();
    }

    private void rebuildInterface() {
        controlPanel.removeAll();
        if (node == null) return;
        int rowindex = 0;
        for (Parameter p : node.getParameters()) {
            Class widgetClass = CONTROL_MAP.get(p.getWidget());
            JComponent control;
            if (widgetClass != null) {
                control = (JComponent) constructControl(widgetClass, p);

            } else {
                control = new JLabel("  ");
            }
            ParameterRow parameterRow = new ParameterRow(p, control);
            GridBagConstraints rowConstraints = new GridBagConstraints();
            rowConstraints.gridx = 0;
            rowConstraints.gridy = rowindex;
            rowConstraints.fill = GridBagConstraints.HORIZONTAL;
            rowConstraints.weightx = 1.0;
            controlPanel.add(parameterRow, rowConstraints);
            rowindex++;
        }

        JLabel filler = new JLabel();
        GridBagConstraints fillerConstraints = new GridBagConstraints();
        fillerConstraints.gridx = 0;
        fillerConstraints.gridy = rowindex;
        fillerConstraints.fill = GridBagConstraints.BOTH;
        fillerConstraints.weighty = 1.0;
        fillerConstraints.gridwidth = GridBagConstraints.REMAINDER;
        controlPanel.add(filler, fillerConstraints);
        revalidate();
    }

    private ParameterControl constructControl(Class controlClass, Parameter p) {
        try {
            Constructor constructor = controlClass.getConstructor(Parameter.class);
            return (ParameterControl) constructor.newInstance(p);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Cannot construct control", e);
            throw new AssertionError("Cannot construct control:" + e);
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        NodeLibraryManager manager = new NodeLibraryManager();
        NodeLibrary testLibrary = new NodeLibrary("test");
        Node n = manager.getNode("corevector.rect").newInstance(testLibrary, "myrect");
        ParameterView p = new ParameterView();
        p.setNode(n);
        frame.setContentPane(p);
        frame.setSize(500, 500);
        frame.setVisible(true);
    }

    //// Node events ////

    private class NodeEventHandler implements NodeAttributeListener {
        public void attributeChanged(Node source) {
            if (source == getNode()) {
                rebuildInterface();
            }
        }
    }


}
