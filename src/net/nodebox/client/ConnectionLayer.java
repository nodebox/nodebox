package net.nodebox.client;

import edu.umd.cs.piccolo.PLayer;
import edu.umd.cs.piccolo.util.PPaintContext;
import net.nodebox.graphics.Rect;
import net.nodebox.node.Connection;
import net.nodebox.node.Network;
import net.nodebox.node.Node;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
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
        super.paint(pPaintContext);
        Graphics2D g = pPaintContext.getGraphics();
        Network net = networkView.getNetwork();
        for (Node n : net.getNodes()) {
            for (Connection c : n.getOutputConnections()) {
                GeneralPath p = new GeneralPath();
                if (selections.contains(c)) {
                    g.setStroke(new BasicStroke(3));
                    g.setColor(Theme.getInstance().getActionColor());
                } else {
                    g.setStroke(new BasicStroke(1));
                    if (c.isExplicit()) {
                        g.setColor(Theme.getInstance().getConnectionColor());
                    } else {
                        g.setColor(Theme.getInstance().getImplicitConnectionColor());
                    }
                }
                float x0 = (float) (c.getOutputNode().getX() + NodeView.NODE_WIDTH / 2);
                float y0 = (float) (c.getOutputNode().getY() + NodeView.NODE_HEIGHT);
                float x1 = (float) (c.getInputNode().getX() + NodeView.NODE_WIDTH / 2);
                float y1 = (float) (c.getInputNode().getY());
                float dx = Math.abs(x1 - x0) / 2;
                p.moveTo(x0, y0);
                p.curveTo(x0, y0 + dx, x1, y1 - dx, x1, y1);
                g.draw(p);
            }
        }
    }

    private Rectangle2D boundsForConnection(Connection c) {
        double x0 = c.getOutputNode().getX();
        double y0 = c.getOutputNode().getY();
        double x1 = c.getInputNode().getX();
        double y1 = c.getInputNode().getY();
        Rect bounds = new Rect(x0, y0, x1 - x0, y1 - y0);
        bounds = bounds.normalized();
        return bounds.getRectangle2D();
    }

    public void select(Rectangle2D bounds) {
        deselectAll();
        Network net = networkView.getNetwork();
        for (Node n : net.getNodes()) {
            for (Connection c : n.getOutputConnections()) {
                if (bounds.intersects(boundsForConnection(c))) {
                    addSelection(c);
                }
            }
        }
    }

}
