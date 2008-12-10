package net.nodebox.client;

import net.nodebox.node.Node;
import net.nodebox.node.Parameter;
import net.nodebox.node.vector.RectNode;

import javax.swing.*;
import java.awt.*;

public class ParameterView extends JPanel {

    private Node node;

    public ParameterView() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
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

}
