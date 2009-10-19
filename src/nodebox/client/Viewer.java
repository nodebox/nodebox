package nodebox.client;

import nodebox.graphics.GraphicsContext;
import nodebox.graphics.Grob;
import nodebox.graphics.IGeometry;
import nodebox.graphics.Path;
import nodebox.handle.Handle;
import nodebox.node.DirtyListener;
import nodebox.node.Node;
import nodebox.node.ProcessingContext;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class Viewer extends JComponent implements DirtyListener, MouseListener, MouseMotionListener, KeyListener {

    public static final float POINT_SIZE = 4f;

    private Pane pane;
    private Node node;
    private Node activeNode;
    private Handle handle;
    private BufferedImage canvasImage;
    private boolean showHandle;
    private boolean showPoints;
    private boolean showPointNumbers;

    public Viewer(Pane pane, Node node) {
        this.pane = pane;
        this.node = node;
        addMouseListener(this);
        addMouseMotionListener(this);
        showHandle = true;
        showPoints = false;
        setFocusable(true);
        addKeyListener(this);
    }

    public boolean isShowHandle() {
        return showHandle;
    }

    public void setShowHandle(boolean showHandle) {
        this.showHandle = showHandle;
        repaint();
    }

    public boolean isShowPoints() {
        return showPoints;
    }

    public void setShowPoints(boolean showPoints) {
        this.showPoints = showPoints;
        repaint();
    }

    public boolean isShowPointNumbers() {
        return showPointNumbers;
    }

    public void setShowPointNumbers(boolean showPointNumbers) {
        this.showPointNumbers = showPointNumbers;
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
            if (handle != null) {
                handle.setViewer(this);
            }
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

        // Draw the handle.
        if (handle != null && showHandle) {
            // Create a canvas with a transparent background
            nodebox.graphics.Canvas canvas = new nodebox.graphics.Canvas();
            canvas.setBackground(new nodebox.graphics.Color(0, 0, 0, 0));
            GraphicsContext ctx = new GraphicsContext(canvas);
            handle.draw(ctx);
            ctx.getCanvas().draw(g2);
        }

        // Draw the points.
        if (showPoints && outputValue instanceof IGeometry) {
            // Create a canvas with a transparent background
            Path onCurves = new Path();
            Path offCurves = new Path();
            onCurves.setFill(new nodebox.graphics.Color(0f, 0f, 1f));
            offCurves.setFill(new nodebox.graphics.Color(1f, 0f, 0f));
            IGeometry p = (IGeometry) outputValue;
            for (nodebox.graphics.Point pt : p.getPoints()) {
                if (pt.isOnCurve()) {
                    onCurves.ellipse(pt.x, pt.y, POINT_SIZE, POINT_SIZE);
                } else {
                    offCurves.ellipse(pt.x, pt.y, POINT_SIZE, POINT_SIZE);
                }
            }
            onCurves.draw(g2);
            offCurves.draw(g2);
        }

        // Draw the point numbers.
        if (showPointNumbers && outputValue instanceof IGeometry) {
            g2.setFont(new Font("Monospaced", Font.PLAIN, 10));
            g2.setColor(Color.BLUE);
            // Create a canvas with a transparent background
            IGeometry p = (IGeometry) outputValue;
            int index = 0;
            for (nodebox.graphics.Point pt : p.getPoints()) {
                if (pt.isOnCurve()) {
                    g2.setColor(Color.BLUE);
                } else {
                    g2.setColor(Color.RED);
                }
                g2.drawString(index + "", pt.x + 3, pt.y - 2);
                index++;
            }
        }

        // Draw the center.
        //g.setColor(new Color(240, 240, 240));
        //g.drawLine(-getWidth() / 2, 0, getWidth() / 2, 0);
        //g.drawLine(0, -getHeight() / 2, 0, getHeight() / 2);

        // Draw the bounding box.
        //if (outputValue instanceof IGeometry) {
        //    IGeometry p = (IGeometry) outputValue;
        //    g2.setColor(Color.BLUE);
        //    g2.draw(p.getBounds().getRectangle2D());
        //}
    }

    //// Network data events ////

    public void nodeDirty(Node node) {
        // The node is dirty, but we wait for the document to update the network.
        // This will send the nodeUpdated event.
    }

    public void nodeUpdated(Node node, ProcessingContext context) {
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
        return new nodebox.graphics.Point((float) cx, (float) cy);
    }

    public void mouseClicked(MouseEvent e) {
        requestFocus();
        if (handle == null) return;
        handle.mouseClicked(pointForEvent(e));
    }

    public void mousePressed(MouseEvent e) {
        requestFocus();
        if (handle == null) return;
        handle.mousePressed(pointForEvent(e));
    }

    public void mouseReleased(MouseEvent e) {
        requestFocus();
        if (handle == null) return;
        handle.mouseReleased(pointForEvent(e));
    }

    public void mouseEntered(MouseEvent e) {
        requestFocus();
        if (handle == null) return;
        handle.mouseEntered(pointForEvent(e));
    }

    public void mouseExited(MouseEvent e) {
        requestFocus();
        if (handle == null) return;
        handle.mouseExited(pointForEvent(e));
    }

    public void mouseDragged(MouseEvent e) {
        requestFocus();
        if (handle == null) return;
        handle.mouseDragged(pointForEvent(e));
    }

    public void mouseMoved(MouseEvent e) {
        requestFocus();
        if (handle == null) return;
        handle.mouseMoved(pointForEvent(e));
    }

    public void keyTyped(KeyEvent e) {
        if (handle == null) return;
        handle.keyTyped(e.getKeyCode(), e.getModifiersEx());
    }

    public void keyPressed(KeyEvent e) {
        if (handle == null) return;
        handle.keyPressed(e.getKeyCode(), e.getModifiersEx());
    }

    public void keyReleased(KeyEvent e) {
        if (handle == null) return;
        handle.keyReleased(e.getKeyCode(), e.getModifiersEx());
    }

    @Override
    public boolean isFocusable() {
        return true;
    }
}
