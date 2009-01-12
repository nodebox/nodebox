package net.nodebox.client;

import net.nodebox.node.Network;
import net.nodebox.node.NetworkDataListener;
import net.nodebox.node.Node;

import javax.swing.*;
import java.awt.*;

public class LoggingPane extends Pane implements NetworkDataListener {

    private PaneHeader paneHeader;
    private JTextArea loggingArea;
    private Network network;

    public LoggingPane(NodeBoxDocument document) {
        this();
        setDocument(document);
    }

    public LoggingPane() {
        setLayout(new BorderLayout(0, 0));
        paneHeader = new PaneHeader(this);
        loggingArea = new JTextArea(80, 30);
        loggingArea.setFont(PlatformUtils.getInfoFont());
        loggingArea.setEditable(false);
        JScrollPane loggingScroll = new JScrollPane(loggingArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        add(paneHeader, BorderLayout.NORTH);
        add(loggingScroll, BorderLayout.CENTER);
    }

    @Override
    public void activeNetworkChanged(Network activeNetwork) {
        if (network != null) {
            network.removeNetworkDataListener(this);
        }
        network = activeNetwork;
        if (network != null)
            network.addNetworkDataListener(this);
    }

    public void networkDirty(Network network) {
    }

    public void networkUpdated(Network network) {
        StringBuffer sb = new StringBuffer();
        for (Node.Message m : network.getMessages()) {
            sb.append(m.getLevel().toString());
            sb.append(": ");
            sb.append(m.getMessage());
            sb.append("\n");
        }
        loggingArea.setText(sb.toString());
    }

    public Pane clone() {
        return new LoggingPane(getDocument());
    }

}
