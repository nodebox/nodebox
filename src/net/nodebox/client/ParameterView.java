package net.nodebox.client;

import net.nodebox.node.Network;
import net.nodebox.node.NetworkEventAdapter;
import net.nodebox.node.Node;
import net.nodebox.node.Parameter;
import net.nodebox.node.vector.RectNode;

import javax.swing.*;
import java.awt.*;

public class ParameterView extends JPanel {

    private Node node;
    private NetworkEventHandler handler = new NetworkEventHandler();
    // private static Map CONTROL_MAP

    public ParameterView() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
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
        removeAll();
        if (node == null) return;
        for (Parameter p : node.getParameters()) {
            JTextField f = new JTextField(p.getName());
            add(f);
        }
        add(new Box.Filler(new Dimension(0, 0), new Dimension(0, Integer.MAX_VALUE), new Dimension(0, Integer.MAX_VALUE)));
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        Node n = new RectNode();
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

    //// Inner classes ////

    public class ParameterRow extends JPanel {

    }
}
