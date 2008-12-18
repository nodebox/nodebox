package net.nodebox.client;

import net.nodebox.graphics.Grob;
import net.nodebox.node.Network;
import net.nodebox.node.NetworkDataListener;

import javax.swing.*;
import java.awt.*;

public class Viewer extends JComponent implements NetworkDataListener {

    private Pane pane;
    private Network network;

    public Viewer(Pane pane, Network network) {
        this.pane = pane;
        this.network = network;
    }

    public Network getNetwork() {
        return network;
    }

    public void setNetwork(Network network) {
        if (this.network == network) return;
        Network oldNetwork = this.network;
        if (oldNetwork != null) {
            oldNetwork.removeNetworkDataListener(this);
        }
        this.network = network;
        if (this.network == null) return;
        network.addNetworkDataListener(this);
        repaint();
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (getNetwork() == null) return;
        Object outputValue = getNetwork().getOutputValue();
        if (outputValue instanceof Grob) {
            ((Grob) outputValue).draw(g2);
        }
    }

    //// Network data events ////

    public void networkDirty(Network network) {
        // The network is dirty, but we wait for the document to update the network.
        // This will send the networkUpdated event.
    }

    public void networkUpdated(Network network) {
        if (network == getNetwork()) {
            repaint();
        }
    }
}
