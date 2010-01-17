package nodebox.client;

import nodebox.node.DirtyListener;
import nodebox.node.Node;
import nodebox.node.ProcessingContext;

import javax.swing.*;
import java.awt.*;

public class LoggingPane extends Pane implements DirtyListener {

    private LoggingArea loggingArea;
    private Node node;

    public LoggingPane(NodeBoxDocument document) {
        this();
        setDocument(document);
    }

    public LoggingPane() {
        setLayout(new BorderLayout(0, 0));
        PaneHeader paneHeader = new PaneHeader(this);
        loggingArea = new LoggingArea(80, 30);
        loggingArea.setFont(Theme.INFO_FONT);
        loggingArea.setEditable(false);
        JScrollPane loggingScroll = new JScrollPane(loggingArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        add(paneHeader, BorderLayout.NORTH);
        add(loggingScroll, BorderLayout.CENTER);
    }

    @Override
    public void currentNodeChanged(Node activeNetwork) {
        if (node != null) {
            node.removeDirtyListener(this);
        }
        node = activeNetwork;
        if (node != null)
            node.addDirtyListener(this);
    }

    public void nodeDirty(Node node) {
    }

    public void nodeUpdated(Node node, ProcessingContext context) {
        StringBuffer sb = new StringBuffer();
        if (node.hasError()) {
            sb.append(node.getError().toString());
        }
        loggingArea.setText(sb.toString());
    }

    public Pane clone() {
        return new LoggingPane(getDocument());
    }

    public String getPaneName() {
        return "Log";
    }

    public PaneHeader getPaneHeader() {
        return null;
    }

    public PaneView getPaneView() {
        return loggingArea;
    }

    public static class LoggingArea extends JTextArea implements PaneView {

        public LoggingArea(int rows, int columns) {
            super(rows, columns);
        }
    }
}
