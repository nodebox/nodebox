package net.nodebox.client;

import net.nodebox.node.Network;

public class RenderThread extends Thread {

    private Network network;
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

    public void render(Network network) {
        this.network = network;
        shouldRender = true;
    }

    public Network getNetwork() {
        return network;
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
        network.update();
        shouldRender = false;
        isRendering = false;
    }

}
