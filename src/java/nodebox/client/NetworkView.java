package nodebox.client;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import nodebox.node.*;
import nodebox.ui.PaneView;
import nodebox.ui.Platform;
import nodebox.ui.Theme;
import org.python.google.common.base.Joiner;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class NetworkView extends JComponent implements PaneView, KeyListener, MouseListener, MouseWheelListener, MouseMotionListener {

    public static final int GRID_CELL_SIZE = 48;
    public static final int NODE_MARGIN = 6;
    public static final int NODE_PADDING = 5;
    public static final int NODE_WIDTH = GRID_CELL_SIZE * 3 - NODE_MARGIN * 2;
    public static final int NODE_HEIGHT = GRID_CELL_SIZE - NODE_MARGIN * 2;
    public static final int NODE_ICON_SIZE = 26;
    public static final int GRID_OFFSET = 6;
    public static final int PORT_WIDTH = 10;
    public static final int PORT_HEIGHT = 3;
    public static final int PORT_SPACING = 10;
    public static final Dimension NODE_DIMENSION = new Dimension(NODE_WIDTH, NODE_HEIGHT);

    public static final String SELECT_PROPERTY = "NetworkView.select";
    public static final String HIGHLIGHT_PROPERTY = "highlight";
    public static final String RENDER_PROPERTY = "render";
    public static final String NETWORK_PROPERTY = "network";

    private static Map<String, BufferedImage> nodeImageCache = new HashMap<String, BufferedImage>();
    private static BufferedImage nodeGeneric;

    public static final float MIN_ZOOM = 0.05f;
    public static final float MAX_ZOOM = 1.0f;

    public static final Map<String, Color> PORT_COLORS = Maps.newHashMap();
    public static final Color DEFAULT_PORT_COLOR = Color.WHITE;
    public static final Color NODE_BACKGROUND_COLOR = new Color(123, 154, 152);
    public static final Color PORT_HOVER_COLOR = Color.YELLOW;
    public static final Color TOOLTIP_BACKGROUND_COLOR = new Color(254, 255, 215);
    public static final Color TOOLTIP_STROKE_COLOR = Color.DARK_GRAY;
    public static final Color TOOLTIP_TEXT_COLOR = Color.DARK_GRAY;
    public static final Color DRAG_SELECTION_COLOR = new Color(255, 255, 255, 100);
    public static final BasicStroke DRAG_SELECTION_STROKE = new BasicStroke(1f);
    public static final BasicStroke CONNECTION_STROKE = new BasicStroke(2);

    private static Cursor defaultCursor, panCursor;

    private final NodeBoxDocument document;

    private JPopupMenu networkMenu;
    private Point networkMenuLocation;

    private JPopupMenu nodeMenu;
    private Point nodeMenuLocation;

    // View state
    private double viewX, viewY, viewScale = 1;
    private transient AffineTransform viewTransform = null;
    private transient AffineTransform inverseViewTransform = null;

    private Set<String> selectedNodes = new HashSet<String>();

    // Interaction state
    private boolean isDraggingNodes = false;
    private boolean isSpacePressed = false;
    private boolean isShiftPressed = false;
    private boolean isDragSelecting = false;
    private ImmutableMap<String, nodebox.graphics.Point> dragPositions = ImmutableMap.of();
    private NodePort overInput;
    private Node overOutput;
    private Node connectionOutput;
    private NodePort connectionInput;
    private Point2D connectionPoint;
    private boolean startDragging;
    private Point2D dragStartPoint;
    private Point2D dragCurrentPoint;

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
            nodeGeneric = ImageIO.read(new File("res/node-generic.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        PORT_COLORS.put(Port.TYPE_INT, new Color(116, 119, 121));
        PORT_COLORS.put(Port.TYPE_FLOAT, new Color(116, 119, 121));
        PORT_COLORS.put(Port.TYPE_STRING, new Color(92, 90, 91));
        PORT_COLORS.put(Port.TYPE_BOOLEAN, new Color(92, 90, 91));
        PORT_COLORS.put(Port.TYPE_POINT, new Color(119, 154, 173));
        PORT_COLORS.put(Port.TYPE_COLOR, new Color(94, 85, 112));
        PORT_COLORS.put("geometry", new Color(20, 20, 20));
        PORT_COLORS.put("list", new Color(76, 137, 174));
        PORT_COLORS.put("data", new Color(52, 85, 129));
    }

    /**
     * Tries to find an image representation for the node.
     * The image should be located near the library, and have the same name as the library.
     * <p/>
     * If this node has no image, the prototype is searched to find its image. If no image could be found,
     * a generic image is returned.
     *
     * @param node           the node
     * @param nodeRepository the list of nodes to look for the icon
     * @return an Image object.
     */
    public static BufferedImage getImageForNode(Node node, NodeRepository nodeRepository) {
        for (NodeLibrary library : nodeRepository.getLibraries()) {
            BufferedImage img = findNodeImage(library, node);
            if (img != null) {
                return img;
            }
        }
        if (node.getPrototype() != null) {
            return getImageForNode(node.getPrototype(), nodeRepository);
        } else {
            return nodeGeneric;
        }
    }

    public static BufferedImage findNodeImage(NodeLibrary library, Node node) {
        if (node == null || node.getImage() == null || node.getImage().isEmpty()) return null;
        if (!library.getRoot().hasChild(node)) return null;
        File libraryFile = library.getFile();
        if (libraryFile != null) {
            File libraryDirectory = libraryFile.getParentFile();
            if (libraryDirectory != null) {
                File nodeImageFile = new File(libraryDirectory, node.getImage());
                if (nodeImageFile.exists()) {
                    return readNodeImage(nodeImageFile);
                }
            }
        }
        return null;
    }

    public static BufferedImage readNodeImage(File nodeImageFile) {
        String imagePath = nodeImageFile.getAbsolutePath();
        if (nodeImageCache.containsKey(imagePath)) {
            return nodeImageCache.get(imagePath);
        } else {
            try {
                BufferedImage image = ImageIO.read(nodeImageFile);
                nodeImageCache.put(imagePath, image);
                return image;
            } catch (IOException e) {
                return null;
            }
        }
    }

    public NetworkView(NodeBoxDocument document) {
        this.document = document;
        setBackground(Theme.NETWORK_BACKGROUND_COLOR);
        initEventHandlers();
        initMenus();
    }

    private void initEventHandlers() {
        setFocusable(true);
        // This is disabled so we can detect the tab key.
        setFocusTraversalKeysEnabled(false);
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
    }

    private void initMenus() {
        networkMenu = new JPopupMenu();
        networkMenu.add(new NewNodeAction());
        networkMenu.add(new ResetViewAction());
        networkMenu.add(new GoUpAction());

        nodeMenu = new JPopupMenu();
        nodeMenu.add(new SetRenderedAction());
        nodeMenu.add(new RenameAction());
        nodeMenu.add(new DeleteAction());
        nodeMenu.add(new GoInAction());
    }

    public NodeBoxDocument getDocument() {
        return document;
    }

    public Node getActiveNetwork() {
        return document.getActiveNetwork();
    }

    //// Events ////

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

    public void checkErrorAndRepaint() {
        //if (!networkError && !activeNetwork.hasError()) return;
        //networkError = activeNetwork.hasError();
        repaint();
    }

    public void codeChanged(Node node, boolean changed) {
        repaint();
    }

    //// Model queries ////

    public Node getActiveNode() {
        return document.getActiveNode();
    }

    private ImmutableList<Node> getNodes() {
        return getDocument().getActiveNetwork().getChildren();
    }

    private ImmutableList<Node> getNodesReversed() {
        return getNodes().reverse();
    }

    private Iterable<Connection> getConnections() {
        return getDocument().getActiveNetwork().getConnections();
    }

    //// Painting the nodes ////

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        // Draw background
        g2.setColor(Theme.NETWORK_BACKGROUND_COLOR);
        g2.fill(g.getClipBounds());

        // Paint the grid
        // (The grid is not really affected by the view transform)
        paintGrid(g2);

        // Set the view transform
        AffineTransform originalTransform = g2.getTransform();
        g2.transform(getViewTransform());

        paintNodes(g2);
        paintConnections(g2);
        paintCurrentConnection(g2);
        paintPortTooltip(g2);
        paintDragSelection(g2);

        // Restore original transform
        g2.setTransform(originalTransform);
    }

    private void paintGrid(Graphics2D g) {
        g.setColor(Theme.NETWORK_GRID_COLOR);

        int gridCellSize = (int) Math.round(GRID_CELL_SIZE * viewScale);
        int gridOffset = (int) Math.round(GRID_OFFSET * viewScale);
        if (gridCellSize < 10) return;

        int transformOffsetX = (int) (viewX % gridCellSize);
        int transformOffsetY = (int) (viewY % gridCellSize);

        for (int y = -gridCellSize; y < getHeight() + gridCellSize; y += gridCellSize) {
            g.drawLine(0, y - gridOffset + transformOffsetY, getWidth(), y - gridOffset + transformOffsetY);
        }
        for (int x = -gridCellSize; x < getWidth() + gridCellSize; x += gridCellSize) {
            g.drawLine(x - gridOffset + transformOffsetX, 0, x - gridOffset + transformOffsetX, getHeight());
        }
    }

    private void paintConnections(Graphics2D g) {
        g.setColor(Theme.CONNECTION_DEFAULT_COLOR);
        g.setStroke(CONNECTION_STROKE);
        for (Connection connection : getConnections()) {
            paintConnection(g, connection);
        }
    }

    private void paintConnection(Graphics2D g, Connection connection) {
        Node outputNode = findNodeWithName(connection.getOutputNode());
        Node inputNode = findNodeWithName(connection.getInputNode());
        Port inputPort = inputNode.getInput(connection.getInputPort());
        g.setColor(portTypeColor(outputNode.getOutputType()));
        Rectangle outputRect = nodeRect(outputNode);
        Rectangle inputRect = nodeRect(inputNode);
        paintConnectionLine(g, outputRect.x + 4, outputRect.y + outputRect.height + 1, inputRect.x + portOffset(inputNode, inputPort) + 4, inputRect.y - 4);

    }

    private void paintCurrentConnection(Graphics2D g) {
        g.setColor(Theme.CONNECTION_DEFAULT_COLOR);
        if (connectionOutput != null) {
            Rectangle outputRect = nodeRect(connectionOutput);
            g.setColor(portTypeColor(connectionOutput.getOutputType()));
            paintConnectionLine(g, outputRect.x + 4, outputRect.y + outputRect.height + 1, (int) connectionPoint.getX(), (int) connectionPoint.getY());
        }
    }

    private static void paintConnectionLine(Graphics2D g, int x0, int y0, int x1, int y1) {
        double dy = Math.abs(y1 - y0);
        if (dy < GRID_CELL_SIZE) {
            g.drawLine(x0, y0, x1, y1);
        } else {
            double halfDx = Math.abs(x1 - x0) / 2;
            GeneralPath p = new GeneralPath();
            p.moveTo(x0, y0);
            p.curveTo(x0, y0 + halfDx, x1, y1 - halfDx, x1, y1);
            g.draw(p);
        }
    }

    private void paintNodes(Graphics2D g) {
        g.setColor(Theme.NETWORK_NODE_NAME_COLOR);
        for (Node node : getNodes()) {
            Port hoverInputPort = overInput != null && overInput.node == node.getName() ? findNodeWithName(overInput.node).getInput(overInput.port) : null;
            BufferedImage icon = getImageForNode(node, getDocument().getNodeRepository());
            paintNode(g, node, icon, isSelected(node), isRendered(node), connectionOutput, hoverInputPort, overOutput == node);
        }
    }

    private static Color portTypeColor(String type) {
        Color portColor = PORT_COLORS.get(type);
        return portColor == null ? DEFAULT_PORT_COLOR : portColor;
    }

    private static void paintNode(Graphics2D g, Node node, BufferedImage icon, boolean selected, boolean rendered, Node connectionOutput, Port hoverInputPort, boolean hoverOutput) {
        Rectangle r = nodeRect(node);
        String outputType = node.getOutputType();

        // Draw selection ring
        if (selected) {
            g.setColor(Color.WHITE);
            g.fillRect(r.x, r.y, NODE_WIDTH, NODE_HEIGHT);
        }

        // Draw node
        g.setColor(portTypeColor(outputType));
        if (selected) {
            g.fillRect(r.x + 2, r.y + 2, NODE_WIDTH - 4, NODE_HEIGHT - 4);
        } else {
            g.fillRect(r.x, r.y, NODE_WIDTH, NODE_HEIGHT);
        }

        // Draw render flag
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
        int portX = 0;
        for (Port input : node.getAllInputs()) {
            if (hoverInputPort == input) {
                g.setColor(PORT_HOVER_COLOR);
            } else {
                g.setColor(portTypeColor(input.getType()));
            }
            // Highlight ports that match the dragged connection type
            int portHeight = PORT_HEIGHT;
            if (connectionOutput != null) {
                if (connectionOutput.getOutputType().equals(input.getType())) {
                    portHeight = PORT_HEIGHT * 2;
                } else {
                    portHeight = 1;
                }
            }
            g.fillRect(r.x + portX, r.y - portHeight, PORT_WIDTH, portHeight);
            portX += PORT_WIDTH + PORT_SPACING;
        }

        // Draw output port
        if (hoverOutput && connectionOutput == null) {
            g.setColor(PORT_HOVER_COLOR);
        } else {
            g.setColor(portTypeColor(outputType));
        }
        g.fillRect(r.x, r.y + NODE_HEIGHT, PORT_WIDTH, PORT_HEIGHT);

        // Draw icon
        g.drawImage(icon, r.x + NODE_PADDING, r.y + NODE_PADDING, NODE_ICON_SIZE, NODE_ICON_SIZE, null);
        g.setColor(Color.WHITE);
        g.drawString(node.getName(), r.x + NODE_ICON_SIZE + NODE_PADDING * 2 + 2, r.y + 22);
    }

    private void paintPortTooltip(Graphics2D g) {
        if (overInput != null) {
            Node overInputNode = findNodeWithName(overInput.node);
            Port overInputPort = overInputNode.getInput(overInput.port);
            Rectangle r = inputPortRect(overInputNode, overInputPort);
            Point2D pt = new Point2D.Double(r.getX(), r.getY() + 11);
            String text = String.format("%s (%s)", overInput.port, overInputPort.getType());
            if (overInputNode.hasPublishedInput(overInput.port))
                text = text + " (published)";
            paintTooltip(g, pt, text);
        } else if (overOutput != null && connectionOutput == null) {
            Rectangle r = outputPortRect(overOutput);
            Point2D pt = new Point2D.Double(r.getX(), r.getY() + 11);
            String text = String.format("output (%s)", overOutput.getOutputType());
            paintTooltip(g, pt, text);
        }
    }

    private static void paintTooltip(Graphics2D g, Point2D point, String text) {
        FontMetrics fontMetrics = g.getFontMetrics();
        int textWidth = fontMetrics.stringWidth(text);

        int verticalOffset = 10;
        Rectangle r = new Rectangle((int) point.getX(), (int) point.getY() + verticalOffset, textWidth, fontMetrics.getHeight());
        r.grow(4, 3);
        g.setColor(TOOLTIP_STROKE_COLOR);
        g.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8);
        g.setColor(TOOLTIP_BACKGROUND_COLOR);
        g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);

        g.setColor(TOOLTIP_TEXT_COLOR);
        g.drawString(text, (float) point.getX(), (float) point.getY() + fontMetrics.getAscent() + verticalOffset);
    }

    private void paintDragSelection(Graphics2D g) {
        if (isDragSelecting) {
            Rectangle r = dragSelectRect();
            g.setColor(DRAG_SELECTION_COLOR);
            g.setStroke(DRAG_SELECTION_STROKE);
            g.fill(r);
            // To get a smooth line we need to subtract one from the width and height.
            g.drawRect((int) r.getX(), (int) r.getY(), (int) r.getWidth() - 1, (int) r.getHeight() - 1);
        }
    }

    private Rectangle dragSelectRect() {
        int x0 = (int) dragStartPoint.getX();
        int y0 = (int) dragStartPoint.getY();
        int x1 = (int) dragCurrentPoint.getX();
        int y1 = (int) dragCurrentPoint.getY();
        int x = Math.min(x0, x1);
        int y = Math.min(y0, y1);
        int w = (int) Math.abs(dragCurrentPoint.getX() - dragStartPoint.getX());
        int h = (int) Math.abs(dragCurrentPoint.getY() - dragStartPoint.getY());
        return new Rectangle(x, y, w, h);
    }

    private static Rectangle nodeRect(Node node) {
        return new Rectangle(nodePoint(node), NODE_DIMENSION);
    }

    private static Rectangle inputPortRect(Node node, Port port) {
        Point pt = nodePoint(node);
        Rectangle portRect = new Rectangle(pt.x + portOffset(node, port), pt.y - PORT_HEIGHT, PORT_WIDTH, PORT_HEIGHT);
        growHitRectangle(portRect);
        return portRect;
    }

    private static Rectangle outputPortRect(Node node) {
        Point pt = nodePoint(node);
        Rectangle portRect = new Rectangle(pt.x, pt.y + NODE_HEIGHT, PORT_WIDTH, PORT_HEIGHT);
        growHitRectangle(portRect);
        return portRect;
    }

    private static void growHitRectangle(Rectangle r) {
        r.grow(2, 2);
    }

    private static Point nodePoint(Node node) {
        int nodeX = ((int) node.getPosition().getX()) * GRID_CELL_SIZE;
        int nodeY = ((int) node.getPosition().getY()) * GRID_CELL_SIZE;
        return new Point(nodeX, nodeY);
    }

    private Point pointToGridPoint(Point e) {
        Point2D pt = getInverseViewTransform().transform(e, null);
        return new Point(
                (int) Math.floor(pt.getX() / GRID_CELL_SIZE),
                (int) Math.floor(pt.getY() / GRID_CELL_SIZE));
    }

    private static int portOffset(Node node, Port port) {
        int portIndex = node.getAllInputs().indexOf(port);
        return (PORT_WIDTH + PORT_SPACING) * portIndex;
    }

    //// View Transform ////

    private void setViewTransform(double viewX, double viewY, double viewScale) {
        this.viewX = viewX;
        this.viewY = viewY;
        this.viewScale = viewScale;
        this.viewTransform = null;
        this.inverseViewTransform = null;
    }

    private AffineTransform getViewTransform() {
        if (viewTransform == null) {
            viewTransform = new AffineTransform();
            viewTransform.translate(viewX, viewY);
            viewTransform.scale(viewScale, viewScale);
        }
        return viewTransform;
    }

    private AffineTransform getInverseViewTransform() {
        if (inverseViewTransform == null) {
            try {
                inverseViewTransform = getViewTransform().createInverse();
            } catch (NoninvertibleTransformException e) {
                inverseViewTransform = new AffineTransform();
            }
        }
        return inverseViewTransform;
    }

    private void resetViewTransform() {
        setViewTransform(0, 0, 1);
        repaint();
    }

    //// View queries ////

    private Node findNodeWithName(String name) {
        return getActiveNetwork().getChild(name);
    }

    public Node getNodeAt(Point2D point) {
        for (Node node : getNodesReversed()) {
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

    public Node getNodeWithOutputPortAt(Point2D point) {
        for (Node node : getNodesReversed()) {
            Rectangle r = outputPortRect(node);
            if (r.contains(point)) {
                return node;
            }
        }
        return null;
    }

    public NodePort getInputPortAt(Point2D point) {
        for (Node node : getNodesReversed()) {
            for (PublishedPort port : node.getPublishedInputs()) {
                Rectangle r = inputPortRect(node, node.getInput(port.getPublishedName()));
                if (r.contains(point))
                    return NodePort.of(node.getName(), port.getPublishedName());
            }
            for (Port port : node.getInputs()) {
                Rectangle r = inputPortRect(node, port);
                if (r.contains(point)) {
                    return NodePort.of(node.getName(), port.getName());
                }
            }
        }
        return null;
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
        if (selectedNodes.size() == 1 && selectedNodes.contains(node.getName())) return;
        selectedNodes.clear();
        if (node != null && getActiveNetwork().hasChild(node)) {
            selectedNodes.add(node.getName());
            firePropertyChange(SELECT_PROPERTY, null, selectedNodes);
            document.setActiveNode(node);
        }
        repaint();
    }

    public void select(Iterable<Node> nodes) {
        selectedNodes.clear();
        for (Node node : nodes) {
            selectedNodes.add(node.getName());
        }
    }

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

    public void deselectAll() {
        if (selectedNodes.isEmpty()) return;
        selectedNodes.clear();
        firePropertyChange(SELECT_PROPERTY, null, selectedNodes);
        document.setActiveNode((Node) null);
        repaint();
    }

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
        document.removeNodes(getSelectedNodes());
    }

    //// Network navigation ////

    private void goUp() {
        if (getDocument().getActiveNetworkPath().equals("/")) return;
        Iterable it = Splitter.on("/").split(getDocument().getActiveNetworkPath());
        int parts = Iterables.size(it);
        String path = parts - 1 > 1 ? Joiner.on("/").join(Iterables.limit(it, parts - 1)) : "/";
        getDocument().setActiveNetwork(path);
    }

    private void goDown() {
        JOptionPane.showMessageDialog(this, "Child nodes are not supported yet.");
    }

    //// Input Events ////

    public void keyTyped(KeyEvent e) {
        switch (e.getKeyChar()) {
            case KeyEvent.VK_BACK_SPACE:
                getDocument().deleteSelection();
                break;
            case KeyEvent.VK_U:
                goUp();
                break;
            case KeyEvent.VK_ENTER:
                goDown();
                break;
        }
    }

    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if (keyCode == KeyEvent.VK_SHIFT) {
            isShiftPressed = true;
        } else if (keyCode == KeyEvent.VK_SPACE) {
            isSpacePressed = true;
            setCursor(panCursor);
        } else if (keyCode == KeyEvent.VK_UP) {
            moveSelectedNodes(0, -1);
        } else if (keyCode == KeyEvent.VK_RIGHT) {
            moveSelectedNodes(1, 0);
        } else if (keyCode == KeyEvent.VK_DOWN) {
            moveSelectedNodes(0, 1);
        } else if (keyCode == KeyEvent.VK_LEFT) {
            moveSelectedNodes(-1, 0);
        }
    }

    private void moveSelectedNodes(int dx, int dy) {
        for (Node node : getSelectedNodes()) {
            getDocument().setNodePosition(node, node.getPosition().moved(dx, dy));
        }
    }

    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
            isShiftPressed = false;
        } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            isSpacePressed = false;
            setCursor(defaultCursor);
        }
    }


    public boolean isSpacePressed() {
        return isSpacePressed;
    }

    public void mouseClicked(MouseEvent e) {
        Point2D pt = inverseViewTransformPoint(e.getPoint());
        if (e.getButton() == MouseEvent.BUTTON1) {
            if (e.getClickCount() == 1) {
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
            } else if (e.getClickCount() == 2) {
                Node clickedNode = getNodeAt(pt);
                if (clickedNode == null) {
                    Point gridPoint = pointToGridPoint(e.getPoint());
                    getDocument().showNodeSelectionDialog(gridPoint);
                } else {
                    document.setRenderedNode(clickedNode);
                }
            }
        }
    }

    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) {
            Point pt = e.getPoint();
            NodePort nodePort = getInputPortAt(inverseViewTransformPoint(pt));
            if (nodePort != null) {
                JPopupMenu pMenu = new JPopupMenu();
                pMenu.add(new PublishAction(nodePort));
                pMenu.show(this, e.getX(), e.getY());
            } else {
                Node pressedNode = getNodeAt(inverseViewTransformPoint(pt));
                if (pressedNode != null) {
                    nodeMenuLocation = pt;
                    nodeMenu.show(this, e.getX(), e.getY());
                } else {
                    networkMenuLocation = pt;
                    networkMenu.show(this, e.getX(), e.getY());
                }
            }
        } else {
            // If the space bar and mouse is pressed, we're getting ready to pan the view.
            if (isSpacePressed) {
                // When panning the view use the original mouse point, not the one affected by the view transform.
                dragStartPoint = e.getPoint();
                return;
            }

            Point2D pt = inverseViewTransformPoint(e.getPoint());

            // Check if we're over an output port.
            connectionOutput = getNodeWithOutputPortAt(pt);
            if (connectionOutput != null) return;

            // Check if we're over a connected input port.
            connectionInput = getInputPortAt(pt);
            if (connectionInput != null) {
                // We're over a port, but is it connected?
                Connection c = getActiveNetwork().getConnection(connectionInput.node, connectionInput.port);
                // Disconnect it, but start a new connection on the same node immediately.
                if (c != null) {
                    getDocument().disconnect(c);
                    connectionOutput = getActiveNetwork().getChild(c.getOutputNode());
                    connectionPoint = pt;
                }
                return;
            }

            // Check if we're pressing a node.
            Node pressedNode = getNodeAt(pt);
            if (pressedNode != null) {
                // Don't immediately set "isDragging."
                // We wait until we actually drag the first time to do the work.
                startDragging = true;
                return;
            }

            // We're creating a drag selection.
            isDragSelecting = true;
            dragStartPoint = pt;
        }
    }

    public void mouseReleased(MouseEvent e) {
        isDraggingNodes = false;
        isDragSelecting = false;
        if (connectionOutput != null && connectionInput != null) {
            getDocument().connect(connectionOutput.getName(), connectionInput.node, connectionInput.port);
        }
        connectionOutput = null;
        if (e.isPopupTrigger()) {
            networkMenu.show(this, e.getX(), e.getY());
        }
        repaint();
    }

    public void mouseEntered(MouseEvent e) {
        grabFocus();
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseDragged(MouseEvent e) {
        Point2D pt = inverseViewTransformPoint(e.getPoint());
        // Panning the view has the first priority.
        if (isSpacePressed) {
            // When panning the view use the original mouse point, not the one affected by the view transform.
            Point2D offset = minPoint(e.getPoint(), dragStartPoint);
            setViewTransform(viewX + offset.getX(), viewY + offset.getY(), viewScale);
            dragStartPoint = e.getPoint();
            repaint();
            return;
        }

        if (connectionOutput != null) {
            repaint();
            connectionInput = getInputPortAt(pt);
            connectionPoint = pt;
            overOutput = getNodeWithOutputPortAt(pt);
            overInput = getInputPortAt(pt);
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
        }

        if (isDragSelecting) {
            dragCurrentPoint = pt;
            Rectangle r = dragSelectRect();
            selectedNodes.clear();
            for (Node node : getNodes()) {
                if (r.intersects(nodeRect(node))) {
                    selectedNodes.add(node.getName());
                }
            }
            repaint();
        }
    }

    public void mouseMoved(MouseEvent e) {
        Point2D pt = inverseViewTransformPoint(e.getPoint());
        overOutput = getNodeWithOutputPortAt(pt);
        overInput = getInputPortAt(pt);
        // It is probably very inefficient to repaint the view every time the mouse moves.
        repaint();
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        double scaleDelta = 1F - e.getWheelRotation() / 10F;
        double newViewScale = viewScale * scaleDelta;

        if (newViewScale < MIN_ZOOM) {
            scaleDelta = MIN_ZOOM / viewScale;
        } else if (newViewScale > MAX_ZOOM) {
            scaleDelta = MAX_ZOOM / viewScale;
        }

        double vx = viewX - (e.getX() - viewX) * (scaleDelta - 1);
        double vy = viewY - (e.getY() - viewY) * (scaleDelta - 1);
        setViewTransform(vx, vy, viewScale * scaleDelta);
        repaint();
    }

    private ImmutableMap<String, nodebox.graphics.Point> selectedNodePositions() {
        ImmutableMap.Builder<String, nodebox.graphics.Point> b = ImmutableMap.builder();
        for (String nodeName : selectedNodes) {
            b.put(nodeName, findNodeWithName(nodeName).getPosition());
        }
        return b.build();
    }

    private Point2D inverseViewTransformPoint(Point p) {
        Point2D pt = new Point2D.Double(p.getX(), p.getY());
        return getInverseViewTransform().transform(pt, null);
    }

    private Point2D minPoint(Point2D a, Point2D b) {
        return new Point2D.Double(a.getX() - b.getX(), a.getY() - b.getY());
    }

    private class NewNodeAction extends AbstractAction {
        private NewNodeAction() {
            super("New Node");
        }

        public void actionPerformed(ActionEvent e) {
            Point gridPoint = pointToGridPoint(networkMenuLocation);
            getDocument().showNodeSelectionDialog(gridPoint);
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

    private class PublishAction extends AbstractAction {
        private NodePort nodePort;

        private PublishAction(NodePort nodePort) {
            super(getActiveNetwork().hasPublishedChildInput(nodePort.getNode(), nodePort.getPort()) ? "Unpublish" : "Publish");
            this.nodePort = nodePort;
        }

        public void actionPerformed(ActionEvent e) {
            if (getActiveNetwork().hasPublishedChildInput(nodePort.getNode(), nodePort.getPort())) {
                unpublish();
            } else {
                publish();
            }
        }

        private void unpublish() {
            for (PublishedPort pp : getActiveNetwork().getPublishedInputs()) {
                if (pp.getChildNode().equals(nodePort.getNode()) &&
                        pp.getChildPort().equals(nodePort.getPort()))
                    getDocument().unpublish(pp.getPublishedName());
            }
        }

        private void publish() {
            String s = JOptionPane.showInputDialog(NetworkView.this, "Publish as:", nodePort.getPort());
            if (s == null || s.length() == 0)
                return;
            getDocument().publish(nodePort.getNode(), nodePort.getPort(), s);
        }
    }

    private class SetRenderedAction extends AbstractAction {
        private SetRenderedAction() {
            super("Set Rendered");
        }

        public void actionPerformed(ActionEvent e) {
            Node node = getNodeAt(inverseViewTransformPoint(nodeMenuLocation));
            document.setRenderedNode(node);
        }
    }

    private class RenameAction extends AbstractAction {
        private RenameAction() {
            super("Rename");
        }

        public void actionPerformed(ActionEvent e) {
            Node node = getNodeAt(inverseViewTransformPoint(nodeMenuLocation));
            String s = JOptionPane.showInputDialog(NetworkView.this, "New name:", node.getName());
            if (s == null || s.length() == 0)
                return;
            try {
                getDocument().setNodeName(node, s);
            } catch (InvalidNameException ex) {
                JOptionPane.showMessageDialog(NetworkView.this, "The given name is not valid.\n" + ex.getMessage(), Application.NAME, JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private class DeleteAction extends AbstractAction {
        private DeleteAction() {
            super("Delete");
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0));
        }

        public void actionPerformed(ActionEvent e) {
            deleteSelection();
        }
    }

    private class GoInAction extends AbstractAction {
        private GoInAction() {
            super("Edit Children");
            putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
        }

        public void actionPerformed(ActionEvent e) {
            Node node = getNodeAt(inverseViewTransformPoint(nodeMenuLocation));
            String childPath = Node.path(getDocument().getActiveNetworkPath(), node.getName());
            getDocument().setActiveNetwork(childPath);
        }
    }
}
