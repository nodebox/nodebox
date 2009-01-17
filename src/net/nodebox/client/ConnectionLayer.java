package net.nodebox.client;

import edu.umd.cs.piccolo.PLayer;
import edu.umd.cs.piccolo.util.PPaintContext;
import net.nodebox.node.Connection;
import net.nodebox.node.Network;
import net.nodebox.node.Node;

import java.awt.*;
import java.awt.geom.GeneralPath;
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
                for (Node outputNode : c.getOutputNodes()) {
                    float x0 = (float) (outputNode.getX() + NodeView.NODE_WIDTH / 2);
                    float y0 = (float) (outputNode.getY() + NodeView.NODE_HEIGHT - 2); // Compensate for selection border
                    float x1 = (float) (c.getInputNode().getX() + NodeView.NODE_WIDTH / 2);
                    float y1 = (float) (c.getInputNode().getY() + 2); // Compensate for selection border
                    float dx = Math.abs(x1 - x0) / 2;
                    p.moveTo(x0, y0);
                    p.curveTo(x0, y0 + dx, x1, y1 - dx, x1, y1);
                    g.draw(p);
                }
            }
        }
    }

}
