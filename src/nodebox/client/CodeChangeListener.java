package nodebox.client;

import nodebox.node.Node;

import java.util.EventListener;

public interface CodeChangeListener extends EventListener {

    public void codeChanged(Node node, boolean changed);

}
