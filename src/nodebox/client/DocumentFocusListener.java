package nodebox.client;

import nodebox.node.Node;

import java.util.EventListener;

public interface DocumentFocusListener extends EventListener {

    public void currentNodeChanged(Node node);

    public void focusedNodeChanged(Node node);
}
