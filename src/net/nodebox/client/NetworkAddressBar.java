package net.nodebox.client;

import net.nodebox.node.Network;
import net.nodebox.node.Node;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class NetworkAddressBar extends JPanel {


    private Pane pane;
    private Node node;

    public NetworkAddressBar(Pane pane) {
        setLayout(new FlowLayout(FlowLayout.LEADING, 5, 0));
        this.pane = pane;
        setPreferredSize(new Dimension(400, 22));
        setBackground(Theme.getInstance().getBackgroundColor());
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
        removeAll();
        if (node == null) return;
        for (Node n : getNetworkParts()) {
            NetworkSegment ns = new NetworkSegment(pane, n);
            add(ns);
        }
        validate();
        repaint();
    }

    private java.util.List<Node> getNetworkParts() {
        ArrayList<Node> parts = new ArrayList<Node>();
        Node currentNode = node;
        parts.add(0, currentNode);
        while (currentNode.getNetwork() != null) {
            parts.add(0, currentNode.getNetwork());
            currentNode = currentNode.getNetwork();
        }
        return parts;
    }

    class NetworkSegment extends JButton implements ActionListener {
        private Pane pane;
        private Node node;

        NetworkSegment(Pane pane, Node node) {
            super(node.getName());
            this.pane = pane;
            this.node = node;
            addActionListener(this);
            setPreferredSize(new Dimension(80, 22));
            setBackground(Theme.getInstance().getBackgroundColor());
            setForeground(Theme.getInstance().getTextColor());
        }

        public void actionPerformed(ActionEvent e) {
            if (node instanceof Network) {
                pane.getDocument().setActiveNetwork((Network) node);
            }
        }

        @Override
        public void paint(Graphics g) {
            getModel().isArmed();
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(Theme.getInstance().getTextColor());
            g2.drawString(node.getName(), 0, 18);
            Dimension d = getSize();
            if (getModel().isArmed()) {
                g2.setColor(Theme.getInstance().getActionColor());
            } else {
                g2.setColor(Theme.getInstance().getBorderColor());
            }
            g2.drawLine(0, 20, 100, 20);
        }
    }

}
