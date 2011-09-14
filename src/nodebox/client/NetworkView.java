package nodebox.client;

import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.*;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PPaintContext;
import nodebox.node.Node;
import nodebox.node.NodeLibrary;
import nodebox.node.Port;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class NetworkView extends PCanvas implements PaneView, CodeChangeListener, KeyListener {

    public static final String SELECT_PROPERTY = "NetworkView.select";
    public static final String HIGHLIGHT_PROPERTY = "highlight";
    public static final String RENDER_PROPERTY = "render";
    public static final String NETWORK_PROPERTY = "network";

    public static final float MIN_ZOOM = 0.2f;
    public static final float MAX_ZOOM = 1.0f;
    
    private NodeBoxDocument document;
    private Node activeNetwork;
    private Node activeNode;

    private static Cursor defaultCursor, panCursor;

    private Set<NodeView> selection = new HashSet<NodeView>();
    private ConnectionLayer connectionLayer;
    private SelectionMarker selectionMarker;
    private JPopupMenu networkMenu;
    private NodeView connectionSource, connectionTarget;
    private Point2D connectionPoint;
    private Delegate delegate;

    private boolean panEnabled = false;

    static {
        Image panCursorImage;

        try {
            if (PlatformUtils.onWindows())
                panCursorImage = ImageIO.read(new File("res/view-cursor-pan-32.png"));
            else
                panCursorImage = ImageIO.read(new File("res/view-cursor-pan.png"));
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            panCursor = toolkit.createCustomCursor(panCursorImage, new Point(0, 0), "PanCursor");
            defaultCursor = Cursor.getDefaultCursor();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public NetworkView() {
        setBackground(Theme.NETWORK_BACKGROUND_COLOR);
        SelectionHandler selectionHandler = new SelectionHandler();
        addInputEventListener(selectionHandler);
        setAnimatingRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING);
        setInteractingRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING);
        // Remove default panning and zooming behaviour
        removeInputEventListener(getPanEventHandler());
        removeInputEventListener(getZoomEventHandler());
        // Install custom panning and zooming
        PInputEventFilter panFilter = new PInputEventFilter();
        panFilter.setNotMask(InputEvent.CTRL_MASK);
        PPanEventHandler panHandler = new PPanEventHandler() {
            public void processEvent(final PInputEvent evt, final int i) {
                if (evt.isMouseEvent() && evt.isLeftMouseButton() && panEnabled)
                    super.processEvent(evt, i);
            }
        };
        panHandler.setAutopan(false);
        panHandler.setEventFilter(panFilter);
        addInputEventListener(panHandler);
        connectionLayer = new ConnectionLayer(this);
        getCamera().addLayer(0, connectionLayer);
        setZoomEventHandler(new PZoomEventHandler() {
            public void processEvent(final PInputEvent evt, final int i) {
                if (evt.isMouseWheelEvent()) {
                    double currentScale = evt.getCamera().getViewScale();
                    double scaleDelta = 1D - 0.1 * evt.getWheelRotation();
                    double newScale = currentScale * scaleDelta;
                    if (newScale < MIN_ZOOM) {
                        scaleDelta = MIN_ZOOM / currentScale;
                    } else if (newScale > MAX_ZOOM) {
                        scaleDelta = MAX_ZOOM / currentScale;
                    }
                    final Point2D p = evt.getPosition();
                    evt.getCamera().scaleViewAboutPoint(scaleDelta, p.getX(), p.getY());
                }
            }
        });
        DialogHandler dialogHandler = new DialogHandler();
        addKeyListener(dialogHandler);
        addKeyListener(new DeleteHandler());
        addKeyListener(new UpDownHandler());
        addKeyListener(this);
        initMenus();
        // This is disabled so we can detect the tab key.
        setFocusTraversalKeysEnabled(false);
    }

    private void initMenus() {
        networkMenu = new JPopupMenu();
        networkMenu.add(new NewNodeAction());
        networkMenu.add(new ResetViewAction());
        networkMenu.add(new GoUpAction());
        PopupHandler popupHandler = new PopupHandler();
        addInputEventListener(popupHandler);
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        connectionLayer.setBounds(getBounds());
    }

    public NodeBoxDocument getDocument() {
        return document;
    }

    // TODO Turn this into a delegate.
    public void setDocument(NodeBoxDocument document) {
        this.document = document;
    }

    public Node getActiveNetwork() {
        return activeNetwork;
    }

    /**
     * Refresh the nodes and connections cache.
     */
    public void updateAll() {
        updateNodes();
        updateConnections();
    }

    public void updateNodes() {
        getLayer().removeAllChildren();
        deselectAll();
        if (activeNetwork == null) return;
        // Add nodes
        for (Node n : activeNetwork.getChildren()) {
            NodeView nv = new NodeView(this, n);
            getLayer().addChild(nv);
        }
        // TODO: Do we need validate?
        validate();
        repaint();
    }

    public void updateConnections() {
        connectionLayer.repaint();
    }

    public void updatePosition(Node node) {
        NodeView nv = getNodeView(node);
        if (nv == null) return;
        // TODO Make this work.
        //nv.setOffset(node.getX(), node.getY());
        updateConnections();
    }

    public void setActiveNetwork(Node activeNetwork) {
        this.activeNetwork = activeNetwork;
        updateNodes();
    }

    public Node getActiveNode() {
        return activeNode;
    }

    public void setActiveNode(Node activeNode) {
        this.activeNode = activeNode;
        singleSelect(activeNode);
        repaint();
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

    public NodeView getNodeViewAt(Point2D point) {
        for (Object child : getLayer().getChildrenReference()) {
            if (!(child instanceof NodeView)) continue;
            NodeView nv = (NodeView) child;
            Rectangle2D r = new Rectangle2D.Double(nv.getNode().getX(), nv.getNode().getY(), NodeView.NODE_FULL_SIZE, NodeView.NODE_FULL_SIZE);
            if (r.contains(point)) {
                return nv;
            }
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
        return nodeView != null && selection.contains(nodeView);
    }

    public void select(Node node) {
        NodeView nodeView = getNodeView(node);
        addToSelection(nodeView);
    }

    /**
     * Select this node, and only this node.
     * <p/>
     * All other selected nodes will be deselected.
     *
     * @param node The node to select. If node is null, everything is deselected.
     */
    public void singleSelect(Node node) {
        NodeView nodeView = getNodeView(node);
        singleSelect(nodeView);
    }

    /**
     * Select this node view, and only this node view.
     * <p/>
     * All other selected nodes will be deselected.
     *
     * @param nodeView The node view to select or null to deselect everything.
     */
    public void singleSelect(NodeView nodeView) {
        connectionLayer.deselect();
        if (nodeView == null) return;
        if (selection.size() == 1 && selection.contains(nodeView)) return;
        for (NodeView nv : selection) {
            nv.setSelected(false);
        }
        selection.clear();
        selection.add(nodeView);
        nodeView.setSelected(true);
        firePropertyChange(SELECT_PROPERTY, null, selection);
        delegate.activeNodeChanged(nodeView.getNode());
    }

    public void select(Set<NodeView> newSelection) {
        boolean selectionChanged = false;
        ArrayList<NodeView> nodeViewsToRemove = new ArrayList<NodeView>();
        for (NodeView nodeView : selection) {
            if (!newSelection.contains(nodeView)) {
                selectionChanged = true;
                nodeView.setSelected(false);
                nodeViewsToRemove.add(nodeView);
            }
        }
        for (NodeView nodeView : nodeViewsToRemove) {
            selection.remove(nodeView);
        }
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

    public void addToSelection(NodeView nodeView) {
        if (nodeView == null) return;
        // If the selection already contained the object, bail out.
        // This is to prevent the select event from firing.
        if (selection.contains(nodeView)) return;
        selection.add(nodeView);
        nodeView.setSelected(true);
        firePropertyChange(SELECT_PROPERTY, null, selection);
    }

    public void addToSelection(Set<NodeView> newSelection) {
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
        connectionLayer.deselect();
    }

    private Set<Node> nodeViewsToNodes(Iterable<NodeView> nodeViews) {
        Set<Node> nodes = new HashSet<Node>();
        for (NodeView nodeView : nodeViews) {
            nodes.add(nodeView.getNode());
        }
        return nodes;
    }

    public void deleteSelected() {
        Set<NodeView> nodeViews = new HashSet<NodeView>(selection);
        Set<Node> nodes = nodeViewsToNodes(nodeViews);
        getDocument().removeNodes(nodes);
        connectionLayer.deleteSelected();
    }

    public void cutSelected() {
        copySelected();
        List<Node> nodes = getSelectedNodes();
        for (Node n : nodes) {
            getDocument().removeNode(n);
        }
    }

    public void copySelected() {
        // When copying, create copies of all the nodes and store them under a new parent.
        // The parent is used to preserve the connections, and also to save the state of the
        // copied nodes.
        // This parent is the root of a new library.
        NodeLibrary clipboardLibrary = new NodeLibrary("clipboard");
        Node clipboardRoot = clipboardLibrary.getRootNode();
        getDocument().copyChildren(getSelectedNodes(), getActiveNetwork(), clipboardRoot);
        Application.getInstance().setNodeClipboard(clipboardLibrary);
    }

    public void pasteSelected() {
        Node newParent = getActiveNetwork();
        NodeLibrary clipboardLibrary = Application.getInstance().getNodeClipboard();
        if (clipboardLibrary == null) return;
        Node clipboardRoot = clipboardLibrary.getRootNode();
        if (clipboardRoot.size() == 0) return;
        Collection<Node> newNodes = getDocument().copyChildren(clipboardRoot.getChildren(), clipboardRoot, newParent);
        deselectAll();
        for (Node newNode : newNodes) {
            nodebox.graphics.Point pt = newNode.getPosition();
            pt.x += 5;
            pt.y += 5;
            newNode.setPosition(pt);
            NodeView nv = getNodeView(newNode);
            assert nv != null;
            nv.updateIcon();
            addToSelection(nv);
        }
    }

    private List<Node> getSelectedNodes() {
        ArrayList<Node> nodes = new ArrayList<Node>();
        for (NodeView nv : selection) {
            nodes.add(nv.getNode());
        }
        return nodes;
    }

    //// Events ////

    public void childAdded(Node child) {
        NodeView nv = new NodeView(this, child);
        getLayer().addChild(nv);
    }

    public void childRemoved(Node child) {
        NodeView nv = getNodeView(child);
        if (nv == null) return;
        getLayer().removeChild(nv);
        if (selection.contains(nv)) {
            deselect(nv);
        }
        // If this child was connected, it is now disconnected.
        // This means we should repaint the connection layer.
        connectionLayer.repaint();
    }

    public void childAttributeChanged(Node child, Node.Attribute attribute) {
        NodeView nv = getNodeView(child);
        if (nv == null) return;
        // The port is part of the icon. If a port was added or removed, also update the icon.
        if (attribute == Node.Attribute.PORT) {
            nv.updateIcon();
        } else if (attribute == Node.Attribute.NAME
                || attribute == Node.Attribute.IMAGE) {
            // When visual attributes change, repaint the node view.
            nv.repaint();
        } else if (attribute == Node.Attribute.POSITION) {
            // When the position changes, change the node view offset, and also repaint the connections.
            if (!nv.getOffset().equals(child.getPosition().getPoint2D())) {
                nv.setOffset(child.getX(), child.getY());
            }
            nv.repaint();
            connectionLayer.repaint();
        }
    }

    public void checkErrorAndRepaint() {
        //if (!networkError && !activeNetwork.hasError()) return;
        //networkError = activeNetwork.hasError();
        repaint();
    }

    public void codeChanged(Node node, boolean changed) {
        NodeView nv = getNodeView(node);
        if (nv == null) return;
        nv.setCodeChanged(changed);
        repaint();
    }

    //// Node manager ////

    // TODO move to the document.
    public void showNodeSelectionDialog() {
        NodeBoxDocument doc = getDocument();
        NodeSelectionDialog dialog = new NodeSelectionDialog(doc, doc.getNodeLibrary(), doc.getManager());
        Point pt = getMousePosition();
        if (pt == null) {
            pt = new Point((int) (Math.random() * 300), (int) (Math.random() * 300));
        }
        pt = (Point) getCamera().localToView(pt);
        dialog.setVisible(true);
        if (dialog.getSelectedNode() != null) {
            doc.createNode(dialog.getSelectedNode(), pt);
        }
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

    //// Connections ////

    /**
     * This method gets called when we start dragging a connection line from a node view.
     *
     * @param connectionSource the node view where we start from.
     */
    public void startConnection(NodeView connectionSource) {
        this.connectionSource = connectionSource;
    }

    /**
     * This method gets called from the NodeView to connect the output port to the input port.
     *
     * @param output the output port
     * @param input  the input port
     */
    public void connect(Port output, Port input) {
        getDocument().connect(output, input);
    }

    /**
     * This method gets called when a dragging operation ends.
     * <p/>
     * We don't care if a connection was established or not.
     */
    public void endConnection() {
        NodeView oldTarget = this.connectionTarget;
        this.connectionSource = null;
        connectionTarget = null;
        connectionPoint = null;
        if (oldTarget != null)
            oldTarget.repaint();
        connectionLayer.repaint();
    }

    /**
     * Return true if we are in the middle of a connection drag operation.
     *
     * @return true if we are connecting nodes together.
     */
    public boolean isConnecting() {
        return connectionSource != null;
    }

    /**
     * NodeView calls this method to indicate that the mouse was dragged while connecting.
     * <p/>
     * This method updates the point and redraws the connection layer.
     *
     * @param pt the new mouse location.
     */
    public void dragConnectionPoint(Point2D pt) {
        assert isConnecting();
        this.connectionPoint = pt;
        connectionLayer.repaint();
    }

    /**
     * NodeView calls this method to indicate that the new target is now the given node view.
     *
     * @param target the new NodeView target.
     */
    public void setTemporaryConnectionTarget(NodeView target) {
        NodeView oldTarget = this.connectionTarget;
        this.connectionTarget = target;
        if (oldTarget != null)
            oldTarget.repaint();
        if (connectionTarget != null)
            connectionTarget.repaint();
    }

    public NodeView getConnectionSource() {
        return connectionSource;
    }

    public NodeView getConnectionTarget() {
        return connectionTarget;
    }

    public Point2D getConnectionPoint() {
        return connectionPoint;
    }

    //// Network navigation ////

    private void goUp() {
        if (activeNetwork.getParent() == null) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        getDocument().setActiveNetwork(activeNetwork.getParent());
    }

    private void goDown() {
        if (selection.size() != 1) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        NodeView selectedNode = selection.iterator().next();
        getDocument().setActiveNetwork(selectedNode.getNode());
    }

    //// Other node operations ////

    public void setRenderedNode(Node node) {
        getDocument().setRenderedNode(node);
    }

    public void removeNode(Node node) {
        getDocument().removeNode(node);
    }

    public void setNodePosition(Node node, nodebox.graphics.Point point) {
        getDocument().setNodePosition(node, point);
    }

    public void keyTyped(KeyEvent e) {
    }

    public void keyPressed(KeyEvent e) {
        panEnabled = e.getKeyCode() == KeyEvent.VK_SPACE;
        if (panEnabled && ! getCursor().equals(panCursor))
            setCursor(panCursor);
    }

    public void keyReleased(KeyEvent e) {
        panEnabled = false;
        if (! getCursor().equals(defaultCursor))
            setCursor(defaultCursor);
    }


    //// Inner classes ////

    private class SelectionMarker extends PNode {
        public SelectionMarker(Point2D p) {
            setOffset(p);
        }

        protected void paint(PPaintContext c) {
            Graphics2D g = c.getGraphics();
            g.setColor(Theme.NETWORK_SELECTION_COLOR);
            PBounds b = getBounds();
            // Inset the bounds so we don't draw outside the refresh region.
            b.inset(1, 1);
            g.fill(b);
            g.setColor(Theme.NETWORK_SELECTION_BORDER_COLOR);
            g.draw(b);
        }
    }

    class SelectionHandler extends PBasicInputEventHandler {
        private Set<NodeView> temporarySelection = new HashSet<NodeView>();

        public void mouseClicked(PInputEvent e) {
            if (e.getButton() != MouseEvent.BUTTON1) return;
            deselectAll();
            getDocument().setActiveNode(null);
            connectionLayer.mouseClickedEvent(e);
        }

        public void mousePressed(PInputEvent e) {
            if (e.getButton() != MouseEvent.BUTTON1) return;
            temporarySelection.clear();
            // Make sure no Node View is under the mouse cursor.
            // In that case, we're not selecting, but moving a node.
            Point2D p = e.getPosition();
            NodeView nv = getNodeViewAt(p);
            if (nv == null) {
                selectionMarker = new SelectionMarker(p);
                getLayer().addChild(selectionMarker);
            } else {
                selectionMarker = null;
            }
        }

        public void mouseDragged(PInputEvent e) {
            if (selectionMarker == null) return;
            Point2D prev = selectionMarker.getOffset();
            Point2D p = e.getPosition();
            double width = p.getX() - prev.getX();
            double absWidth = Math.abs(width);
            double height = p.getY() - prev.getY();
            double absHeight = Math.abs(height);
            selectionMarker.setWidth(absWidth);
            selectionMarker.setHeight(absHeight);
            selectionMarker.setX(absWidth != width ? width : 0);
            selectionMarker.setY(absHeight != height ? height : 0);
            ListIterator childIter = getLayer().getChildrenIterator();
            connectionLayer.deselect();
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

        public void mouseReleased(PInputEvent e) {
            if (selectionMarker == null) return;
            getLayer().removeChild(selectionMarker);
            selectionMarker = null;
            select(temporarySelection);
            temporarySelection.clear();
        }
    }

    private class UpDownHandler extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_U:
                    goUp();
                    break;
                case KeyEvent.VK_ENTER:
                    goDown();
                    break;
            }
        }
    }

    private class DialogHandler extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_TAB) {
                showNodeSelectionDialog();
            }
        }
    }

    private class DeleteHandler extends KeyAdapter {
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                deleteSelected();
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
            super("New Node...");
        }

        public void actionPerformed(ActionEvent e) {
            showNodeSelectionDialog();
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

    private class GoUpAction extends AbstractAction {
        private GoUpAction() {
            super("Go Up");
            putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_U, 0));
        }

        public void actionPerformed(ActionEvent e) {
            goUp();
        }
    }

    public Delegate getDelegate() {
        return delegate;
    }

    public void setDelegate(Delegate delegate) {
        this.delegate = delegate;
    }

    /**
     * A callback interface for listening to changes in the network view.
     */
    public static interface Delegate {

        /**
         * Callback method invoked when the active node was changed.
         *
         * @param node The new active node.
         */
        public void activeNodeChanged(Node node);

    }

}
