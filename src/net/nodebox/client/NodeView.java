package net.nodebox.client;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.util.PPaintContext;
import net.nodebox.node.Network;
import net.nodebox.node.Node;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class NodeView extends PNode implements Selectable, PropertyChangeListener {

    private final static Border normalBorder = BorderFactory.createLineBorder(Color.GRAY, 1);
    private final static Border selectedBorder = BorderFactory.createLineBorder(Color.BLACK, 3);
    private final static Font labelFont = new Font("Arial", Font.PLAIN, 12);

    public static final int NODE_WIDTH = 100;
    public static final int NODE_HEIGHT = 25;

    private NetworkView networkView;
    private Node node;

    private Border border;

    private boolean selected;
    private boolean highlighted;

    public NodeView(NetworkView networkView, Node node) {
        this.networkView = networkView;
        this.node = node;
        this.selected = false;
        setPaint(new Color(140, 140, 145));
        setTransparency(1.0F);
        setBorder(normalBorder);
        addInputEventListener(new NodeHandler());
        setOffset(node.getX(), node.getY());
        setBounds(0, 0, NODE_WIDTH, NODE_HEIGHT);
        addPropertyChangeListener(PROPERTY_TRANSFORM, this);
        addPropertyChangeListener(PROPERTY_BOUNDS, this);

    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(PROPERTY_TRANSFORM)) {
            node.setPosition(new net.nodebox.graphics.Point(getOffset()));
        }
    }

    public NetworkView getNetworkView() {
        return networkView;
    }

    public Node getNode() {
        return node;
    }

    public Border getBorder() {
        return border;
    }

    public void setBorder(Border border) {
        this.border = border;
    }

    protected void paint(PPaintContext ctx) {
        //if (ctx.getScale() < 1) {
        Graphics2D g = ctx.getGraphics();
        Shape clip = g.getClip();
        g.clip(getBounds());
        //g.setClip(getBounds());

        g.setStroke(new BasicStroke(1));
        if (!node.hasError()) {
            g.setPaint(getPaint());
        } else {
            g.setPaint(Color.RED);
        }
        g.fill(getBounds());

        g.setFont(labelFont);
        g.setColor(Color.WHITE);


        g.drawString(node.getName(), 10, NODE_HEIGHT / 2);

        if (node.isRendered()) {
            g.setColor(Theme.getInstance().getActionColor());
            g.fillRect(0, 0, 5, NODE_HEIGHT);
        }

        paintBorder(g);

        if (highlighted) {
            g.setColor(Color.WHITE);
            g.drawRect((int) getX(), (int) getY(), (int) getWidth() - 1, (int) getHeight() - 1);
        }
        g.setClip(clip);
    }

    protected void paintBorder(Graphics2D g2) {
        Color borderColor;
        if (selected) {
            borderColor = Theme.getInstance().getActionColor();
        } else {
            borderColor = Color.BLACK;
        }
        g2.setColor(borderColor);
        int x = (int) getX();
        int y = (int) getY();
        int width = (int) getWidth();
        int height = (int) getHeight();
        g2.drawRect(x, y, width - 1, height - 1);
//        g2.setColor(new Color(88, 87, 96));
//        g2.drawLine(x, y, x+1, y);
//        g2.drawLine(x+width-3, )
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean s) {
        if (selected == s) return;
        selected = s;
        if (s) {
            //LOG.debug("select " + node.getName());
            setBorder(selectedBorder);
        } else {
            //LOG.debug("deselect " + node.getName());
            setBorder(normalBorder);
        }
        repaint();
    }

    public boolean isHighlighted() {
        return highlighted;
    }

    public void setHighlighted(boolean h) {
        if (highlighted == h) return;
        highlighted = h;
        repaint();
    }

    private class NodeHandler extends PBasicInputEventHandler {

        protected Point2D dragPoint;
        private boolean isDraggingNode;

        public void mouseClicked(PInputEvent e) {
            if (e.getButton() == 3) {
                node.setRendered();
            } else if (e.getClickCount() == 1) {
                e.getInputManager().setKeyboardFocus(this);
                boolean oldSelection = selected;
                if ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != InputEvent.SHIFT_DOWN_MASK) {
                    networkView.deselectAll();
                }
                if (!oldSelection) {
                    networkView.select(NodeView.this);
                    networkView.getPane().getDocument().setActiveNode(node);
                }
            } else if (e.getClickCount() == 2 && node instanceof Network) {
                networkView.getPane().getDocument().setActiveNetwork((Network) node);
            }
            e.setHandled(true);
        }

//        public void keyReleased(PInputEvent e) {
//            if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
//                LOG.info("Pressed backspace");
//                controller.remove(node);
//            }
//        }

        public void mousePressed(PInputEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                isDraggingNode = true;
                Point2D pt = NodeView.this.getOffset();
                double x = e.getPosition().getX() - pt.getX();
                double y = e.getPosition().getY() - pt.getY();
                dragPoint = new Point2D.Double(x, y);
            }
        }

        public void mouseDragged(PInputEvent e) {
            if (isDraggingNode) {
                Point2D pt = e.getPosition();
                double x = pt.getX() - dragPoint.getX();
                double y = pt.getY() - dragPoint.getY();
                setOffset(x, y);
            }
            e.setHandled(true);
        }

        public void mouseReleased(PInputEvent event) {
            isDraggingNode = false;
        }

    }

    /*
    private class RenderHandler extends PBasicInputEventHandler {
        public void mouseClicked(PInputEvent e) {
            if (isRendered()) {
                networkView.setRendered(null);
            } else {
                networkView.setRendered(NodeView.this);
            }
            e.setHandled(true);
        }
    }

    private class HighlightHandler extends PBasicInputEventHandler {
        public void mouseClicked(PInputEvent e) {
            if (isHighlighted()) {
                networkView.setHighlight(null);
            } else {
                networkView.setHighlight(NodeView.this);
            }
            e.setHandled(true);
        }
    }*/


}


