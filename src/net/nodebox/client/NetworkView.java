package net.nodebox.client;

import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.event.PInputEventFilter;
import edu.umd.cs.piccolo.event.PPanEventHandler;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PPaintContext;
import net.nodebox.node.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

public class NetworkView extends PCanvas implements NodeChildListener, DirtyListener {

    public static final String SELECT_PROPERTY = "NetworkView.select";
    public static final String HIGHLIGHT_PROPERTY = "highlight";
    public static final String RENDER_PROPERTY = "render";
    public static final String NETWORK_PROPERTY = "network";

    private Pane pane;
    private Node node;
    private Set<NodeView> selection = new HashSet<NodeView>();
    private ConnectionLayer connectionLayer;
    private SelectionHandler selectionHandler = new SelectionHandler();
    private SelectionMarker selectionMarker;
    private DialogHandler dialogHandler = new DialogHandler();
    private PopupHandler popupHandler;
    private JPopupMenu networkMenu;
    private boolean networkError;

    public NetworkView(Pane pane, Node node) {
        this.pane = pane;
        this.node = node;
        if (node != null)
            this.networkError = node.hasError();
        setBackground(new Color(69, 69, 69));
        addInputEventListener(selectionHandler);
        setAnimatingRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING);
        setInteractingRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING);
        // Remove default panning and zooming behaviour
        removeInputEventListener(getPanEventHandler());
        removeInputEventListener(getZoomEventHandler());
        // Install custom panning and zooming
        PInputEventFilter panFilter = new PInputEventFilter(InputEvent.BUTTON2_MASK);
        panFilter.setNotMask(InputEvent.CTRL_MASK);
        PPanEventHandler panHandler = new PPanEventHandler();
        panHandler.setEventFilter(panFilter);
        addInputEventListener(panHandler);
        connectionLayer = new ConnectionLayer(this);
        getCamera().addLayer(0, connectionLayer);
        /*
        setZoomEventHandler(new PZoomEventHandler() {
            public void processEvent(final PInputEvent evt, final int i) {
                if (evt.isMouseWheelEvent()) {
                    double currentScale = evt.getCamera().getViewScale();
                    double scaleDelta = 1D - 0.1 * evt.getWheelRotation();
                    double newScale = currentScale * scaleDelta;
                    final Point2D p = evt.getPosition();
                    if (newScale > 0.2 && newScale <= 1.0) {
                        evt.getCamera().scaleViewAboutPoint(scaleDelta, p.getX(), p.getY());
                    }
                }
            }
        });
        */
        addKeyListener(dialogHandler);
        addKeyListener(new DeleteHandler());
        initMenus();
        // This is disabled so we can detect the tab key.
        setFocusTraversalKeysEnabled(false);
    }

    private void initMenus() {
        networkMenu = new JPopupMenu();
        networkMenu.add(new NewNodeAction());
        networkMenu.add(new ResetViewAction());
        popupHandler = new PopupHandler();
        addInputEventListener(popupHandler);
    }

    @Override
    public void setBounds(int i, int i1, int i2, int i3) {
        super.setBounds(i, i1, i2, i3);
        connectionLayer.setBounds(getBounds());
    }

    public Pane getPane() {
        return pane;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        if (this.node == node) return;
        Node oldNode = this.node;
        if (oldNode != null) {
            oldNode.removeDirtyListener(this);
            oldNode.removeNodeChildListener(this);
        }
        this.node = node;
        getLayer().removeAllChildren();
        deselectAll();
        if (node == null) return;
        node.addDirtyListener(this);
        node.addNodeChildListener(this);
        networkError = node.hasError();
        // Add nodes
        for (Node n : node.getChildren()) {
            NodeView nv = new NodeView(this, n);
            getLayer().addChild(nv);
        }
        validate();
        repaint();
        firePropertyChange(NETWORK_PROPERTY, oldNode, node);
    }

    //// View queries ////

    public NodeView getNodeView(Node node) {
        if (node == null) return null;
        for (Object child : getLayer().getChildrenReference()) {
            if (!(child instanceof NodeView)) continue;
            if (((NodeView) child).getNode() == node)
                return (NodeView) child;
        }
        return null;
    }

    //// Selections ////

    public boolean isSelected(Node node) {
        if (node == null) return false;
        NodeView nodeView = getNodeView(node);
        return isSelected(nodeView);
    }

    public boolean isSelected(NodeView nodeView) {
        if (nodeView == null) return false;
        return selection.contains(nodeView);
    }

    public void select(Node node) {
        NodeView nodeView = getNodeView(node);
        select(nodeView);
    }

    /**
     * Only select this node.
     *
     * @param node
     */
    public void singleSelect(Node node) {
        if (node == null) return;
        NodeView nodeView = getNodeView(node);
        singleSelect(nodeView);
    }

    public void singleSelect(NodeView nodeView) {
        if (nodeView == null) return;
        if (selection.size() == 1 && selection.contains(nodeView)) return;
        for (NodeView nv : selection) {
            nv.setSelected(false);
        }
        selection.clear();
        selection.add(nodeView);
        nodeView.setSelected(true);
        firePropertyChange(SELECT_PROPERTY, null, selection);
    }

    public void select(NodeView nodeView) {
        if (nodeView == null) return;
        // If the selection already contained the object, bail out.
        // This is to prevent the select event from firing.
        if (selection.contains(nodeView)) return;
        selection.add(nodeView);
        nodeView.setSelected(true);
        firePropertyChange(SELECT_PROPERTY, null, selection);
    }

    public void select(Set<NodeView> newSelection) {
        boolean selectionChanged = false;
        for (NodeView nodeView : newSelection) {
            if (!selection.contains(nodeView)) {
                selectionChanged = true;
                nodeView.setSelected(true);
                selection.add(nodeView);
            }
        }
        if (selectionChanged)
            firePropertyChange(SELECT_PROPERTY, null, selection);
    }

    public void deselect(NodeView nodeView) {
        if (nodeView == null) return;
        // If the selection didn't contain the object in the first place, bail out.
        // This is to prevent the select event from firing.
        if (!selection.contains(nodeView)) return;
        selection.remove(nodeView);
        nodeView.setSelected(false);
        firePropertyChange(SELECT_PROPERTY, null, selection);
    }

    public void selectAll() {
        boolean selectionChanged = false;
        for (Object child : getLayer().getChildrenReference()) {
            if (!(child instanceof NodeView)) continue;
            NodeView nodeView = (NodeView) child;
            // Check if the selection already contained the node view.
            // If it didn't, that means that the old selection is different
            // from the new selection.
            if (!selection.contains(nodeView)) {
                selectionChanged = true;
                nodeView.setSelected(true);
                selection.add(nodeView);
            }
        }
        if (selectionChanged)
            firePropertyChange(SELECT_PROPERTY, null, selection);
    }

    public void deselectAll() {
        // If the selection was already empty, we don't need to do anything.
        if (selection.isEmpty()) return;
        for (NodeView nodeView : selection) {
            nodeView.setSelected(false);
        }
        selection.clear();
        firePropertyChange(SELECT_PROPERTY, null, selection);
    }

    //// Events ////

    public void childAdded(Node source, Node child) {
        NodeView nv = new NodeView(this, child);
        getLayer().addChild(nv);
    }

    public void childRemoved(Node source, Node child) {
        NodeView nv = getNodeView(child);
        if (nv == null) return;
        getLayer().removeChild(nv);
        if (selection.contains(nv)) {
            deselect(nv);
        }
    }

    public void connectionAdded(Node source, Connection connection) {
        connectionLayer.repaint();
    }

    public void connectionRemoved(Node source, Connection connection) {
        connectionLayer.repaint();
    }

    public void childAttributeChanged(Node source, Node child, NodeAttributeListener.Attribute attribute) {
        NodeView nv = getNodeView(child);
        if (nv == null) return;
        if (attribute == NodeAttributeListener.Attribute.NAME || attribute == NodeAttributeListener.Attribute.IMAGE) {
            nv.repaint();
        } else if (attribute == NodeAttributeListener.Attribute.POSITION) {
            if (!nv.getOffset().equals(child.getPosition().getPoint2D())) {
                nv.setOffset(child.getX(), child.getY());
                connectionLayer.repaint();
            }
            nv.repaint();
        }
    }

    public void renderedChildChanged(Node source, Node child) {
        repaint();
    }

    public void nodeDirty(Node node) {
    }

    public void nodeUpdated(Node node) {
        if (!networkError && !node.hasError()) return;
        networkError = node.hasError();
        repaint();
    }

    //// Node manager ////

    public void showNodeManagerDialog() {
        NodeBoxDocument doc = getPane().getDocument();
        NodeSelectionDialog dialog = new NodeSelectionDialog(doc, doc.getManager());
        Point pt = getMousePosition();
        if (pt == null) {
            pt = new Point((int) (Math.random() * 300), (int) (Math.random() * 300));
        }
        dialog.setVisible(true);
        if (dialog.getSelectedNode() != null) {
            Node n = getNode().create(dialog.getSelectedNode());
            boolean success = smartConnect(getPane().getDocument().getActiveNode(), n);
            if (!success)
                n.setPosition(new net.nodebox.graphics.Point(pt));
            n.setRendered();
            doc.setActiveNode(n);
        }
    }

    /**
     * Try to connect the new node to the active node.
     *
     * @param activeNode the currently selected node
     * @param newNode    the newly created node
     * @return true if a connection could be made.
     */
    private boolean smartConnect(Node activeNode, Node newNode) {
        // Check if there is an active node.
        if (activeNode == null) return false;
        // Check if there are compatible ports on the new node that can be connected
        // to the output of the active node.
        List<Port> compatibles = newNode.getCompatibleInputs(activeNode);
        if (compatibles.size() == 0) return false;
        // Connect the output of the active node to the first compatible input of the new node.
        compatibles.get(0).connect(activeNode);
        // Move the node under the active node.
        newNode.setPosition(activeNode.getX(), activeNode.getY() + 40);
        // Return true to indicate the connection was created successfully.
        return true;
    }

    //// Dragging ////

    /**
     * Change the position of all the selected nodes by adding the delta values to their positions.
     *
     * @param deltaX the change from the original X position.
     * @param deltaY the change from the original Y position.
     */
    public void dragSelection(double deltaX, double deltaY) {
        for (NodeView nv : selection) {
            Point2D pt = nv.getOffset();
            nv.setOffset(pt.getX() + deltaX, pt.getY() + deltaY);
        }
    }

    //// Inner classes ////

    private class SelectionMarker extends PNode {
        public SelectionMarker(Point2D p) {
            setOffset(p);
        }

        protected void paint(PPaintContext c) {
            Graphics2D g = c.getGraphics();
            g.setColor(new Color(200, 200, 200, 100));
            PBounds b = getBounds();
            g.fill(b);
            g.setColor(new Color(100, 100, 100, 100));
            g.draw(b);
        }
    }

    class SelectionHandler extends PBasicInputEventHandler {
        private Set<NodeView> temporarySelection = new HashSet<NodeView>();

        public void mouseClicked(PInputEvent e) {
            if (e.getButton() != MouseEvent.BUTTON1) return;
            deselectAll();
        }

        public void mousePressed(PInputEvent e) {
            if (e.getButton() != MouseEvent.BUTTON1) return;
            Point2D p = e.getPosition();
            selectionMarker = new SelectionMarker(p);
            getLayer().addChild(selectionMarker);
            temporarySelection.clear();
        }

        public void mouseDragged(PInputEvent e) {
            if (selectionMarker == null) return;
            Point2D prev = selectionMarker.getOffset();
            Point2D p = e.getPosition();
            selectionMarker.setWidth(p.getX() - prev.getX());
            selectionMarker.setHeight(p.getY() - prev.getY());
            ListIterator childIter = getLayer().getChildrenIterator();
            temporarySelection.clear();
            while (childIter.hasNext()) {
                Object o = childIter.next();
                if (o instanceof NodeView) {
                    NodeView nodeView = (NodeView) o;
                    PNode n = (PNode) o;
                    if (selectionMarker.getFullBounds().intersects(n.getFullBounds())) {
                        nodeView.setSelected(true);
                        temporarySelection.add(nodeView);
                    } else {
                        nodeView.setSelected(false);
                    }
                }
            }
        }

        public void mouseReleased(PInputEvent pInputEvent) {
            if (selectionMarker == null) return;
            getLayer().removeChild(selectionMarker);
            selectionMarker = null;
            select(temporarySelection);
            temporarySelection.clear();
        }
    }

    private class DialogHandler extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_TAB) {
                showNodeManagerDialog();
            }
        }
    }

    private class DeleteHandler extends KeyAdapter {
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                for (NodeView nodeView : selection) {
                    node.remove(nodeView.getNode());
                }
            }
        }
    }

    private class PopupHandler extends PBasicInputEventHandler {
        public void processEvent(PInputEvent e, int i) {
            if (!e.isPopupTrigger()) return;
            if (e.isHandled()) return;
            Point2D p = e.getCanvasPosition();
            networkMenu.show(NetworkView.this, (int) p.getX(), (int) p.getY());
        }

    }

    private class NewNodeAction extends AbstractAction {
        public NewNodeAction() {
            super("Create New Node...");
        }

        public void actionPerformed(ActionEvent e) {
            showNodeManagerDialog();
        }
    }

    private class ResetViewAction extends AbstractAction {
        private ResetViewAction() {
            super("Reset View");
        }

        public void actionPerformed(ActionEvent e) {
            getCamera().setViewTransform(new AffineTransform());
        }
    }
}
