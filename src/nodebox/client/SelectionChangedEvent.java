package nodebox.client;

import nodebox.node.Node;

public class SelectionChangedEvent {

    public static final int NODE = 1;
    public static final int NETWORK = 2;

    public int type;
    public Node node;

    public SelectionChangedEvent(int type, Node node) {
        this.type = type;
        this.node = node;
    }

    public int getType() {
        return type;
    }

    public Node getNode() {
        return node;
    }
}
