package net.nodebox.client;

import net.nodebox.client.parameter.*;
import net.nodebox.node.*;
import net.nodebox.node.vector.RectType;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ParameterView extends JComponent {

    private static Logger logger = Logger.getLogger("net.nodebox.client.ParameterView");

    private static final Map<ParameterType.Type, Class> CONTROL_MAP;
    private JPanel controlPanel;

    static {
        CONTROL_MAP = new HashMap<ParameterType.Type, Class>();
        CONTROL_MAP.put(ParameterType.Type.ANGLE, FloatControl.class);
        CONTROL_MAP.put(ParameterType.Type.COLOR, ColorControl.class);
        CONTROL_MAP.put(ParameterType.Type.FILE, FileControl.class);
        CONTROL_MAP.put(ParameterType.Type.FLOAT, FloatControl.class);
        CONTROL_MAP.put(ParameterType.Type.FONT, FontControl.class);
        CONTROL_MAP.put(ParameterType.Type.GRADIENT, null);
        CONTROL_MAP.put(ParameterType.Type.IMAGE, ImageControl.class);
        CONTROL_MAP.put(ParameterType.Type.INT, IntControl.class);
        CONTROL_MAP.put(ParameterType.Type.MENU, MenuControl.class);
        CONTROL_MAP.put(ParameterType.Type.SEED, IntControl.class);
        CONTROL_MAP.put(ParameterType.Type.STRING, StringControl.class);
        CONTROL_MAP.put(ParameterType.Type.TEXT, TextControl.class);
        CONTROL_MAP.put(ParameterType.Type.TOGGLE, ToggleControl.class);
        CONTROL_MAP.put(ParameterType.Type.NODEREF, NoderefControl.class);
    }

    private Node node;
    private NetworkEventHandler handler = new NetworkEventHandler();
    private ArrayList<ParameterControl> controls = new ArrayList<ParameterControl>();

    public ParameterView() {
        setLayout(new BorderLayout());
        controlPanel = new JPanel(new GridBagLayout());
        controlPanel.setBackground(new Color(204, 204, 204));
        controlPanel.setPreferredSize(new Dimension(300, 300));
        JScrollPane scrollPane = new JScrollPane(controlPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, BorderLayout.CENTER);
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        Node oldNode = this.node;
        if (oldNode != null && oldNode.inNetwork())
            oldNode.getNetwork().removeNetworkEventListener(handler);
        this.node = node;
        if (node != null && node.inNetwork())
            node.getNetwork().addNetworkEventListener(handler);
        rebuildInterface();
        validate();
        repaint();
    }

    private void rebuildInterface() {
        controlPanel.removeAll();
        if (node == null) return;
        int rowindex = 0;
        for (Parameter p : node.getParameters()) {
            if (!p.isPrimitive()) continue;
            JLabel label = new JLabel(p.getLabel());
            label.setAlignmentX(JLabel.RIGHT_ALIGNMENT);
            label.putClientProperty("JComponent.sizeVariant", "small");
            label.setFont(PlatformUtils.getSmallBoldFont());
            Class controlClass = CONTROL_MAP.get(p.getType());
            JComponent control;
            if (controlClass != null) {
                control = (JComponent) constructControl(controlClass, p);
                controls.add((ParameterControl) control);

            } else {
                control = new JLabel("<no control>");
            }
            GridBagConstraints labelConstraints = new GridBagConstraints();
            labelConstraints.gridx = 0;
            labelConstraints.gridy = rowindex;
            labelConstraints.fill = GridBagConstraints.HORIZONTAL;
            labelConstraints.anchor = GridBagConstraints.LINE_END;
            controlPanel.add(label, labelConstraints);
            GridBagConstraints controlConstraints = new GridBagConstraints();
            controlConstraints.gridx = 1;
            controlConstraints.gridy = rowindex;
            controlConstraints.ipadx = 5;
            controlConstraints.ipady = 5;
            controlConstraints.fill = GridBagConstraints.HORIZONTAL;
            controlConstraints.anchor = GridBagConstraints.LINE_START;
            controlPanel.add(control, controlConstraints);
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
    }

    private ParameterControl constructControl(Class controlClass, Parameter p) {
        try {
            Constructor constructor = controlClass.getConstructor(Parameter.class);
            return (ParameterControl) constructor.newInstance(p);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Cannot construct control", e);
            throw new AssertionError("Cannot construct control");
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        Node n = new RectType(new NodeManager()).createNode();
        ParameterView p = new ParameterView();
        p.setNode(n);
        frame.setContentPane(p);
        frame.setSize(500, 500);
        frame.setVisible(true);
    }

    //// Network events ////

    private class NetworkEventHandler extends NetworkEventAdapter {
        @Override
        public void nodeChanged(Network source, Node node) {
            if (node == getNode()) {
                rebuildInterface();
            }
        }
    }

}
