package nodebox.client;

import nodebox.graphics.GraphicsContext;
import nodebox.graphics.Grob;
import nodebox.handle.Handle;
import nodebox.node.DirtyListener;
import nodebox.node.Node;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

public class Viewer extends JComponent implements DirtyListener, MouseListener, MouseMotionListener {

    private Pane pane;
    private Node node;
    private Node activeNode;
    private Handle handle;
    private BufferedImage canvasImage;
    private boolean showHandle;

    public Viewer(Pane pane, Node node) {
        this.pane = pane;
        this.node = node;
        addMouseListener(this);
        addMouseMotionListener(this);
        showHandle = true;
    }

    public boolean isShowHandle() {
        return showHandle;
    }

    public void setShowHandle(boolean showHandle) {
        this.showHandle = showHandle;
        repaint();
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        if (this.node == node) return;
        Node oldNode = this.node;
        if (oldNode != null) {
            oldNode.removeDirtyListener(this);
        }
        this.node = node;
        if (this.node == null) return;
        node.addDirtyListener(this);
        repaint();
    }

    public void setActiveNode(Node node) {
        activeNode = node;
        if (activeNode != null) {
            handle = activeNode.createHandle();
        } else {
            handle = null;
        }
        repaint();
    }


    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Fill the background with a neutral grey.
        g2.setColor(new Color(232, 232, 232));
        Rectangle clip = g.getClipBounds();
        g2.fillRect(clip.x, clip.y, clip.width, clip.height);

        //if (canvasImage != null)
        //g2.drawImage(canvasImage,0, 0, null);

        if (getNode() == null) return;
        Object outputValue = getNode().getOutputValue();
        if (outputValue instanceof Grob) {
            g2.translate(getWidth() / 2.0, getHeight() / 2.0);
            ((Grob) outputValue).draw(g2);
        } else if (outputValue != null) {
            String s = outputValue.toString();
            g2.setColor(SwingUtils.COLOR_NORMAL);
            g2.setFont(PlatformUtils.getEditorFont());
            g2.drawString(s, 5, 20);
        }

        // Draw handle
        if (handle != null && showHandle) {
            // Create a canvas with a transparent background
            nodebox.graphics.Canvas canvas = new nodebox.graphics.Canvas();
            canvas.setBackground(new nodebox.graphics.Color(0, 0, 0, 0));
            GraphicsContext ctx = new GraphicsContext(canvas);
            handle.draw(ctx);
            ctx.getCanvas().draw(g2);
        }
        // Draw center
        //g.setColor(new Color(240, 240, 240));
        //g.drawLine(-getWidth() / 2, 0, getWidth() / 2, 0);
        //g.drawLine(0, -getHeight() / 2, 0, getHeight() / 2);
    }

    //// Network data events ////

    public void nodeDirty(Node node) {
        // The node is dirty, but we wait for the document to update the network.
        // This will send the nodeUpdated event.
    }

    public void nodeUpdated(Node node) {
        if (node == getNode()) {
            repaint();
        }
        /*
        canvasImage = null;
        if (getNetwork() == null || getNetwork() != network) return;
        Object outputValue = getNetwork().getOutputValue();
        if (!(outputValue instanceof Grob)) return;

        Grob g = (Grob)outputValue;
        Rect grobBounds = g.getBounds();
        if (grobBounds.isEmpty()) return;
        canvasImage = new BufferedImage((int)grobBounds.getWidth(), (int)grobBounds.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = (Graphics2D) canvasImage.getGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.translate(getWidth() / 2.0, getHeight() / 2.0);
        g.draw(g2);
        */
    }

    //// Mouse events ////

    private nodebox.graphics.Point pointForEvent(MouseEvent e) {
        double cx = -getWidth() / 2.0 + e.getX();
        double cy = -getHeight() / 2.0 + e.getY();
        return new nodebox.graphics.Point(cx, cy);
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
