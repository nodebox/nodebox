package nodebox.client;

import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.*;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PPaintContext;
import nodebox.node.Node;
import nodebox.node.Port;
import nodebox.ui.PaneView;
import nodebox.ui.Platform;
import nodebox.ui.Theme;

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

public class NetworkView extends PCanvas implements PaneView, KeyListener {

    public static final String SELECT_PROPERTY = "NetworkView.select";
    public static final String HIGHLIGHT_PROPERTY = "highlight";
    public static final String RENDER_PROPERTY = "render";
    public static final String NETWORK_PROPERTY = "network";

    public static final float MIN_ZOOM = 0.2f;
    public static final float MAX_ZOOM = 1.0f;

    private final NodeBoxDocument document;

    private static Cursor defaultCursor, panCursor;

    private Set<NodeView> selection = new HashSet<NodeView>();
    private ConnectionLayer connectionLayer;
    private SelectionMarker selectionMarker;
    private JPopupMenu networkMenu;
    private NodeView connectionSource, connectionTarget;
    private Point2D connectionPoint;

    private boolean panEnabled = false;

    static {
        Image panCursorImage;

        try {
            if (Platform.onWindows())
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

    public NetworkView(NodeBoxDocument document) {
        this.document = document;
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
        addInputEventListener(new DoubleClickHandler());
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

    public Node getActiveNetwork() {
        return document.getActiveNetwork();
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
        if (getActiveNetwork() == null) return;
        // Add nodes
        for (Node n : getActiveNetwork().getChildren()) {
            NodeView nv = new NodeView(this, n.getName());
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
        updateConnections();
    }

    public Node getActiveNode() {
        return document.getActiveNode();
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
            nodebox.graphics.Point pt = nv.getNode().getPosition();
            Rectangle2D r = new Rectangle2D.Double(pt.x, pt.y, NodeView.NODE_FULL_SIZE, NodeView.NODE_FULL_SIZE);
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
        document.setActiveNode(nodeView.getNode());
    }

    public void select(Iterable<Node> nodes) {
        Set<NodeView> nodeViews = nodesToNodeViews(nodes);
        select(nodeViews);
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

    private Set<NodeView> nodesToNodeViews(Iterable<Node> nodes) {
        Set<NodeView> nodeViews = new HashSet<NodeView>();
        for (Node node : nodes) {
            nodeViews.add(getNodeView(node));
        }
        return nodeViews;
    }

    private Set<Node> nodeViewsToNodes(Iterable<NodeView> nodeViews) {
        Set<Node> nodes = new HashSet<Node>();
        for (NodeView nodeView : nodeViews) {
            nodes.add(nodeView.getNode());
        }
        return nodes;
    }

    public List<Node> getSelectedNodes() {
        ArrayList<Node> nodes = new ArrayList<Node>();
        for (NodeView nv : selection) {
            nodes.add(nv.getNode());
        }
        return nodes;
    }

    public boolean hasSelectedConnection() {
        return connectionLayer.hasSelection();
    }

    public void deleteSelectedConnection() {
        if (hasSelectedConnection())
            connectionLayer.deleteSelected();
    }

    //// Events ////

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
     * @param outputNode The output node.
     * @param inputNode  The input node.
     * @param inputPort  The input port.
     */
    public void connect(Node outputNode, Node inputNode, Port inputPort) {
        getDocument().connect(outputNode, inputNode, inputPort);
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
        JOptionPane.showMessageDialog(this, "Child nodes are not supported yet.");
//        getDocument().goUp();
    }

    private void goDown() {
        JOptionPane.showMessageDialog(this, "Child nodes are not supported yet.");
//        if (selection.size() != 1) {
//            Toolkit.getDefaultToolkit().beep();
//            return;
//        }
//        NodeView selectedNode = selection.iterator().next();
//
//        String childPath = Node.path(getDocument().getActiveNetworkPath(), selectedNode.getNodeName());
//        getDocument().setActiveNetwork(childPath);
    }

    //// Other node operations ////

    public void keyTyped(KeyEvent e) {
    }

    public void keyPressed(KeyEvent e) {
        panEnabled = e.getKeyCode() == KeyEvent.VK_SPACE;
        if (panEnabled && !getCursor().equals(panCursor))
            setCursor(panCursor);
    }

    public void keyReleased(KeyEvent e) {
        panEnabled = false;
        if (!getCursor().equals(defaultCursor))
            setCursor(defaultCursor);
    }

    public  boolean isPanning() {
        return panEnabled;
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
            getDocument().setActiveNode((Node) null);
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
                document.showNodeSelectionDialog();
            }
        }
    }

    private class DeleteHandler extends KeyAdapter {
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                getDocument().deleteSelection();
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

    private class DoubleClickHandler extends PBasicInputEventHandler {
        @Override
        public void processEvent(PInputEvent e, int i) {
            if (e.getClickCount() != 2) return;
            NodeView view = getNodeViewAt(e.getPosition());
            if (view != null || e.isHandled()) return;
            e.setHandled(true);
            document.showNodeSelectionDialog();
        }
    }

    private class NewNodeAction extends AbstractAction {
        public NewNodeAction() {
            super("New Node...");
        }

        public void actionPerformed(ActionEvent e) {
            document.showNodeSelectionDialog();
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

}
