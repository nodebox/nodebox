package nodebox.client;

import com.google.common.base.Splitter;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.*;
import nodebox.node.*;
import nodebox.ui.PaneView;
import nodebox.ui.Platform;
import nodebox.ui.Theme;
import nodebox.ui.Zoom;
import org.python.google.common.base.Joiner;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static com.google.common.base.Preconditions.checkNotNull;

public class NetworkView extends ZoomableView implements PaneView, Zoom {

    public static final int GRID_CELL_SIZE = 48;
    public static final int NODE_MARGIN = 6;
    public static final int NODE_PADDING = 5;
    public static final int NODE_WIDTH = GRID_CELL_SIZE * 3 - NODE_MARGIN * 2;
    public static final int NODE_HEIGHT = GRID_CELL_SIZE - NODE_MARGIN * 2;
    public static final int NODE_ICON_SIZE = 26;
    public static final int GRID_OFFSET = 6;
    public static final int PORT_WIDTH = 10;
    public static final int PORT_HEIGHT = 3;
    public static final int PORT_MARGIN = 6;
    public static final int PORT_SPACING = 10;
    public static final Dimension NODE_DIMENSION = new Dimension(NODE_WIDTH, NODE_HEIGHT);

    public static final String SELECT_PROPERTY = "NetworkView.select";
    public static final int COMMENT_BOX_MARGIN_HORIZONTAL = 5;

    private static Map<String, BufferedImage> fileImageCache = new HashMap<String, BufferedImage>();
    private static BufferedImage nodeGeneric, commentIcon, commentBox;

    public static final float MIN_ZOOM = 0.05f;
    public static final float MAX_ZOOM = 1.0f;

    public static final Map<String, Color> PORT_COLORS = Maps.newHashMap();
    public static final Color DEFAULT_PORT_COLOR = new Color(52, 85, 52);
    public static final Color PORT_HOVER_COLOR = Color.YELLOW;
    public static final Color TOOLTIP_BACKGROUND_COLOR = new Color(254, 255, 215);
    public static final Color TOOLTIP_STROKE_COLOR = Color.DARK_GRAY;
    public static final Color TOOLTIP_TEXT_COLOR = Color.DARK_GRAY;
    public static final Color DRAG_SELECTION_COLOR = new Color(255, 255, 255, 100);
    public static final BasicStroke DRAG_SELECTION_STROKE = new BasicStroke(1f);
    public static final BasicStroke CONNECTION_STROKE = new BasicStroke(2);

    private final NodeBoxDocument document;

    private JPopupMenu networkMenu;
    private Point networkMenuLocation;

    private Point nodeMenuLocation;

    private LoadingCache<Node, BufferedImage> nodeImageCache;

    private Set<String> selectedNodes = new HashSet<String>();

    // Interaction state
    private boolean isDraggingNodes = false;
    private boolean isShiftPressed = false;
    private boolean isAltPressed = false;
    private boolean isDragSelecting = false;
    private ImmutableMap<String, nodebox.graphics.Point> dragPositions = ImmutableMap.of();
    private NodePort overInput;
    private Node overOutput;
    private Node overComment;
    private Node connectionOutput;
    private NodePort connectionInput;
    private Point2D connectionPoint;
    private boolean startDragging;
    private Point2D dragStartPoint;
    private Point2D dragCurrentPoint;

    static {
        try {
            nodeGeneric = ImageIO.read(NetworkView.class.getResourceAsStream("/node-generic.png"));
            commentIcon = ImageIO.read(NetworkView.class.getResourceAsStream("/comment-icon.png"));
            commentBox = ImageIO.read(NetworkView.class.getResourceAsStream("/notes-background.png"));
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

        File libraryDirectory = null;
        if (library.getFile() != null)
            libraryDirectory = library.getFile().getParentFile();
        else if (library.equals(NodeLibrary.coreLibrary))
            libraryDirectory = new File("libraries/core");

        if (libraryDirectory != null) {
            File nodeImageFile = new File(libraryDirectory, node.getImage());
            if (nodeImageFile.exists()) {
                return readNodeImage(nodeImageFile);
            }
        }
        return null;
    }

    public static BufferedImage readNodeImage(File nodeImageFile) {
        String imagePath = nodeImageFile.getAbsolutePath();
        if (fileImageCache.containsKey(imagePath)) {
            return fileImageCache.get(imagePath);
        } else {
            try {
                BufferedImage image = ImageIO.read(nodeImageFile);
                fileImageCache.put(imagePath, image);
                return image;
            } catch (IOException e) {
                return null;
            }
        }
    }

    public NetworkView(NodeBoxDocument document) {
        super(MIN_ZOOM, MAX_ZOOM);
        this.document = document;
        setBackground(Theme.NETWORK_BACKGROUND_COLOR);
        initEventHandlers();
        initMenus();
        nodeImageCache = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .build(new NodeImageCacheLoader(document.getNodeRepository()));
    }

    private void initEventHandlers() {
        setFocusable(true);
        // This is disabled so we can detect the tab key.
        setFocusTraversalKeysEnabled(false);
        addKeyListener(new KeyHandler());
        MouseHandler mh = new MouseHandler();
        addMouseListener(mh);
        addMouseMotionListener(mh);
        addFocusListener(new FocusHandler());
    }

    private void initMenus() {
        networkMenu = new JPopupMenu();
        networkMenu.add(new NewNodeAction());
        networkMenu.add(new ResetViewAction());
        networkMenu.add(new GoUpAction());
    }

    private JPopupMenu createNodeMenu(Node node) {
        JPopupMenu menu = new JPopupMenu();
        menu.add(new SetRenderedAction());
        menu.add(new RenameAction());
        menu.add(new DeleteAction());
        menu.add(new GroupIntoNetworkAction(null));

        if (node.isNetwork()) {
            menu.add(new GoInAction());
        }
        if (!node.hasComment()) {
            menu.add(new AddCommentAction());
        } else {
            menu.add(new EditCommentAction());
            menu.add(new RemoveCommentAction());
        }

        menu.add(new HelpAction());
        return menu;
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
        // TODO Check for errors in an efficient way.
    }

    public void codeChanged(Node node, boolean changed) {
        repaint();
    }

    //// Model queries ////

    private ImmutableList<Node> getNodes() {
        return getDocument().getActiveNetwork().getChildren();
    }

    private ImmutableList<Node> getNodesReversed() {
        return getNodes().reverse();
    }

    private Iterable<Connection> getConnections() {
        return getDocument().getActiveNetwork().getConnections();
    }

    public static boolean isPublished(Node network, Node childNode, Port childPort) {
        return network.hasPublishedInput(childNode.getName(), childPort.getName());
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
        paintCommentBox(g2);

        // Restore original transform
        g2.setTransform(originalTransform);
    }

    private void paintGrid(Graphics2D g) {
        g.setColor(Theme.NETWORK_GRID_COLOR);

        int gridCellSize = (int) Math.round(GRID_CELL_SIZE * getViewScale());
        int gridOffset = (int) Math.round(GRID_OFFSET * getViewScale());
        if (gridCellSize < 10) return;

        int transformOffsetX = (int) (getViewX() % gridCellSize);
        int transformOffsetY = (int) (getViewY() % gridCellSize);

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
            double halfDx = Math.abs(x1 - x0) / 2.0;
            GeneralPath p = new GeneralPath();
            p.moveTo(x0, y0);
            p.curveTo(x0, y0 + halfDx, x1, y1 - halfDx, x1, y1);
            g.draw(p);
        }
    }

    private void paintNodes(Graphics2D g) {
        g.setColor(Theme.NETWORK_NODE_NAME_COLOR);
        Node renderedNode = getActiveNetwork().getRenderedChild();
        for (Node node : getNodes()) {
            Port hoverInputPort = overInput != null && overInput.node.equals(node.getName()) ? findNodeWithName(overInput.node).getInput(overInput.port) : null;
            BufferedImage icon = getCachedImageForNode(node);
            paintNode(g, getActiveNetwork(), node, icon, commentIcon, isSelected(node), renderedNode == node, connectionOutput, hoverInputPort, overOutput == node);
        }
    }

    private BufferedImage getCachedImageForNode(Node node) {
        try {
            return nodeImageCache.get(node);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public static Color portTypeColor(String type) {
        Color portColor = PORT_COLORS.get(type);
        return portColor == null ? DEFAULT_PORT_COLOR : portColor;
    }

    private static String getShortenedName(String name, int startChars) {
        nodebox.graphics.Text text = new nodebox.graphics.Text(name, nodebox.graphics.Point.ZERO);
        text.setFontName(Theme.NETWORK_FONT.getFontName());
        text.setFontSize(Theme.NETWORK_FONT.getSize());
        int cells = Math.min(Math.max(3, 1 + (int) Math.ceil(text.getMetrics().getWidth() / (GRID_CELL_SIZE - 6))), 6);
        if (cells > 3)
            return getShortenedName(name.substring(0, startChars) + "\u2026" + name.substring(name.length() - 3, name.length()), startChars - 1);
        return name;
    }

    private void paintNode(Graphics2D g, Node network, Node node, BufferedImage icon, BufferedImage commentIcon, boolean selected, boolean rendered, Node connectionOutput, Port hoverInputPort, boolean hoverOutput) {
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
        for (Port input : node.getInputs()) {
            if (isHiddenPort(input)) {
                continue;
            }
            if (hoverInputPort == input) {
                g.setColor(PORT_HOVER_COLOR);
            } else {
                g.setColor(portTypeColor(input.getType()));
            }
            // Highlight ports that match the dragged connection type
            int portHeight = PORT_HEIGHT;
            if (connectionOutput != null) {
                String connectionOutputType = connectionOutput.getOutputType();
                String inputType = input.getType();
                if (connectionOutputType.equals(inputType) || inputType.equals(Port.TYPE_LIST)) {
                    portHeight = PORT_HEIGHT * 2;
                } else if (TypeConversions.canBeConverted(connectionOutputType, inputType)) {
                    portHeight = PORT_HEIGHT - 1;
                } else {
                    portHeight = 1;
                }
            }

            if (isPublished(network, node, input)) {
                Point2D topLeft = inverseViewTransformPoint(new Point(4, 0));
                g.setColor(portTypeColor(input.getType()));
                g.setStroke(CONNECTION_STROKE);
                paintConnectionLine(g, (int) topLeft.getX(), (int) topLeft.getY(), r.x + portX + 4, r.y - 2);
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
        g.setFont(Theme.NETWORK_FONT);
        g.drawString(getShortenedName(node.getName(), 7), r.x + NODE_ICON_SIZE + NODE_PADDING * 2 + 2, r.y + 22);

        // Draw comment icon
        if (node.hasComment()) {
            g.drawImage(commentIcon, r.x + NODE_WIDTH - 13, r.y + 5, null);
        }
    }

    private void paintPortTooltip(Graphics2D g) {
        if (overInput != null) {
            Node overInputNode = findNodeWithName(overInput.node);
            Port overInputPort = overInputNode.getInput(overInput.port);
            Rectangle r = inputPortRect(overInputNode, overInputPort, false);
            Point2D pt = new Point2D.Double(r.getX(), r.getY() + 11);
            String text = String.format("%s (%s)", overInput.port, overInputPort.getType());
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

    private void paintCommentBox(Graphics2D g) {
        if (overComment != null) {
            Rectangle r = nodeRect(overComment);
            FontMetrics fontMetrics = g.getFontMetrics();
            int commentWidth = fontMetrics.stringWidth(overComment.getComment());
            int x = r.x + 16;
            int y = r.y + GRID_CELL_SIZE - 5;
            g.setColor(Color.DARK_GRAY);
            g.fillRect(x + 1, y + 1, commentWidth + COMMENT_BOX_MARGIN_HORIZONTAL * 2, commentBox.getHeight());
            g.drawImage(commentBox, x, y, commentWidth + COMMENT_BOX_MARGIN_HORIZONTAL * 2, commentBox.getHeight(), null);
            g.setColor(Color.DARK_GRAY);
            g.drawString(overComment.getComment(), x + COMMENT_BOX_MARGIN_HORIZONTAL, y + 14);
        }
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
        if (dragStartPoint == null || dragCurrentPoint == null) return new Rectangle();
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

    private static Rectangle inputPortRect(Node node, Port port, boolean isConnecting) {
        if (isHiddenPort(port)) return new Rectangle();
        Point pt = nodePoint(node);
        int portWidth = !isConnecting ? PORT_WIDTH : PORT_WIDTH + PORT_MARGIN;
        int portHeight = !isConnecting ? PORT_HEIGHT : PORT_HEIGHT + NODE_HEIGHT;
        Rectangle portRect = new Rectangle(pt.x + portOffset(node, port), pt.y - PORT_HEIGHT, portWidth, portHeight);
        growHitRectangle(portRect);
        return portRect;
    }

    private static Rectangle outputPortRect(Node node) {
        Point pt = nodePoint(node);
        Rectangle portRect = new Rectangle(pt.x, pt.y + NODE_HEIGHT - 10, PORT_WIDTH + 10, PORT_HEIGHT + 10);
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

    public Point centerGridPoint() {
        Point pt = pointToGridPoint(new Point((int) (getBounds().getWidth() / 2), (int) (getBounds().getHeight() / 2)));
        return new Point((int) pt.getX() - 1, (int) pt.getY());
    }

    private static int portOffset(Node node, Port port) {
        int portIndex = node.getInputs().indexOf(port);
        return (PORT_WIDTH + PORT_SPACING) * portIndex;
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

    public Node getNodeWithOutputPortAt(Point2D point) {
        for (Node node : getNodesReversed()) {
            Rectangle r = outputPortRect(node);
            if (r.contains(point)) {
                return node;
            }
        }
        return null;
    }

    public NodePort getInputPortAt(Point2D point, boolean isConnecting) {
        for (Node node : getNodesReversed()) {
            for (Port port : node.getInputs()) {
                Rectangle r = inputPortRect(node, port, isConnecting);
                if (r.contains(point)) {
                    return NodePort.of(node.getName(), port.getName());
                }
            }
        }
        return null;
    }

    /**
     * Check if there is a commented node at a given point
     *
     * @param point The point that the mouse produces a MouseEvent
     * @return the Node if it exist at the given point
     */
    public Node getNodeWithCommentAt(Point2D point) {
        for (Node node : getNodesReversed()) {
            if (node.hasComment()) {
                Rectangle r = nodeRect(node);
                if (r.contains(point)) {
                    return node;
                }
            }
        }
        return null;
    }

    private static boolean isHiddenPort(Port port) {
        return port.getType().equals(Port.TYPE_STATE) || port.getType().equals(Port.TYPE_CONTEXT);
    }

    @Override
    protected void onViewTransformChanged(double viewX, double viewY, double viewScale) {
        document.setActiveNetworkPanZoom(viewX, viewY, viewScale);
    }

    //// Selections ////

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
            document.setActiveNode(node);
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
        if (selectedNodes.isEmpty()) return ImmutableList.of();
        ImmutableList.Builder<Node> b = new ImmutableList.Builder<nodebox.node.Node>();
        for (String name : getSelectedNodeNames()) {
            b.add(findNodeWithName(name));
        }
        return b.build();
    }

    public void deleteSelection() {
        // Delete the nodes from the document
        document.removeNodes(getSelectedNodes());

        // Remove the deleted nodes from the current selection
        selectedNodes.clear();
    }

    private void moveSelectedNodes(int dx, int dy) {
        for (Node node : getSelectedNodes()) {
            getDocument().setNodePosition(node, node.getPosition().moved(dx, dy));
        }
    }

    private void renameNode(Node node) {
        String s = JOptionPane.showInputDialog(this, "New name (no spaces, don't start with a digit):", node.getName());
        if (s == null || s.length() == 0)
            return;
        try {
            getDocument().setNodeName(node, s);
        } catch (InvalidNameException ex) {
            JOptionPane.showMessageDialog(this, "The given name is not valid.\n" + ex.getMessage(), Application.NAME, JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "An error occurred:\n" + ex.getMessage(), Application.NAME, JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Show an input dialog to insert a new comment.
     */
    private void addComment(Node node) {
        String comment = JOptionPane.showInputDialog(this, "New comment:");
        if (comment != null && !comment.trim().isEmpty()) {
            getDocument().setNodeComment(node, comment);
        }
    }

    private void editComment(Node node) {
        String comment = JOptionPane.showInputDialog(this, "Edit comment:", node.getComment());
        getDocument().setNodeComment(node, comment);
    }

    //// Network navigation ////

    private void goUp() {
        if (getDocument().getActiveNetworkPath().equals("/")) return;
        Iterable<String> it = Splitter.on("/").split(getDocument().getActiveNetworkPath());
        int parts = Iterables.size(it);
        String path = parts - 1 > 1 ? Joiner.on("/").join(Iterables.limit(it, parts - 1)) : "/";
        getDocument().setActiveNetwork(path);
    }

    //// Input Events ////

    private class KeyHandler extends KeyAdapter {

        public void keyTyped(KeyEvent e) {
            switch (e.getKeyChar()) {
                case KeyEvent.VK_BACK_SPACE:
                    getDocument().deleteSelection();
                    break;
            }
        }

        public void keyPressed(KeyEvent e) {
            int keyCode = e.getKeyCode();
            if (keyCode == KeyEvent.VK_SHIFT) {
                isShiftPressed = true;
            } else if (keyCode == KeyEvent.VK_ALT) {
                isAltPressed = true;
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

        public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
                isShiftPressed = false;
            } else if (e.getKeyCode() == KeyEvent.VK_ALT) {
                isAltPressed = false;
            }
        }

    }

    private class MouseHandler implements MouseListener, MouseMotionListener {

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
                showPopup(e);
            } else if (isDragTrigger(e)) {
            } else {
                Point2D pt = inverseViewTransformPoint(e.getPoint());

                // Check if we're over an output port.
                connectionOutput = getNodeWithOutputPortAt(pt);
                if (connectionOutput != null) return;

                // Check if we're over a connected input port.
                connectionInput = getInputPortAt(pt, false);
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
            if (e.isPopupTrigger()) {
                showPopup(e);
            } else {
                isDraggingNodes = false;
                isDragSelecting = false;
                if (isAltPressed)
                    getDocument().stopEditing();
                if (connectionOutput != null && connectionInput != null) {
                    getDocument().connect(connectionOutput.getName(), connectionInput.node, connectionInput.port);
                }
                connectionOutput = null;
                repaint();
            }
        }

        public void mouseEntered(MouseEvent e) {
            grabFocus();
        }

        public void mouseExited(MouseEvent e) {
        }

        public void mouseDragged(MouseEvent e) {
            Point2D pt = inverseViewTransformPoint(e.getPoint());
            // Panning the view has the first priority.
            if (isPanning()) return;

            if (connectionOutput != null) {
                repaint();
                connectionInput = getInputPortAt(pt, true);
                connectionPoint = pt;
                overOutput = getNodeWithOutputPortAt(pt);
                overInput = getInputPortAt(pt, true);
                if (overInput != null && connectionOutput.getName().equals(overInput.node)) {
                    overInput = null;
                }
            }

            if (startDragging) {
                startDragging = false;
                Node pressedNode = getNodeAt(pt);
                if (pressedNode != null) {
                    if (selectedNodes == null || selectedNodes.isEmpty() || !selectedNodes.contains(pressedNode.getName())) {
                        singleSelect(pressedNode);
                    }
                    if (isAltPressed) {
                        getDocument().startEdits("Copy Node");
                        getDocument().dragCopy();
                    }
                    isDraggingNodes = true;
                    dragPositions = selectedNodePositions();
                    dragStartPoint = pt;
                    // Change selection here.
                } else {
                    isDraggingNodes = false;
                }
            }

            if (isDraggingNodes) {
                Point2D offset = minPoint(pt, dragStartPoint);
                int gridX = (int) Math.round(offset.getX() / GRID_CELL_SIZE);
                int gridY = (int) Math.round(offset.getY() / (float) GRID_CELL_SIZE);
                for (Map.Entry<String, nodebox.graphics.Point> entry : dragPositions.entrySet()) {
                    nodebox.graphics.Point originalPosition = entry.getValue();
                    if (originalPosition == null) {
                        // Just in case...
                        originalPosition = nodebox.graphics.Point.ZERO;
                    }
                    nodebox.graphics.Point newPosition = originalPosition.moved(gridX, gridY);
                    Node node = findNodeWithName(entry.getKey());
                    if (node != null) {
                        // This avoids an issue where you delete a node while dragging.
                        getDocument().setNodePosition(node, newPosition);
                    }
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
            overInput = getInputPortAt(pt, false);
            overComment = getNodeWithCommentAt(pt);
            // It is probably very inefficient to repaint the view every time the mouse moves.
            repaint();
        }
    }


    public void zoom(double scaleDelta) {
        // todo: implement
    }

    public boolean containsPoint(Point point) {
        return isVisible() && getBounds().contains(point);
    }

    private void showPopup(MouseEvent e) {
        Point pt = e.getPoint();
        NodePort nodePort = getInputPortAt(inverseViewTransformPoint(pt), false);
        if (nodePort != null) {
            JPopupMenu pMenu = new JPopupMenu();
            pMenu.add(new PublishAction(nodePort));

            if (findNodeWithName(nodePort.getNode()).hasPublishedInput(nodePort.getPort()))
                pMenu.add(new GoToPortAction(nodePort));

            pMenu.show(this, e.getX(), e.getY());
        } else {
            Node pressedNode = getNodeAt(inverseViewTransformPoint(pt));
            if (pressedNode != null) {
                JPopupMenu nodeMenu = createNodeMenu(pressedNode);
                nodeMenuLocation = pt;
                nodeMenu.show(this, e.getX(), e.getY());
            } else {
                networkMenuLocation = pt;
                networkMenu.show(this, e.getX(), e.getY());
            }
        }
    }

    private ImmutableMap<String, nodebox.graphics.Point> selectedNodePositions() {
        ImmutableMap.Builder<String, nodebox.graphics.Point> b = ImmutableMap.builder();
        for (String nodeName : selectedNodes) {
            b.put(nodeName, findNodeWithName(nodeName).getPosition());
        }
        return b.build();
    }

    private Point2D minPoint(Point2D a, Point2D b) {
        return new Point2D.Double(a.getX() - b.getX(), a.getY() - b.getY());
    }

    private class FocusHandler extends FocusAdapter {

        @Override
        public void focusLost(FocusEvent focusEvent) {
            isShiftPressed = false;
            isAltPressed = false;
        }

    }

    private static class NodeImageCacheLoader extends CacheLoader<Node, BufferedImage> {
        private NodeRepository nodeRepository;

        private NodeImageCacheLoader(NodeRepository nodeRepository) {
            this.nodeRepository = nodeRepository;
        }

        @Override
        public BufferedImage load(Node node) throws Exception {
            for (NodeLibrary library : nodeRepository.getLibraries()) {
                BufferedImage img = findNodeImage(library, node);
                if (img != null) {
                    return img;
                }
            }
            if (node.getPrototype() != null) {
                return load(node.getPrototype());
            } else {
                return nodeGeneric;
            }
        }
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
        }

        public void actionPerformed(ActionEvent e) {
            goUp();
        }
    }

    private class PublishAction extends AbstractAction {
        private NodePort nodePort;

        private PublishAction(NodePort nodePort) {
            super(getActiveNetwork().hasPublishedInput(nodePort.getNode(), nodePort.getPort()) ? "Unpublish" : "Publish");
            this.nodePort = nodePort;
        }

        public void actionPerformed(ActionEvent e) {
            if (getActiveNetwork().hasPublishedInput(nodePort.getNode(), nodePort.getPort())) {
                unpublish();
            } else {
                publish();
            }
        }

        private void unpublish() {
            Port port = getActiveNetwork().getPortByChildReference(nodePort.getNode(), nodePort.getPort());
            getDocument().unpublish(port.getName());
        }

        private void publish() {
            String s = JOptionPane.showInputDialog(NetworkView.this, "Publish as:", nodePort.getPort());
            if (s == null || s.length() == 0)
                return;
            getDocument().publish(nodePort.getNode(), nodePort.getPort(), s);
        }
    }

    private class GoToPortAction extends AbstractAction {
        private NodePort nodePort;

        private GoToPortAction(NodePort nodePort) {
            super("Go to Port");
            this.nodePort = nodePort;
        }

        public void actionPerformed(ActionEvent e) {
            getDocument().setActiveNetwork(Node.path(getDocument().getActiveNetworkPath(), nodePort.getNode()));

            // todo: visually indicate the origin port.
            // Node node = findNodeWithName(nodePort.getNode());
            // Port publishedPort = node.getInput(nodePort.getPort());
            // publishedPort.getChildNodeName()
            // publishedPort.getChildPortName()
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
            if (node != null) {
                renameNode(node);
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
        }

        public void actionPerformed(ActionEvent e) {
            Node node = getNodeAt(inverseViewTransformPoint(nodeMenuLocation));
            String childPath = Node.path(getDocument().getActiveNetworkPath(), node.getName());
            getDocument().setActiveNetwork(childPath);
        }
    }

    private class GroupIntoNetworkAction extends AbstractAction {
        private Point gridPoint;

        private GroupIntoNetworkAction(Point gridPoint) {
            super("Group into Network");
            this.gridPoint = gridPoint;
        }

        public void actionPerformed(ActionEvent e) {
            nodebox.graphics.Point position;
            if (gridPoint == null)
                position = getNodeAt(inverseViewTransformPoint(nodeMenuLocation)).getPosition();
            else
                position = new nodebox.graphics.Point(gridPoint);
            getDocument().groupIntoNetwork(position);
        }
    }

    private class RemoveCommentAction extends AbstractAction {
        private RemoveCommentAction() {
            super("Remove Comment");
        }

        public void actionPerformed(ActionEvent e) {
            Node node = getNodeAt(inverseViewTransformPoint(nodeMenuLocation));
            if (node != null) {
                getDocument().setNodeComment(node, "");
                // Since this node no longer has a comment, we're no longer over a comment node.
                overComment = null;
                repaint();
            }
        }
    }

    private class EditCommentAction extends AbstractAction {
        private EditCommentAction() {
            super("Edit Comment");
        }

        public void actionPerformed(ActionEvent e) {
            Node node = getNodeAt(inverseViewTransformPoint(nodeMenuLocation));
            if (node != null) {
                editComment(node);
            }
        }
    }

    private class AddCommentAction extends AbstractAction {
        private AddCommentAction() {
            super("Add Comment");
        }

        public void actionPerformed(ActionEvent e) {
            Node node = getNodeAt(inverseViewTransformPoint(nodeMenuLocation));
            if (node != null) {
                addComment(node);
                repaint();
            }
        }
    }

    private class HelpAction extends AbstractAction {
        private HelpAction() {
            super("Help");
        }

        public void actionPerformed(ActionEvent e) {
            Node node = getNodeAt(inverseViewTransformPoint(nodeMenuLocation));
            Node prototype = node.getPrototype();
            for (NodeLibrary library : document.getNodeRepository().getLibraries()) {
                if (library.getRoot().hasChild(prototype)) {
                    String libraryName = library.getName();
                    String nodeName = prototype.getName();
                    String nodeRef = String.format("http://nodebox.net/node/reference/%s/%s", libraryName, nodeName);
                    Platform.openURL(nodeRef);
                    return;
                }
            }
            JOptionPane.showMessageDialog(NetworkView.this, "There is no reference documentation for node " + prototype, Application.NAME, JOptionPane.WARNING_MESSAGE);
        }
    }
}
