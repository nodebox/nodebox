package net.nodebox.client;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.util.PPaintContext;
import net.nodebox.node.ConnectionError;
import net.nodebox.node.InvalidNameException;
import net.nodebox.node.Node;
import net.nodebox.node.Port;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;

public class NodeView extends PNode implements Selectable, PropertyChangeListener {

    public static final int NODE_FULL_SIZE = 70;
    public static final int NODE_IMAGE_SIZE = 50;
    public static final int TEXT_HEIGHT = 14;
    public static final Rectangle OUTPUT_BOUNDS = new Rectangle(60, 29, 10, 12);
    public static final int NODE_PORT_WIDTH = 10;
    public static final int NODE_PORT_HEIGHT = 10;


    private static Image nodeMask, nodeGlow, nodeInPort, nodeOutPort, nodeGeneric, nodeReflection, nodeError, nodeRendered, nodeRim;

    static {
        try {
            nodeMask = ImageIO.read(new File("res/node-mask.png"));
            nodeGlow = ImageIO.read(new File("res/node-glow.png"));
            nodeInPort = ImageIO.read(new File("res/node-in-port.png"));
            nodeOutPort = ImageIO.read(new File("res/node-out-port.png"));
            nodeGeneric = ImageIO.read(new File("res/node-generic.png"));
            nodeReflection = ImageIO.read(new File("res/node-reflection.png"));
            nodeError = ImageIO.read(new File("res/node-error.png"));
            nodeRendered = ImageIO.read(new File("res/node-rendered.png"));
            nodeRim = ImageIO.read(new File("res/node-rim.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private NetworkView networkView;
    private Node node;
    private BufferedImage fullIcon;

    private Border border;

    private boolean selected;

    private static boolean isConnecting;
    private static NodeView connectSource;
    private static NodeView connectTarget;

    public NodeView(NetworkView networkView, Node node) {
        this.networkView = networkView;
        this.node = node;
        this.selected = false;
        setTransparency(1.0F);
        addInputEventListener(new NodeHandler());
        setOffset(node.getX(), node.getY());
        setBounds(0, 0, NODE_FULL_SIZE, NODE_FULL_SIZE + TEXT_HEIGHT);
        addPropertyChangeListener(PROPERTY_TRANSFORM, this);
        addPropertyChangeListener(PROPERTY_BOUNDS, this);
        addInputEventListener(new PopupHandler());
        updateIcon();
    }

    public void updateIcon() {
        Image icon = getImageForNode(node);
        // Create the icon.
        // We include only the parts that are not changed by state.
        // This means leaving off the error and rendered image.
        // Also, we draw the rim at the very end, above the error and rendered,
        // so we can't draw it here yet.
        fullIcon = new BufferedImage(NODE_FULL_SIZE, NODE_FULL_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D fg = fullIcon.createGraphics();
        // Count the input ports and draw them.
        java.util.List<Port> inputs = node.getPorts();
        if (inputs.size() > 0) {
            int portY = (NODE_FULL_SIZE - NODE_PORT_HEIGHT) / 2;
            fg.drawImage(nodeInPort, 0, portY, null);
        }
        // Draw the other layers.
        fg.drawImage(nodeOutPort, 0, 0, null);
        fg.drawImage(nodeMask, 0, 0, null);
        fg.setComposite(AlphaComposite.SrcIn);
        fg.drawImage(icon, 10, 10, NODE_IMAGE_SIZE, NODE_IMAGE_SIZE, null);
        fg.setComposite(AlphaComposite.SrcOver);
        //fg.drawImage(nodeReflection, 0, 0, null);
        fg.dispose();
    }

    /**
     * Tries to find an image representation for the node.
     * The image should be located near the library, and have the same name as the library.
     * <p/>
     * If this node has no image, the prototype is searched to find its image. If no image could be found,
     * a generic image is retured.
     *
     * @param node the node
     * @return an Image object.
     */
    private Image getImageForNode(Node node) {
        if (node == null) return nodeGeneric;
        File libraryFile = node.getLibrary().getFile();
        if (libraryFile != null) {
            File libraryDirectory = libraryFile.getParentFile();
            if (libraryDirectory != null) {
                File nodeImageFile = new File(libraryDirectory, node.getName() + ".png");
                if (nodeImageFile.exists()) {
                    try {
                        return ImageIO.read(nodeImageFile);
                    } catch (IOException ignored) {
                        // Pass through
                    }
                }
            }
        }
        // Look for the prototype
        return getImageForNode(node.getPrototype());
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
        Graphics2D g = ctx.getGraphics();
        Shape clip = g.getClip();
        g.clip(getBounds());

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);

        // Draw the selection/connection border
        if (connectTarget == this || selected)
            g.drawImage(nodeGlow, 0, 0, null);
        g.drawImage(fullIcon, 0, 0, null);
        if (node.hasError())
            g.drawImage(nodeError, 0, 0, null);
        if (node.isRendered())
            g.drawImage(nodeRendered, 0, 0, null);
        g.drawImage(nodeRim, 0, 0, null);

        // Draw the node name.
        g.setFont(SwingUtils.FONT_BOLD);
        g.setColor(new Color(20, 20, 20));
        int textWidth = g.getFontMetrics().stringWidth(node.getName());
        int x = (int) ((NODE_FULL_SIZE - textWidth) / 2f);
        SwingUtils.drawShadowText(g, node.getName(), x, NODE_FULL_SIZE + 5, new Color(133, 133, 133));

        // Reset the clipping.
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
            } else if (e.getClickCount() == 2 && e.isLeftMouseButton()) {
                node.setRendered();
                //networkView.getPane().getDocument().setActiveNetwork(node);
            }
            e.setHandled(true);
        }

        public void mousePressed(PInputEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {

                Point2D pt = NodeView.this.getOffset();
                double x = e.getPosition().getX() - pt.getX();
                double y = e.getPosition().getY() - pt.getY();

                // Find the area where the mouse is pressed
                // Possible areas are the output connector and the node itself.
                if (OUTPUT_BOUNDS.contains(x, y)) {
                    isConnecting = true;
                    connectSource = NodeView.this;
                } else {
                    isDragging = true;
                    // Make sure that this node is also selected.
                    if (!isSelected()) {
                        // If other nodes are selected, deselect them so they
                        // don't get dragged along.
                        networkView.singleSelect(NodeView.this);
                    }
                    dragPoint = e.getPosition();
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
                    java.util.List<Port> compatiblePorts = connectTarget.getNode().getCompatibleInputs(connectSource.getNode());
                    if (compatiblePorts.isEmpty()) {
                        // There are no compatible parameters.
                    } else if (compatiblePorts.size() == 1) {
                        // Only one possible connection, make it now.
                        Port inputPort = compatiblePorts.get(0);
                        try {
                            inputPort.connect(connectSource.getNode());
                        } catch (ConnectionError e) {
                            JOptionPane.showMessageDialog(networkView, e.getMessage(), "Connection error", JOptionPane.ERROR_MESSAGE);
                        }
                    } else {
                        JPopupMenu menu = new JPopupMenu("Select input");
                        for (Port p : compatiblePorts) {
                            Action a = new SelectCompatiblePortAction(connectSource.getNode(), connectTarget.getNode(), p);
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

    class SelectCompatiblePortAction extends AbstractAction {

        private Node outputNode;
        private Node inputNode;
        private Port inputPort;

        SelectCompatiblePortAction(Node outputNode, Node inputNode, Port inputPort) {
            super(inputPort.getName());
            this.outputNode = outputNode;
            this.inputNode = inputNode;
            this.inputPort = inputPort;
        }

        public void actionPerformed(ActionEvent e) {
            try {
                inputPort.connect(outputNode);
            } catch (ConnectionError ce) {
                JOptionPane.showMessageDialog(networkView, ce.getMessage(), "Connection error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private class PopupHandler extends PBasicInputEventHandler {
        public void processEvent(PInputEvent e, int i) {
            if (!e.isPopupTrigger()) return;
            JPopupMenu menu = new JPopupMenu();
            menu.add(new SetRenderedAction());
            menu.add(new RenameAction());
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
            networkView.repaint();
        }
    }

    private class RenameAction extends AbstractAction {
        private RenameAction() {
            super("Rename");
        }

        public void actionPerformed(ActionEvent e) {
            String s = JOptionPane.showInputDialog(networkView, "New name:", node.getName());
            if (s == null || s.length() == 0)
                return;
            try {
                node.setName(s);
            } catch (InvalidNameException ex) {
                JOptionPane.showMessageDialog(networkView, "The given name is not valid.\n" + ex.getMessage(), Application.NAME, JOptionPane.ERROR_MESSAGE);
            }
        }
    }


    private class DeleteAction extends AbstractAction {

        public DeleteAction() {
            super("Delete");
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0));
        }

        public void actionPerformed(ActionEvent e) {
            node.getParent().remove(node);
        }
    }

}


