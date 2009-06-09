package nodebox.client;

import edu.umd.cs.piccolo.PLayer;
import edu.umd.cs.piccolo.util.PPaintContext;
import nodebox.node.Connection;
import nodebox.node.Node;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;

public class ConnectionLayer extends PLayer {

    private NetworkView networkView;
    private ArrayList<Connection> selections = new ArrayList<Connection>();

    public ConnectionLayer(NetworkView networkView) {
        this.networkView = networkView;
    }

    public NetworkView getNetworkView() {
        return networkView;
    }

    public void addSelection(Connection connection) {
        selections.add(connection);
    }

    public void removeSelection(Connection connection) {
        selections.remove(connection);
    }

    public void deselectAll() {
        selections.clear();
    }

    @Override
    protected void paint(PPaintContext pPaintContext) {
        // TODO: Draw parameter dependencies using implicitColor.
        super.paint(pPaintContext);
        Graphics2D g = pPaintContext.getGraphics();
        Node node = networkView.getNode();
        for (Node n : node.getChildren()) {
            for (Connection c : n.getDownstreamConnections()) {
                if (selections.contains(c)) {
                    g.setColor(Theme.getInstance().getActionColor());
                } else {
                    g.setColor(Theme.getInstance().getConnectionColor());
                }
                for (Node outputNode : c.getOutputNodes()) {
                    paintConnection(g, outputNode, c.getInputNode());
                }
            }
        }
        // Draw temporary connection
        if (networkView.isConnecting() && networkView.getConnectionPoint() != null) {
            // Set the color to some kind of yellow
            g.setColor(new Color(170, 167, 18));
            Point2D pt = networkView.getConnectionPoint();
            ConnectionLayer.paintConnection(g, networkView.getConnectionSource().getNode(), (float) pt.getX(), (float) pt.getY());
        }
    }

    public static void paintConnection(Graphics2D g, Node outputNode, Node inputNode) {
        float x1 = (float) (inputNode.getX() + 1); // Compensate for selection border
        float y1 = (float) (inputNode.getY() + NodeView.NODE_FULL_SIZE / 2);
        paintConnection(g, outputNode, x1, y1);
    }

    public static void paintConnection(Graphics2D g, Node outputNode, float x1, float y1) {
        g.setStroke(new BasicStroke(2));
        GeneralPath p = new GeneralPath();
        // Start position is at the middle right of the node.
        float x0 = (float) (outputNode.getX() + NodeView.NODE_FULL_SIZE - 1); // Compensate for selection border
        float y0 = (float) (outputNode.getY() + NodeView.NODE_FULL_SIZE / 2);
        // End position is at the middle left of the node.
        float dx = Math.abs(y1 - y0) / 2;
        p.moveTo(x0, y0);
        p.curveTo(x0 + dx, y0, x1 - dx, y1, x1, y1);
        g.draw(p);
    }

}
