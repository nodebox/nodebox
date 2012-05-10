package nodebox.client;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import nodebox.node.Connection;
import nodebox.node.Node;
import nodebox.node.Port;
import nodebox.ui.PaneView;
import nodebox.ui.Platform;
import nodebox.ui.Theme;
import org.python.google.common.collect.ImmutableList;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class NetworkView extends JComponent implements PaneView, KeyListener {

    public static final int GRID_CELL_SIZE = 40;
    public static final int NODE_WIDTH = GRID_CELL_SIZE * 4 - 10;
    public static final int NODE_HEIGHT = GRID_CELL_SIZE - 10;
    public static final int PORT_WIDTH = 10;
    public static final int PORT_HEIGHT = 3;
    public static final int PORT_SPACING = 10;
    public static final Dimension NODE_DIMENSION = new Dimension(NODE_WIDTH, NODE_HEIGHT);

    public static final String SELECT_PROPERTY = "NetworkView.select";
    public static final String HIGHLIGHT_PROPERTY = "highlight";
    public static final String RENDER_PROPERTY = "render";
    public static final String NETWORK_PROPERTY = "network";

    public static final float MIN_ZOOM = 0.2f;
    public static final float MAX_ZOOM = 1.0f;

    private final NodeBoxDocument document;

    private static Cursor defaultCursor, panCursor;

    private Set<String> selectedNodes = new HashSet<String>();

    //private SelectionMarker selectionMarker;
    private JPopupMenu networkMenu;
    private NodeView connectionSource, connectionTarget;
    private Point2D connectionPoint;
    private AffineTransform viewTransform = new AffineTransform();

    // Interaction state
    private boolean isDraggingNodes = false;
    private boolean isMakingConnection = false;
    private boolean isPanningView = false;
    private boolean isShiftPressed = false;
    private int dragStartX = 0;
    private int dragStartY = 0;
    private ImmutableMap<String, nodebox.graphics.Point> dragPositions = ImmutableMap.of();

    private boolean panEnabled = false;
    public static final Color NODE_BACKGROUND_COLOR = new Color(123, 154, 152);
    private boolean startDragging;
    private Point2D dragStartPoint;


    public static final Map<String, Color> PORT_COLORS = Maps.newHashMap();
    public static final Color DEFAULT_PORT_COLOR = Color.WHITE;


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

        PORT_COLORS.put(Port.TYPE_INT, Color.GRAY);
        PORT_COLORS.put(Port.TYPE_FLOAT, Color.GRAY);
        PORT_COLORS.put(Port.TYPE_STRING, Color.LIGHT_GRAY);
        PORT_COLORS.put(Port.TYPE_BOOLEAN, Color.DARK_GRAY);
        PORT_COLORS.put(Port.TYPE_POINT, Color.RED);
        PORT_COLORS.put(Port.TYPE_COLOR, Color.CYAN);
        PORT_COLORS.put("geometry", Color.BLUE);

    }

    public NetworkView(NodeBoxDocument document) {
        this.document = document;
        setBackground(Theme.NETWORK_BACKGROUND_COLOR);
        DialogHandler dialogHandler = new DialogHandler();
        addKeyListener(dialogHandler);
        addKeyListener(new DeleteHandler());
        addKeyListener(new UpDownHandler());
        addKeyListener(this);
        initMenus();
        // This is disabled so we can detect the tab key.
        setFocusTraversalKeysEnabled(false);
        enableEvents(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);
    }


    private void initMenus() {
        networkMenu = new JPopupMenu();
        networkMenu.add(new NewNodeAction());
        networkMenu.add(new ResetViewAction());
        networkMenu.add(new GoUpAction());
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
        repaint();
    }

    public void updateConnections() {
        repaint();
    }

    public void updatePosition(Node node) {
        updateConnections();
    }

    public Node getActiveNode() {
        return document.getActiveNode();
    }

    private Iterable<Node> getNodes() {
        return getDocument().getActiveNetwork().getChildren();
    }

    private Iterable<Connection> getConnections() {
        return getDocument().getActiveNetwork().getConnections();
    }

    //// Painting the nodes ////

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        // Draw background
        g2.setColor(Theme.NETWORK_BACKGROUND_COLOR);
        g2.fill(g.getClipBounds());

        // Paint the grid
        // (The grid is not really affected by the view transform)
        paintGrid(g2);

        // Set the view transform
        AffineTransform originalTransform = g2.getTransform();
        g2.transform(viewTransform);

        paintConnections(g2);
        paintNodes(g2);

        // Restore original transform
        g2.setTransform(originalTransform);
    }

    private void paintGrid(Graphics2D g) {
        g.setColor(Theme.NETWORK_GRID_COLOR);
        int transformOffsetX = (int) (viewTransform.getTranslateX() % GRID_CELL_SIZE);
        int transformOffsetY = (int) (viewTransform.getTranslateY() % GRID_CELL_SIZE);

        for (int y = -GRID_CELL_SIZE; y < getHeight() + GRID_CELL_SIZE; y += GRID_CELL_SIZE) {
            for (int x = -GRID_CELL_SIZE; x < getWidth() + GRID_CELL_SIZE; x += GRID_CELL_SIZE) {
                paintGridCross(g, x - 5 + transformOffsetX, y - 5 + transformOffsetY);
            }
        }
    }

    private void paintGridCross(Graphics2D g, int x, int y) {
        g.drawLine(x - 2, y, x + 2, y);
        g.drawLine(x, y - 2, x, y + 2);
    }

    private void paintConnections(Graphics2D g) {
        g.setColor(Theme.CONNECTION_DEFAULT_COLOR);
        g.setStroke(new BasicStroke(3));
        for (Connection connection : getConnections()) {
            paintConnection(g, connection);
        }
    }

    private void paintConnection(Graphics2D g, Connection connection) {
        Node outputNode = findNodeWithName(connection.getOutputNode());
        Node inputNode = findNodeWithName(connection.getInputNode());
        Rectangle outputRect = nodeRect(outputNode);
        Rectangle inputRect = nodeRect(inputNode);
        g.drawLine(outputRect.x + 4, outputRect.y + outputRect.height, inputRect.x + 4, inputRect.y);
    }

    private Node findNodeWithName(String name) {
        return getActiveNetwork().getChild(name);
    }

    private void paintNodes(Graphics2D g) {
        g.setColor(Theme.NETWORK_NODE_NAME_COLOR);
        for (Node node : getNodes()) {
            paintNode(g, node, isSelected(node), isRendered(node));
        }
    }

    private void paintNode(Graphics2D g, Node node, boolean selected, boolean rendered) {
        Rectangle r = nodeRect(node);
        if (selected) {
            g.setColor(Color.WHITE);
            g.fillRect(r.x, r.y, NODE_WIDTH, NODE_HEIGHT);
            g.setColor(NODE_BACKGROUND_COLOR);
            g.fillRect(r.x + 2, r.y + 2, NODE_WIDTH - 4, NODE_HEIGHT - 4);
        } else {
            g.setColor(NODE_BACKGROUND_COLOR);
            g.fillRect(r.x, r.y, NODE_WIDTH, NODE_HEIGHT);
        }

        if (rendered) {
            g.setColor(Color.WHITE);
            GeneralPath gp = new GeneralPath();
            gp.moveTo(r.x + NODE_WIDTH - 2, r.y + NODE_HEIGHT - 20);
            gp.lineTo(r.x + NODE_WIDTH - 2, r.y + NODE_HEIGHT - 2);
            gp.lineTo(r.x + NODE_WIDTH - 20, r.y + NODE_HEIGHT - 2);
            g.fill(gp);
        }

        // Draw input ports
        g.setColor(Color.WHITE);
        List<Port> inputs = node.getInputs();
        int portX = 0;
        for (Port input : node.getInputs()) {
            Color portColor = PORT_COLORS.get(input.getType());
            if (portColor == null) {
                portColor = DEFAULT_PORT_COLOR;
            }
            g.setColor(portColor);
            g.fillRect(r.x + portX, r.y - PORT_HEIGHT, PORT_WIDTH, PORT_HEIGHT);
            portX += PORT_WIDTH + PORT_SPACING;
        }

        // Draw output port
        if (selected) {
            g.setColor(Color.WHITE);
        } else {
            g.setColor(NODE_BACKGROUND_COLOR);
        }
        g.fillRect(r.x, r.y + NODE_HEIGHT, PORT_WIDTH, PORT_HEIGHT);

        g.setColor(Color.WHITE);
        g.fillRect(r.x + 5, r.y + 5, NODE_HEIGHT - 10, NODE_HEIGHT - 10);
        g.setColor(Color.WHITE);
        g.drawString(node.getName(), r.x + 30, r.y + 20);
    }


    private Rectangle nodeRect(Node node) {
        return new Rectangle(nodePoint(node), NODE_DIMENSION);
    }

    private Point nodePoint(Node node) {
        int nodeX = ((int) node.getPosition().getX()) * GRID_CELL_SIZE;
        int nodeY = ((int) node.getPosition().getY()) * GRID_CELL_SIZE;
        return new Point(nodeX, nodeY);
    }

    //// View Transform ////

    private void resetViewTransform() {
        viewTransform = new AffineTransform();
        repaint();
    }

    //// Interaction ////

    private ImmutableMap<String, nodebox.graphics.Point> selectedNodePositions() {
        ImmutableMap.Builder<String, nodebox.graphics.Point> b = ImmutableMap.builder();
        for (String nodeName : selectedNodes) {
            b.put(nodeName, findNodeWithName(nodeName).getPosition());
        }
        return b.build();
    }

    private Point2D inverseViewTransformPoint(Point p) {
        Point2D pt = new Point2D.Double(p.getX(), p.getY());
        try {
            return viewTransform.inverseTransform(pt, null);
        } catch (NoninvertibleTransformException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void processMouseEvent(MouseEvent e) {
        Point2D pt = inverseViewTransformPoint(e.getPoint());
        if (e.getID() == MouseEvent.MOUSE_PRESSED) {
            Node pressedNode = getNodeAt(pt);
            if (pressedNode != null) {
                // Don't immediately set "isDragging."
                // We wait until we actually drag the first time to do the work.
                startDragging = true;
            }
            if (isPanningView) {
                // When panning the view use the original mouse point, not the one affected by the view transform.
                dragStartPoint = e.getPoint();
            }
        } else if (e.getID() == MouseEvent.MOUSE_RELEASED) {
            isDraggingNodes = false;
        } else if (e.getID() == MouseEvent.MOUSE_CLICKED) {
            if (e.getClickCount() == 1 && e.getButton() == MouseEvent.BUTTON1) {
                Node clickedNode = getNodeAt(pt);
                if (clickedNode == null) {
                    deselectAll();
                } else {
                    if (isShiftPressed) {
                        toggleSelection(clickedNode);
                    } else {
                        singleSelect(clickedNode);
                    }
                }
            } else if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                Node clickedNode = getNodeAt(pt);
                if (clickedNode == null) {
                    // showNodeSelectionDialog();
                } else {
                    document.setRenderedNode(clickedNode);
                }
            }
        }
    }

    private Point2D minPoint(Point2D a, Point2D b) {
        return new Point2D.Double(a.getX() - b.getX(), a.getY() - b.getY());
    }

    @Override
    protected void processMouseMotionEvent(MouseEvent e) {
        Point2D pt = inverseViewTransformPoint(e.getPoint());
        if (e.getID() == MouseEvent.MOUSE_DRAGGED) {
            // Panning the view has the first priority.
            if (isPanningView) {
                // When panning the view use the original mouse point, not the one affected by the view transform.
                Point2D offset = minPoint(e.getPoint(), dragStartPoint);
                viewTransform.translate(offset.getX(), offset.getY());
                dragStartPoint = e.getPoint();
                repaint();
                return;
            }


            if (startDragging) {
                startDragging = false;
                Node pressedNode = getNodeAt(pt);
                if (pressedNode != null) {
                    if (selectedNodes.isEmpty() || !selectedNodes.contains(pressedNode.getName())) {
                        singleSelect(pressedNode);
                    }
                    isDraggingNodes = true;
                    dragPositions = selectedNodePositions();
                    dragStartPoint = pt;
                } else {
                    isDraggingNodes = false;
                }
            }

            if (isDraggingNodes) {
                Point2D offset = minPoint(pt, dragStartPoint);
                int gridX = (int) Math.round(offset.getX() / GRID_CELL_SIZE);
                int gridY = (int) Math.round(offset.getY() / (float) GRID_CELL_SIZE);
                for (String name : selectedNodes) {
                    nodebox.graphics.Point originalPosition = dragPositions.get(name);
                    nodebox.graphics.Point newPosition = originalPosition.moved(gridX, gridY);
                    getDocument().setNodePosition(findNodeWithName(name), newPosition);
                }
            } else if (isMakingConnection) {
            }

        }
    }

    @Override
    protected void processKeyEvent(KeyEvent e) {
        if (e.getID() == KeyEvent.KEY_PRESSED) {
            if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
                isShiftPressed = true;
            } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                isPanningView = true;
            }
        } else if (e.getID() == KeyEvent.KEY_RELEASED) {
            if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
                isShiftPressed = false;
            } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                isPanningView = false;
            }

        }
//        System.out.println("key e = " + e);
    }

    //// View queries ////

    public Node getNodeAt(Point2D point) {
        for (Node node : getNodes()) {
            Rectangle r = nodeRect(node);
            if (r.contains(point)) {
                return node;
            }
        }
        return null;
    }

    public Node getNodeAt(MouseEvent e) {
        return getNodeAt(e.getPoint());
    }

    //// Selections ////

    public boolean isRendered(Node node) {
        return getActiveNetwork().getRenderedChild() == node;
    }

    public boolean isSelected(Node node) {
        return (selectedNodes.contains(node.getName()));
    }

    public void select(Node node) {
        selectedNodes.add(node.getName());
    }

    /**
     * Select this node, and only this node.
     * <p/>
     * All other selected nodes will be deselected.
     *
     * @param node The node to select. If node is null, everything is deselected.
     */
    public void singleSelect(Node node) {
        selectedNodes.clear();
        if (node != null) {
            selectedNodes.add(node.getName());
            System.out.println("selectedNodes = " + selectedNodes);

            firePropertyChange(SELECT_PROPERTY, null, selectedNodes);
            document.setActiveNode(node);
            repaint();
        }
    }

    public void select(Iterable<Node> nodes) {
    }

    //    public void select(Set<NodeView> newSelection) {
//        boolean selectionChanged = false;
//        ArrayList<NodeView> nodeViewsToRemove = new ArrayList<NodeView>();
//        for (NodeView nodeView : selectedNodes) {
//            if (!newSelection.contains(nodeView)) {
//                selectionChanged = true;
//                nodeView.setSelected(false);
//                nodeViewsToRemove.add(nodeView);
//            }
//        }
//        for (NodeView nodeView : nodeViewsToRemove) {
//            selectedNodes.remove(nodeView);
//        }
//        for (NodeView nodeView : newSelection) {
//            if (!selectedNodes.contains(nodeView)) {
//                selectionChanged = true;
//                nodeView.setSelected(true);
//                selectedNodes.add(nodeView);
//            }
//        }
//        if (selectionChanged)
//            firePropertyChange(SELECT_PROPERTY, null, selectedNodes);
//    }
//
    public void toggleSelection(Node node) {
        checkNotNull(node);
        if (selectedNodes.isEmpty()) {
            singleSelect(node);

        } else {
            if (selectedNodes.contains(node.getName())) {
                selectedNodes.remove(node.getName());
            } else {
                selectedNodes.add(node.getName());
            }
            firePropertyChange(SELECT_PROPERTY, null, selectedNodes);
            repaint();
        }
    }

    //
//    public void addToSelection(Set<NodeView> newSelection) {
//        boolean selectionChanged = false;
//        for (NodeView nodeView : newSelection) {
//            if (!selectedNodes.contains(nodeView)) {
//                selectionChanged = true;
//                nodeView.setSelected(true);
//                selectedNodes.add(nodeView);
//            }
//        }
//        if (selectionChanged)
//            firePropertyChange(SELECT_PROPERTY, null, selectedNodes);
//    }
//
//    public void deselect(NodeView nodeView) {
//        if (nodeView == null) return;
//        // If the selectedNodes didn't contain the object in the first place, bail out.
//        // This is to prevent the select event from firing.
//        if (!selectedNodes.contains(nodeView)) return;
//        selectedNodes.remove(nodeView);
//        nodeView.setSelected(false);
//        firePropertyChange(SELECT_PROPERTY, null, selectedNodes);
//    }
//
//    public void selectAll() {
//        boolean selectionChanged = false;
//        for (Object child : getLayer().getChildrenReference()) {
//            if (!(child instanceof NodeView)) continue;
//            NodeView nodeView = (NodeView) child;
//            // Check if the selectedNodes already contained the node view.
//            // If it didn't, that means that the old selectedNodes is different
//            // from the new selectedNodes.
//            if (!selectedNodes.contains(nodeView)) {
//                selectionChanged = true;
//                nodeView.setSelected(true);
//                selectedNodes.add(nodeView);
//            }
//        }
//        if (selectionChanged)
//            firePropertyChange(SELECT_PROPERTY, null, selectedNodes);
//    }
//
    public void deselectAll() {
        if (selectedNodes.isEmpty()) return;
        selectedNodes.clear();
        firePropertyChange(SELECT_PROPERTY, null, selectedNodes);
        document.setActiveNode((Node) null);
    }

    //
//    private Set<NodeView> nodesToNodeViews(Iterable<Node> nodes) {
//        Set<NodeView> nodeViews = new HashSet<NodeView>();
//        for (Node node : nodes) {
//            nodeViews.add(getNodeView(node));
//        }
//        return nodeViews;
//    }
//
//    private Set<Node> nodeViewsToNodes(Iterable<NodeView> nodeViews) {
//        Set<Node> nodes = new HashSet<Node>();
//        for (NodeView nodeView : nodeViews) {
//            nodes.add(nodeView.getNode());
//        }
//        return nodes;
//    }
//
    public Iterable<String> getSelectedNodeNames() {
        return selectedNodes;
    }

    public Iterable<Node> getSelectedNodes() {
        ImmutableList.Builder<Node> b = new ImmutableList.Builder<nodebox.node.Node>();
        for (String name : getSelectedNodeNames()) {
            b.add(findNodeWithName(name));
        }
        return b.build();
    }

    public void deleteSelection() {
//        java.util.List<Node> selectedNodes = networkView.getSelectedNodes();
//        if (!selectedNodes.isEmpty()) {
//            Node node = getActiveNode();
//            if (node != null && selectedNodes.contains(node))
//                viewerPane.setHandle(null);
//            removeNodes(networkView.getSelectedNodes());
//        }
//        else if (networkView.hasSelectedConnection())
//            networkView.deleteSelectedConnection();
    }
//
//    public boolean hasSelectedConnection() {
//        return connectionLayer.hasSelection();
//    }
//
//    public void deleteSelectedConnection() {
//        if (hasSelectedConnection())
//            connectionLayer.deleteSelected();
//    }

    //// Events ////

    public void checkErrorAndRepaint() {
        //if (!networkError && !activeNetwork.hasError()) return;
        //networkError = activeNetwork.hasError();
        repaint();
    }

    public void codeChanged(Node node, boolean changed) {
        repaint();
    }

    //// Dragging ////

//    /**
//     * Change the position of all the selected nodes by adding the delta values to their positions.
//     *
//     * @param deltaX the change from the original X position.
//     * @param deltaY the change from the original Y position.
//     */
//    public void dragSelection(double deltaX, double deltaY) {
//        for (NodeView nv : selectedNodes) {
//            Point2D pt = nv.getOffset();
//            nv.setOffset(pt.getX() + deltaX, pt.getY() + deltaY);
//        }
//    }

    //// Connections ////

//    /**
//     * This method gets called when we start dragging a connection line from a node view.
//     *
//     * @param connectionSource the node view where we start from.
//     */
//    public void startConnection(NodeView connectionSource) {
//        this.connectionSource = connectionSource;
//    }
//
//    /**
//     * This method gets called from the NodeView to connect the output port to the input port.
//     *
//     * @param outputNode The output node.
//     * @param inputNode  The input node.
//     * @param inputPort  The input port.
//     */
//    public void connect(Node outputNode, Node inputNode, Port inputPort) {
//        getDocument().connect(outputNode, inputNode, inputPort);
//    }
//
//    /**
//     * This method gets called when a dragging operation ends.
//     * <p/>
//     * We don't care if a connection was established or not.
//     */
//    public void endConnection() {
//        NodeView oldTarget = this.connectionTarget;
//        this.connectionSource = null;
//        connectionTarget = null;
//        connectionPoint = null;
//        if (oldTarget != null)
//            oldTarget.repaint();
//        connectionLayer.repaint();
//    }
//
//    /**
//     * Return true if we are in the middle of a connection drag operation.
//     *
//     * @return true if we are connecting nodes together.
//     */
//    public boolean isConnecting() {
//        return connectionSource != null;
//    }

//    /**
//     * NodeView calls this method to indicate that the mouse was dragged while connecting.
//     * <p/>
//     * This method updates the point and redraws the connection layer.
//     *
//     * @param pt the new mouse location.
//     */
//    public void dragConnectionPoint(Point2D pt) {
//        assert isConnecting();
//        this.connectionPoint = pt;
//        connectionLayer.repaint();
//    }
//
//    /**
//     * NodeView calls this method to indicate that the new target is now the given node view.
//     *
//     * @param target the new NodeView target.
//     */
//    public void setTemporaryConnectionTarget(NodeView target) {
//        NodeView oldTarget = this.connectionTarget;
//        this.connectionTarget = target;
//        if (oldTarget != null)
//            oldTarget.repaint();
//        if (connectionTarget != null)
//            connectionTarget.repaint();
//    }

//    public NodeView getConnectionSource() {
//        return connectionSource;
//    }
//
//    public NodeView getConnectionTarget() {
//        return connectionTarget;
//    }
//
//    public Point2D getConnectionPoint() {
//        return connectionPoint;
//    }

    //// Network navigation ////

    private void goUp() {
        JOptionPane.showMessageDialog(this, "Child nodes are not supported yet.");
//        getDocument().goUp();
    }

    private void goDown() {
        JOptionPane.showMessageDialog(this, "Child nodes are not supported yet.");
//        if (selectedNodes.size() != 1) {
//            Toolkit.getDefaultToolkit().beep();
//            return;
//        }
//        NodeView selectedNode = selectedNodes.iterator().next();
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

    public boolean isPanning() {
        return panEnabled;
    }

    //// Inner classes ////

//    private class SelectionMarker extends PNode {
//        public SelectionMarker(Point2D p) {
//            setOffset(p);
//        }
//
//        protected void paint(PPaintContext c) {
//            Graphics2D g = c.getGraphics();
//            g.setColor(Theme.NETWORK_SELECTION_COLOR);
//            PBounds b = getBounds();
//            // Inset the bounds so we don't draw outside the refresh region.
//            b.inset(1, 1);
//            g.fill(b);
//            g.setColor(Theme.NETWORK_SELECTION_BORDER_COLOR);
//            g.draw(b);
//        }
//    }

//    class SelectionHandler extends PBasicInputEventHandler {
//        private Set<NodeView> temporarySelection = new HashSet<NodeView>();
//
//        public void mouseClicked(PInputEvent e) {
//            if (e.getButton() != MouseEvent.BUTTON1) return;
//            deselectAll();
//            getDocument().setActiveNode((Node) null);
//            connectionLayer.mouseClickedEvent(e);
//        }
//
//        public void mousePressed(PInputEvent e) {
//            if (e.getButton() != MouseEvent.BUTTON1) return;
//            temporarySelection.clear();
//            // Make sure no Node View is under the mouse cursor.
//            // In that case, we're not selecting, but moving a node.
//            Point2D p = e.getPosition();
//            NodeView nv = getNodeViewAt(p);
//            if (nv == null) {
//                selectionMarker = new SelectionMarker(p);
//                getLayer().addChild(selectionMarker);
//            } else {
//                selectionMarker = null;
//            }
//        }
//
//        public void mouseDragged(PInputEvent e) {
//            if (selectionMarker == null) return;
//            Point2D prev = selectionMarker.getOffset();
//            Point2D p = e.getPosition();
//            double width = p.getX() - prev.getX();
//            double absWidth = Math.abs(width);
//            double height = p.getY() - prev.getY();
//            double absHeight = Math.abs(height);
//            selectionMarker.setWidth(absWidth);
//            selectionMarker.setHeight(absHeight);
//            selectionMarker.setX(absWidth != width ? width : 0);
//            selectionMarker.setY(absHeight != height ? height : 0);
//            ListIterator childIter = getLayer().getChildrenIterator();
//            connectionLayer.deselect();
//            temporarySelection.clear();
//            while (childIter.hasNext()) {
//                Object o = childIter.next();
//                if (o instanceof NodeView) {
//                    NodeView nodeView = (NodeView) o;
//                    PNode n = (PNode) o;
//                    if (selectionMarker.getFullBounds().intersects(n.getFullBounds())) {
//                        nodeView.setSelected(true);
//                        temporarySelection.add(nodeView);
//                    } else {
//                        nodeView.setSelected(false);
//                    }
//                }
//            }
//        }
//
//        public void mouseReleased(PInputEvent e) {
//            if (selectionMarker == null) return;
//            getLayer().removeChild(selectionMarker);
//            selectionMarker = null;
//            select(temporarySelection);
//            temporarySelection.clear();
//        }
//    }

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

//    private class PopupHandler extends PBasicInputEventHandler {
//        public void processEvent(PInputEvent e, int i) {
//            if (!e.isPopupTrigger()) return;
//            if (e.isHandled()) return;
//            Point2D p = e.getCanvasPosition();
//            networkMenu.show(NetworkView.this, (int) p.getX(), (int) p.getY());
//        }
//    }

//    private class DoubleClickHandler extends PBasicInputEventHandler {
//        @Override
//        public void processEvent(PInputEvent e, int i) {
//            if (e.getClickCount() != 2) return;
//            NodeView view = getNodeViewAt(e.getPosition());
//            if (view != null || e.isHandled()) return;
//            e.setHandled(true);
//            document.showNodeSelectionDialog();
//        }
//    }

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
            resetViewTransform();
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
