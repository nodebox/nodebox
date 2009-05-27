package net.nodebox.client;

import net.nodebox.node.Node;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class AddressBar extends JPanel implements MouseListener {

    public static Image addressGradient;
    public static Image addressArrow;
    public static Font font = new Font("Lucida Grande", Font.BOLD, 11);
    public static Color normalColor = new Color(60, 60, 60);
    public static Color armedColor = new Color(0, 0, 0);
    public static Color shadowColor = new Color(255, 255, 255);

    static {
        try {
            addressGradient = ImageIO.read(new File("res/address-gradient.png"));
            addressArrow = ImageIO.read(new File("res/address-arrow.png"));
        } catch (IOException e) {
            System.out.println("e = " + e);
        }
    }

    //private ArrayList<Node> parts = new List<Node>[]{"root", "poster", "background"};
    private int[] positions;
    private int armed = -1;

    private NodeBoxDocument document;
    private Node node;

    public AddressBar(NodeBoxDocument document) {
        addMouseListener(this);
        setMinimumSize(new Dimension(0, 26));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        setPreferredSize(new Dimension(Integer.MAX_VALUE, 26));
        this.document = document;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
        repaint();
    }

    private java.util.List<Node> getNetworkParts() {
        ArrayList<Node> parts = new ArrayList<Node>();
        if (node == null) return parts;
        Node currentNode = node;
        parts.add(0, currentNode);
        while (currentNode.getParent() != null) {
            parts.add(0, currentNode.getParent());
            currentNode = currentNode.getParent();
        }
        return parts;
    }

    @Override
    protected void paintComponent(Graphics g) {
        java.util.List<Node> nodes = getNetworkParts();
        positions = new int[nodes.size()];
        Graphics2D g2 = (Graphics2D) g;

        g2.setFont(font);

        g2.drawImage(addressGradient, 0, 0, getWidth(), 26, null);

        int x = 6;

        for (int i = 0; i < nodes.size(); i++) {
            Node part = nodes.get(i);
            if (i == armed) {
                g2.setColor(armedColor);
            } else {
                g2.setColor(normalColor);
            }
            drawShadowText(g2, part.getName(), x, 16);

            int width = (int) g2.getFontMetrics().stringWidth(part.getName());
            x += width + 5;
            positions[i] = x + 10;
            g2.drawImage(addressArrow, x, 1, null);
            x += 15;
        }
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        int mx = e.getX();
        armed = partIndex(mx);
        repaint();
    }

    public void mouseReleased(MouseEvent e) {
        armed = -1;
        int mx = e.getX();
        int partIndex = partIndex(mx);
        if (partIndex == -1) return;
        java.util.List<Node> nodes = getNetworkParts();
        Node selectedNode = nodes.get(partIndex);
        //System.out.println("part = " + selectedNode);
        if (selectedNode != null)
            document.setActiveNetwork(selectedNode);
        repaint();
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
        armed = -1;
        repaint();
    }


    private int partIndex(int x) {
        if (positions == null) return -1;
        for (int i = 0; i < positions.length; i++) {
            if (x < positions[i])
                return i;
        }
        return -1;
    }

    public static void drawShadowText(Graphics2D g2, String s, int x, int y) {
        Color c = g2.getColor();
        g2.setColor(shadowColor);
        g2.drawString(s, x, y + 1);
        g2.setColor(c);
        g2.drawString(s, x, y);
    }

}
