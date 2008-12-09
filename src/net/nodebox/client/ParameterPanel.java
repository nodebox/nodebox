package net.nodebox.client;

import net.nodebox.node.Node;
import net.nodebox.node.Parameter;
import net.nodebox.node.canvas.RectNode;

import javax.swing.*;
import java.awt.*;

public class ParameterPanel extends JPanel {

    private Node node;

    public ParameterPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
        rebuildInterface();
        invalidate();
        repaint();
    }

    private void rebuildInterface() {
        for (Parameter p : node.getParameters()) {
            JTextField f = new JTextField(p.getName());
            add(f);
        }
        add(new Box.Filler(new Dimension(0, 0), new Dimension(0, Integer.MAX_VALUE), new Dimension(0, Integer.MAX_VALUE)));
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        Node n = new RectNode();
        ParameterPanel p = new ParameterPanel();
        p.setNode(n);
        frame.setContentPane(p);
        frame.setSize(500, 500);
        frame.setVisible(true);
    }
    
}
