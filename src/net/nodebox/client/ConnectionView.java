package net.nodebox.client;

import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.nodes.PPath;
import net.nodebox.node.Connection;

import java.awt.*;
import java.awt.event.InputEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class ConnectionView extends PPath implements Selectable, PropertyChangeListener {

    private NetworkView networkView;
    private Connection connection;
    private boolean selected;
    private NodeView inputNodeView;
    private NodeView outputNodeView;

    public ConnectionView(NetworkView networkView, Connection connection) {
        this.networkView = networkView;
        this.connection = connection;
        addInputEventListener(new ConnectionHandler());
        //updateBounds();
        updatePath();
        inputNodeView = networkView.getNodeView(connection.getInputNode());
        outputNodeView = networkView.getNodeView(connection.getOutputNode());
        assert (inputNodeView != null);
        assert (outputNodeView != null);
    }

    public NetworkView getNetworkView() {
        return networkView;
    }

    public Connection getConnection() {
        return connection;
    }

//    public void updateBounds() {
//        double x0 = connection.getOutputNode().getX();
//        double y0 = connection.getOutputNode().getY();
//        double x1 = connection.getInputNode().getX();
//        double y1 = connection.getInputNode().getY();
//        Rect bounds = new Rect(x0, y0, x1 - x0, y1 - y0);
//        bounds = bounds.normalized();
//        setBounds(bounds.getRectangle2D());
//    }

    public void invalidateLayout() {
        super.invalidateLayout();
        updatePath();
    }

    public void updatePath() {
        reset();
        float x0 = NodeView.NODE_WIDTH / 2;
        float y0 = NodeView.NODE_HEIGHT;
        float x1 = (float) getWidth();
        float y1 = (float) getHeight();
        float dx = Math.abs(x1 - x0) / 2;
        moveTo(x0, y0);
        curveTo(x0, y0 + dx, x1, y1 - dx, x1, y1);
        repaint();
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean s) {
        if (selected == s) return;
        selected = s;
        if (s) {
            setStroke(new BasicStroke(3));
        } else {
            setStroke(new BasicStroke(1));
        }
    }

    public class ConnectionHandler extends PBasicInputEventHandler {
        public void mouseClicked(PInputEvent e) {
            e.getInputManager().setKeyboardFocus(this);
            if ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != InputEvent.SHIFT_DOWN_MASK) {
                networkView.deselectAll();
            }
            networkView.select(ConnectionView.this);
            e.setHandled(true);
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        invalidateLayout();
        repaint();
    }

    public void nodeMovedEvent() {
        //updateBounds();
        updatePath();
    }


}
