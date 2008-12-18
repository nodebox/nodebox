package net.nodebox.client;

import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.*;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PPaintContext;
import net.nodebox.node.Connection;
import net.nodebox.node.Network;
import net.nodebox.node.NetworkEventListener;
import net.nodebox.node.Node;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class NetworkView extends PCanvas implements NetworkEventListener {

    public static final String SELECT_PROPERTY = "select";
    public static final String HIGHLIGHT_PROPERTY = "highlight";
    public static final String RENDER_PROPERTY = "render";
    public static final String NETWORK_PROPERTY = "network";

    private Pane pane;
    private Network network;
    private List<Selectable> selection = new ArrayList<Selectable>();
    private ConnectionLayer connectionLayer;
    private NodeView highlight;
    private SelectionHandler selectionHandler = new SelectionHandler();
    private SelectionMarker selectionMarker;
    private JPopupMenu popup;
    private PopupHandler popupHandler = new PopupHandler();

    public NetworkView(Pane pane, Network network) {
        this.pane = pane;
        this.network = network;
        setBackground(Theme.getInstance().getViewBackgroundColor());
        addInputEventListener(selectionHandler);
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
        setZoomEventHandler(new PZoomEventHandler() {
            public void processEvent(final PInputEvent evt, final int i) {
                if (evt.isMouseWheelEvent()) {
                    double currentScale = evt.getCamera().getViewScale();
                    double scaleDelta = 1D - 0.1 * evt.getWheelRotation();
                    double newScale = currentScale * scaleDelta;
                    final Point2D p = evt.getPosition();
                    if (newScale > 0.2 && newScale < 2.0) {
                        evt.getCamera().scaleViewAboutPoint(scaleDelta, p.getX(), p.getY());
                    }
                }
            }
        });
        initPopupMenu();
    }

    private void initPopupMenu() {
        popup = new JPopupMenu();
        popup.add(new RemoveNodeAction());

    }

    @Override
    public void setBounds(int i, int i1, int i2, int i3) {
        super.setBounds(i, i1, i2, i3);
        connectionLayer.setBounds(getBounds());
    }

    public Pane getPane() {
        return pane;
    }

    public Network getNetwork() {
        return network;
    }

    public void setNetwork(Network network) {
        if (this.network == network) return;
        Network oldNetwork = this.network;
        if (oldNetwork != null) {
            oldNetwork.removeNetworkEventListener(this);
        }
        this.network = network;
        getLayer().removeAllChildren();
        deselectAll();
        setHighlight((NodeView) null);
        if (network == null) return;
        network.addNetworkEventListener(this);
        // Add nodes
        for (Node n : network.getNodes()) {

            NodeView nv = new NodeView(this, n);
            getLayer().addChild(nv);
        }
        validate();
        firePropertyChange(NETWORK_PROPERTY, oldNetwork, network);
    }

    //// View queries ////

    public NodeView getNodeView(Node n) {
        for (Object child : getLayer().getChildrenReference()) {
            if (!(child instanceof NodeView)) continue;
            if (((NodeView) child).getNode() == n)
                return (NodeView) child;
        }
        return null;
    }

    //// Selections ////

    public void select(Selectable v) {
        if (v == null) return;
        selection.add(v);
        v.setSelected(true);
        firePropertyChange(SELECT_PROPERTY, null, v);
    }

    public void deselect(Selectable v) {
        if (v == null) return;
        selection.remove(v);
        v.setSelected(false);
        firePropertyChange(SELECT_PROPERTY, v, null);
    }

    public void selectAll() {
        for (Object child : getLayer().getChildrenReference()) {
            if (!(child instanceof Selectable)) continue;
            Selectable s = (Selectable) child;
            s.setSelected(true);
            selection.add(s);
        }
        firePropertyChange(SELECT_PROPERTY, selection, null);
    }

    public void deselectAll() {
        for (Object child : getLayer().getChildrenReference()) {
            if (!(child instanceof Selectable)) continue;
            Selectable s = (Selectable) child;
            s.setSelected(false);
            selection.remove(s);
        }
        connectionLayer.deselectAll();
        connectionLayer.repaint();
        firePropertyChange(SELECT_PROPERTY, selection, null);
    }

    //// Highlight ////

    public void setHighlight(Node n) {
        setHighlight(getNodeView(n));
    }

    public void setHighlight(NodeView n) {
        if (highlight == n) return;
        NodeView old = highlight;
        if (highlight != null) {
            highlight.setHighlighted(false);
        }
        highlight = n;
        if (highlight != null) {
            highlight.setHighlighted(true);
        }
        firePropertyChange(HIGHLIGHT_PROPERTY, old, n);
    }

    public NodeView getHighlight() {
        return highlight;
    }


    //// Events ////

    public void nodeAdded(Network source, Node node) {
        NodeView nv = new NodeView(this, node);
        getLayer().addChild(nv);
    }

    public void nodeRemoved(Network source, Node node) {
        NodeView nv = getNodeView(node);
        if (nv == null) return;
        getLayer().removeChild(nv);
    }

    public void connectionAdded(Network source, Connection connection) {
        connectionLayer.repaint();
    }

    public void connectionRemoved(Network source, Connection connection) {
        connectionLayer.repaint();
    }

    public void nodeChanged(Network source, Node node) {
        // We assume the change event is a move.
        // Find the NodeView related to this change event.
        NodeView nv = getNodeView(node);
        if (!nv.getOffset().equals(node.getPosition().getPoint2D())) {
            nv.setOffset(node.getX(), node.getY());
        }
        connectionLayer.repaint();
    }

    public void renderedNodeChanged(Network source, Node node) {
        repaint();
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
        public void mouseClicked(PInputEvent e) {
            if (e.getButton() != MouseEvent.BUTTON1) return;
            deselectAll();
        }

        public void mousePressed(PInputEvent e) {
            if (e.getButton() != MouseEvent.BUTTON1) return;
            Point2D p = e.getPosition();
            selectionMarker = new SelectionMarker(p);
            getLayer().addChild(selectionMarker);
        }

        public void mouseDragged(PInputEvent e) {
            if (selectionMarker == null) return;
            deselectAll();
            Point2D prev = selectionMarker.getOffset();
            Point2D p = e.getPosition();
            selectionMarker.setWidth(p.getX() - prev.getX());
            selectionMarker.setHeight(p.getY() - prev.getY());
            ListIterator childIter = getLayer().getChildrenIterator();
            while (childIter.hasNext()) {
                Object o = childIter.next();
                if (o instanceof Selectable) {
                    Selectable s = (Selectable) o;
                    PNode n = (PNode) o;
                    if (selectionMarker.getFullBounds().intersects(n.getFullBounds())) {
                        select(s);
                    }
                }
            }
            connectionLayer.select(selectionMarker.getBounds().getBounds2D());
            connectionLayer.repaint();
        }

        public void mouseReleased(PInputEvent pInputEvent) {
            if (selectionMarker == null) return;
            getLayer().removeChild(selectionMarker);
            selectionMarker = null;
        }
    }

    private class PopupHandler extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            evaluatePopup(e);
        }

        public void mouseReleased(MouseEvent e) {
            evaluatePopup(e);
        }

        private void evaluatePopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                Point p = e.getPoint();
                popup.show(NetworkView.this, p.x, p.y);
            }
        }
    }


    class RemoveNodeAction extends AbstractAction {

        RemoveNodeAction() {
            super("Remove node");
        }

        public void actionPerformed(ActionEvent e) {

        }
    }

}
