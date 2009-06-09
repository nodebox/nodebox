package nodebox.client;

import nodebox.node.Node;

public class RenderThread extends Thread {

    private Node node;
    private boolean shouldRender = false;
    private boolean isRendering = false;
    private boolean running = true;

    @Override
    public void run() {
        while (running) {
            if (shouldRender && !isRendering)
                doRender();
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
            }
        }
    }

    public void render(Node node) {
        this.node = node;
        shouldRender = true;
    }

    public Node getNode() {
        return node;
    }

    public boolean isRendering() {
        return isRendering;
    }

    public void shutdown() {
        running = false;
    }

    private void doRender() {
        if (isRendering) return;
        isRendering = true;
        node.update();
        shouldRender = false;
        isRendering = false;
    }

}
