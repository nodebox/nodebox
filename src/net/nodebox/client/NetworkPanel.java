package net.nodebox.client;

import edu.umd.cs.piccolo.PCanvas;
import net.nodebox.node.Network;
import net.nodebox.node.Node;
import net.nodebox.node.NodeManager;
import net.nodebox.node.vector.RectType;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;


public class NetworkPanel extends PCanvas implements Observer {

    public static final String SELECT_PROPERTY = "select";
    public static final String HIGHLIGHT_PROPERTY = "highlight";
    public static final String RENDER_PROPERTY = "render";
    public static final String NETWORK_PROPERTY = "network";

    private Network network;
    private ArrayList<Selectable> selection = new ArrayList<Selectable>();

    public NetworkPanel() {
    }

    public Network getNetwork() {
        return network;
    }

    public void setNetwork(Network network) {
        this.network = network;
        //rebuildNodes();
    }

//    private void rebuildNodes() {
//        if (this.network == network) return;
//        Network old = this.network;
//        if (old != null) {
//            old.removeNetworkListener(handler);
//        }
//        getLayer().removeAllChildren();
//        deselectAll();
//        setHighlight(null);
//        if (network != null) {
//            // Add nodes
//            Iterator nodeIter = network.getNodes();
//            while (nodeIter.hasNext()) {
//                Node node = (Node) nodeIter.next();
//                NodeView nv = new NodeView(this, node);
//                getLayer().addChild(nv);
//            }
//            /*
//            // Add connections
//            Iterator connIter = network.getConnections();
//            while (connIter.hasNext()) {
//                Connection conn = (Connection) connIter.next();
//                Iterator outIter = conn.getOutputs();
//                while (outIter.hasNext()) {
//                    Parameter output = (Parameter) outIter.next();
//                    ParameterView iv = findParameterView(conn.getInput());
//                    ParameterView ov = findParameterView(output);
//                    ConnectionView cv = new ConnectionView(this, iv, ov);
//                    getLayer().addChild(cv);
//                }
//            }*/
//            network.addObserver(this);
//            // network.addNetworkListener(handler);
//        }
//        validate();
//        firePropertyChange(NETWORK_PROPERTY, old, network);
//
//    }

    public void selectAll() {
        for (Iterator childIter = getLayer().getChildrenIterator(); childIter.hasNext();) {
            Object child = childIter.next();
            if (!(child instanceof Selectable))
                continue;
            Selectable s = (Selectable) childIter.next();
            assert s.isSelected();
            s.setSelected(false);
            selection.add(s);
        }
        firePropertyChange(SELECT_PROPERTY, selection, null);
    }

    public void deselectAll() {
        for (Iterator<Selectable> selectionIter = selection.iterator(); selectionIter.hasNext();) {
            Selectable s = selectionIter.next();
            assert s.isSelected();
            s.setSelected(false);
        }
        selection.clear();

        firePropertyChange(SELECT_PROPERTY, selection, null);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        Node n = new RectType(new NodeManager()).createNode();
        ParameterView p = new ParameterView();
        p.setNode(n);
        frame.setContentPane(p);
        frame.setSize(500, 500);
        frame.setVisible(true);
    }

    public void update(Observable o, Object arg) {

    }
}
