package nodebox.client;

import edu.umd.cs.piccolo.PLayer;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.util.PPaintContext;
import nodebox.node.Connection;
import nodebox.node.Node;
import nodebox.node.Port;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class ConnectionLayer extends PLayer {

    private NetworkView networkView;
    private Connection selection = null;

    public ConnectionLayer(NetworkView networkView) {
        this.networkView = networkView;
    }

    public NetworkView getNetworkView() {
        return networkView;
    }

    public NodeBoxDocument getDocument() {
        return networkView.getDocument();
    }

    public void select(Connection connection) {
        selection = connection;
    }

    public void deselect() {
        selection = null;
        repaint();
    }

    public boolean hasSelection() {
        return selection != null;
    }

    @Override
    protected void paint(PPaintContext pPaintContext) {
        // TODO: Draw parameter dependencies using implicitColor.
        super.paint(pPaintContext);
        Graphics2D g = pPaintContext.getGraphics();
        Node activeNetwork = networkView.getActiveNetwork();
        if (activeNetwork == null) return;
        for (Connection c : activeNetwork.getConnections()) {
            if (selection == c) {
                g.setColor(Theme.CONNECTION_ACTION_COLOR);
            } else {
                g.setColor(Theme.CONNECTION_DEFAULT_COLOR);
            }
            paintConnection(g, c.getOutputNode(), c.getInput());
        }
        // Draw temporary connection
        if (networkView.isConnecting() && networkView.getConnectionPoint() != null) {
            // Set the color to some kind of yellow
            g.setColor(Theme.CONNECTION_CONNECTING_COLOR);
            Point2D pt = networkView.getConnectionPoint();
            ConnectionLayer.paintConnection(g, networkView.getConnectionSource().getNode(), (float) pt.getX(), (float) pt.getY());
        }
    }

    public static void paintConnection(Graphics2D g, Node outputNode, Port input) {
        GeneralPath p = connectionPath(outputNode, input);
        paintConnectionPath(g, p);
    }

    public static void paintConnection(Graphics2D g, Node outputNode, float x1, float y1) {
        GeneralPath p = connectionPath(outputNode, x1, y1);
        paintConnectionPath(g, p);
    }

    public static void paintConnectionPath(Graphics2D g, GeneralPath p) {
        g.setStroke(new BasicStroke(2));
        g.draw(p);
    }

    public static GeneralPath connectionPath(Node outputNode, Port input) {
        float x1 = (float) (input.getNode().getX() + 1); // Compensate for selection border
        float y1 = (float) (input.getNode().getY() + NodeView.getVerticalOffsetForPort(input) + NodeView.NODE_PORT_HEIGHT / 2);
        return connectionPath(outputNode, x1, y1);
    }

    public static GeneralPath connectionPath(Node outputNode, float x1, float y1) {
        GeneralPath p = new GeneralPath();
        // Start position is at the middle right of the node.
        float x0 = (float) (outputNode.getX() + NodeView.NODE_FULL_SIZE - 1); // Compensate for selection border
        float y0 = (float) (outputNode.getY() + NodeView.NODE_FULL_SIZE / 2);
        // End position is at the middle left of the node.
        float dx = Math.abs(y1 - y0) / 2;
        p.moveTo(x0, y0);
        p.curveTo(x0 + dx, y0, x1 - dx, y1, x1, y1);
        return p;
    }

    public Connection clickedConnection(Point2D p) {
        // Make a rectangle out of the point that is slightly larger than the point itself.
        Rectangle2D clickRect = new Rectangle2D.Double(p.getX() - 3, p.getY() - 3, 6, 6);
        Node node = networkView.getActiveNetwork();
        for (Connection c : node.getConnections()) {
            GeneralPath gp = connectionPath(c.getOutputNode(), c.getInput());
            if (gp.intersects(clickRect))
                return c;
        }
        return null;
    }

    /**
     * Delete the selected connection.
     */
    public void deleteSelected() {
        if (selection == null) return;
        getDocument().disconnect(selection);
    }

    /**
     * Handle the mouse clicked event.
     * <p/>
     * This method is called from the NetworkView to inform us that the background layer was clicked.
     * Check if there is a connection under the mouse cursor and select it.
     *
     * @param e the input event
     */
    public void mouseClickedEvent(PInputEvent e) {
        Connection c = clickedConnection(e.getPosition());
        if (c == null) {
            deselect();
        } else {
            select(c);
            repaint();
        }
    }

}
