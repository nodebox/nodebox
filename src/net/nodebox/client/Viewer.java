package net.nodebox.client;

import net.nodebox.graphics.GraphicsContext;
import net.nodebox.graphics.Grob;
import net.nodebox.handle.Handle;
import net.nodebox.node.Network;
import net.nodebox.node.NetworkDataListener;
import net.nodebox.node.Node;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class Viewer extends JComponent implements NetworkDataListener, MouseListener, MouseMotionListener {

    private Pane pane;
    private Network network;
    private Node activeNode;
    private Handle handle;

    public Viewer(Pane pane, Network network) {
        this.pane = pane;
        this.network = network;
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    public Network getNetwork() {
        return network;
    }

    public void setNetwork(Network network) {
        if (this.network == network) return;
        Network oldNetwork = this.network;
        if (oldNetwork != null) {
            oldNetwork.removeNetworkDataListener(this);
        }
        this.network = network;
        if (this.network == null) return;
        network.addNetworkDataListener(this);
        repaint();
    }

    public void setActiveNode(Node node) {
        activeNode = node;
        if (activeNode != null)
            handle = activeNode.createHandle();
        repaint();
    }


    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.translate(getWidth() / 2.0, getHeight() / 2.0);
        if (getNetwork() == null) return;
        Object outputValue = getNetwork().getOutputValue();
        if (outputValue instanceof Grob) {
            ((Grob) outputValue).draw(g2);
        }
        // Draw handle
        if (handle != null) {
            // Create a canvas with a transparent background
            net.nodebox.graphics.Canvas canvas = new net.nodebox.graphics.Canvas();
            canvas.setBackground(new net.nodebox.graphics.Color(0, 0, 0, 0));
            GraphicsContext ctx = new GraphicsContext(canvas);
            handle.draw(ctx);
            ctx.getCanvas().draw(g2);
        }
        // Draw center
        g.setColor(new Color(240, 240, 240));
        g.drawLine(-getWidth() / 2, 0, getWidth() / 2, 0);
        g.drawLine(0, -getHeight() / 2, 0, getHeight() / 2);
    }

    //// Network data events ////

    public void networkDirty(Network network) {
        // The network is dirty, but we wait for the document to update the network.
        // This will send the networkUpdated event.
    }

    public void networkUpdated(Network network) {
        if (network == getNetwork()) {
            repaint();
        }
    }

    //// Mouse events ////

    private net.nodebox.graphics.Point pointForEvent(MouseEvent e) {
        double cx = -getWidth() / 2.0 + e.getX();
        double cy = -getHeight() / 2.0 + e.getY();
        return new net.nodebox.graphics.Point(cx, cy);
    }

    public void mouseClicked(MouseEvent e) {
        if (handle == null) return;
        handle.mouseClicked(pointForEvent(e));
    }

    public void mousePressed(MouseEvent e) {
        if (handle == null) return;
        handle.mousePressed(pointForEvent(e));
    }

    public void mouseReleased(MouseEvent e) {
        if (handle == null) return;
        handle.mouseReleased(pointForEvent(e));
    }

    public void mouseEntered(MouseEvent e) {
        if (handle == null) return;
        handle.mouseEntered(pointForEvent(e));
    }

    public void mouseExited(MouseEvent e) {
        if (handle == null) return;
        handle.mouseExited(pointForEvent(e));
    }

    public void mouseDragged(MouseEvent e) {
        if (handle == null) return;
        handle.mouseDragged(pointForEvent(e));
    }

    public void mouseMoved(MouseEvent e) {
        if (handle == null) return;
        handle.mouseMoved(pointForEvent(e));
    }
}
