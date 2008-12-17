package net.nodebox.client;

import net.nodebox.client.parameter.FloatControl;
import net.nodebox.client.parameter.ParameterControl;
import net.nodebox.node.*;
import net.nodebox.node.vector.RectType;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ParameterView extends JComponent {

    private static final Map<ParameterType.Type, Class> CONTROL_MAP;
    private JPanel controlPanel;

    static {
        CONTROL_MAP = new HashMap<ParameterType.Type, Class>();
        CONTROL_MAP.put(ParameterType.Type.FLOAT, FloatControl.class);
    }

    private Node node;
    private NetworkEventHandler handler = new NetworkEventHandler();
    private ArrayList<ParameterControl> controls = new ArrayList<ParameterControl>();

    public ParameterView() {
        setLayout(new BorderLayout());
        controlPanel = new JPanel(new GridLayout(0, 2, 5, 5));
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
        for (Parameter p : node.getParameters()) {
            JLabel label = new JLabel(p.getLabel());
            label.putClientProperty("JComponent.sizeVariant", "small");
            Class controlClass = CONTROL_MAP.get(p.getType());
            JComponent control;
            if (controlClass != null) {
                control = (JComponent) constructControl(controlClass, p);
                controls.add((ParameterControl) control);

            } else {
                control = new JLabel("<no control>");
            }
            controlPanel.add(label);
            controlPanel.add(control);

        }
    }

    private ParameterControl constructControl(Class controlClass, Parameter p) {
        try {
            Constructor constructor = controlClass.getConstructor(Parameter.class);
            return (FloatControl) constructor.newInstance(p);
        } catch (Exception e) {
            return null;
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
