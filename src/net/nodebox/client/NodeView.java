package net.nodebox.client;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.util.PPaintContext;
import net.nodebox.Icons;
import net.nodebox.node.Network;
import net.nodebox.node.Node;
import net.nodebox.node.Parameter;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class NodeView extends PNode implements Selectable, PropertyChangeListener {

    private final static Border normalBorder = BorderFactory.createLineBorder(Color.GRAY, 1);
    private final static Border selectedBorder = BorderFactory.createLineBorder(Color.BLACK, 3);
    private final static Font labelFont = new Font("Arial", Font.PLAIN, 12);

    public static final int NODE_WIDTH = 110;
    public static final int NODE_HEIGHT = 35;
    public static final int CONNECTOR_WIDTH = 28;
    public static final int CONNECTOR_HEIGHT = 7;
    public static final int RENDER_FLAG_WIDTH = 12;

    private NetworkView networkView;
    private Node node;

    private Border border;

    private boolean selected;

    private static boolean isConnecting;
    private static NodeView connectSource;
    private static NodeView connectTarget;

    public NodeView(NetworkView networkView, Node node) {
        this.networkView = networkView;
        this.node = node;
        this.selected = false;
        setPaint(new GradientPaint(0, 0, new Color(225, 225, 225), 0, NODE_HEIGHT, new Color(200, 200, 200)));
        //setPaint(new Color(222, 222, 222));
        setTransparency(1.0F);
        setBorder(normalBorder);
        addInputEventListener(new NodeHandler());
        setOffset(node.getX(), node.getY());
        setBounds(0, 0, NODE_WIDTH, NODE_HEIGHT);
        addPropertyChangeListener(PROPERTY_TRANSFORM, this);
        addPropertyChangeListener(PROPERTY_BOUNDS, this);
        addInputEventListener(new PopupHandler());
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

        // Define positions for node and connectors.
        double innerX = 2;
        double innerY = 2;
        double innerWidth = NODE_WIDTH - 5;
        double innerHeight = NODE_HEIGHT - CONNECTOR_HEIGHT - 5;
        Rectangle2D innerBounds = new Rectangle2D.Double(innerX, innerY, innerWidth, innerHeight);

        double outputX = innerX + (innerWidth - CONNECTOR_WIDTH) / 2;
        double outputY = innerY + innerHeight;
        Rectangle2D outputBounds = new Rectangle2D.Double(outputX, outputY, CONNECTOR_WIDTH, CONNECTOR_HEIGHT);

        // Draw the selection/connection border
        if (connectTarget == this || selected) {
            if (connectTarget == this) {
                g.setStroke(new BasicStroke(4.0F));
                g.setColor(Theme.getInstance().getBorderHighlightColor());
            } else {
                g.setStroke(new BasicStroke(4.0F));
                g.setColor(Theme.getInstance().getActionColor());
            }

            Area nodeArea = new Area(innerBounds);
            nodeArea.add(new Area(outputBounds));
            g.draw(nodeArea);
        }

        // Set the color for the node itself.
        g.setStroke(new BasicStroke(1));
        if (!node.hasError()) {
            g.setPaint(getPaint());
        } else {
            g.setPaint(Color.RED);
        }

        Color lightBorderColor = new Color(80, 80, 80);
        // Fill the node area.
        g.fill(innerBounds);

        // Draw the rendered flag.
        Rectangle2D renderFlagBounds = new Rectangle2D.Double(innerX + innerWidth - RENDER_FLAG_WIDTH, innerY, RENDER_FLAG_WIDTH, innerHeight);
        if (node.isRendered()) {
            g.setColor(Theme.getInstance().getActionColor());
            g.fill(renderFlagBounds);
        }
        g.setPaint(new Color(160, 160, 160));
        g.draw(renderFlagBounds);

        // Draw the node name.
        g.setFont(labelFont);
        g.setColor(Color.BLACK);
        g.drawString(node.getName(), 10, NODE_HEIGHT / 2);

        // Draw the node area.
        g.setPaint(lightBorderColor);
        g.draw(innerBounds);

        // Draw the output connector.
        g.setPaint(new Color(160, 160, 160));
        g.fill(outputBounds);
        g.setPaint(lightBorderColor);
        g.draw(outputBounds);
        Icon outputArrow = new Icons.ArrowIcon(Icons.ArrowIcon.SOUTH, lightBorderColor);
        outputArrow.paintIcon(null, g, (int) (outputX + (CONNECTOR_WIDTH - outputArrow.getIconWidth()) / 2), (int) (outputY + 1));


        g.setClip(clip);
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean s) {
        if (selected == s) return;
        selected = s;
        repaint();
    }

    private class NodeHandler extends PBasicInputEventHandler {

        protected Point2D dragPoint;
        private boolean isDragging;

        public void mouseClicked(PInputEvent e) {
            if (e.getClickCount() == 1) {
                e.getInputManager().setKeyboardFocus(this);
                networkView.singleSelect(NodeView.this);
            } else if (e.getClickCount() == 2 && node instanceof Network) {
                networkView.getPane().getDocument().setActiveNetwork((Network) node);
            }
            e.setHandled(true);
        }

        public void mousePressed(PInputEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {

                Point2D pt = NodeView.this.getOffset();
                double x = e.getPosition().getX() - pt.getX();
                double y = e.getPosition().getY() - pt.getY();

                // Find the area where the mouse is pressed
                // Possible areas are the output connector, and node itself.
                if (y < NODE_HEIGHT - CONNECTOR_HEIGHT) {
                    // Clicked in the node itself
                    if (x > NODE_WIDTH - RENDER_FLAG_WIDTH) {
                        // Clicked in the render flag area
                        getNode().setRendered();
                        return;
                    }
                    isDragging = true;
                    // Make sure that this node is also selected.
                    if (!isSelected()) {
                        // If other nodes are selected, deselect them so they
                        // don't get dragged along.
                        networkView.singleSelect(NodeView.this);
                    }
                    dragPoint = e.getPosition();
                } else {
                    isConnecting = true;
                    connectSource = NodeView.this;
                }
            }
        }

        public void mouseEntered(PInputEvent e) {
            if (isConnecting) {
                NodeView oldTarget = connectTarget;
                connectTarget = NodeView.this;
                if (oldTarget != null)
                    oldTarget.repaint();
                connectTarget.repaint();
            }
        }

        public void mouseExited(PInputEvent e) {
            if (isConnecting) {
                NodeView oldTarget = connectTarget;
                connectTarget = null;
                if (oldTarget != null)
                    oldTarget.repaint();
            }
        }

        public void mouseDragged(PInputEvent e) {
            if (isDragging) {
                Point2D pt = e.getPosition();
                double dx = pt.getX() - dragPoint.getX();
                double dy = pt.getY() - dragPoint.getY();
                getNetworkView().dragSelection(dx, dy);
                dragPoint = pt;
            }
            e.setHandled(true);
        }

        public void mouseReleased(PInputEvent event) {
            if (isConnecting) {
                if (connectTarget != null)
                    connectTarget.repaint();
                NodeView.this.repaint();
                if (connectSource != null && connectTarget != null) {
                    java.util.List<Parameter> compatibleParameters = connectTarget.getNode().getCompatibleInputs(connectSource.getNode());
                    if (compatibleParameters.isEmpty()) {
                        // There are no compatible parameters.
                    } else if (compatibleParameters.size() == 1) {
                        // Only one possible connection, make it now.
                        Parameter inputParameter = compatibleParameters.get(0);
                        inputParameter.connect(connectSource.getNode());
                    } else {
                        JPopupMenu menu = new JPopupMenu("Select input");
                        for (Parameter p : compatibleParameters) {
                            Action a = new SelectCompatibleParameterAction(connectSource.getNode(), connectTarget.getNode(), p);
                            menu.add(a);
                        }
                        Point pt = getNetworkView().getMousePosition();
                        menu.show(getNetworkView(), pt.x, pt.y);
                    }
                }
            }
            isDragging = false;
            isConnecting = false;
            connectSource = null;
            connectTarget = null;
        }

    }

    class SelectCompatibleParameterAction extends AbstractAction {

        private Node outputNode;
        private Node inputNode;
        private Parameter inputParameter;

        SelectCompatibleParameterAction(Node outputNode, Node inputNode, Parameter inputParameter) {
            super(inputParameter.getName());
            this.outputNode = outputNode;
            this.inputNode = inputNode;
            this.inputParameter = inputParameter;
        }

        public void actionPerformed(ActionEvent e) {
            inputParameter.connect(outputNode);
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


    private class PopupHandler extends PBasicInputEventHandler {
        public void processEvent(PInputEvent e, int i) {
            if (!e.isPopupTrigger()) return;
            JPopupMenu menu = new JPopupMenu();
            menu.add(new SetRenderedAction());
            menu.add(new DeleteAction());
            Point2D p = e.getCanvasPosition();
            menu.show(NodeView.this.networkView, (int) p.getX(), (int) p.getY());
            e.setHandled(true);
        }

    }

    private class SetRenderedAction extends AbstractAction {
        private SetRenderedAction() {
            super("Set Rendered");
        }

        public void actionPerformed(ActionEvent e) {
            node.setRendered();
        }
    }


    private class DeleteAction extends AbstractAction {

        public DeleteAction() {
            super("Delete");
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0));
        }

        public void actionPerformed(ActionEvent e) {
            node.getNetwork().remove(node);
        }
    }
}


