package nodebox.client;

import nodebox.node.Node;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class AddressBar extends JPanel implements MouseListener {

    public static Image addressGradient;
    public static Image addressArrow;

    static {
        try {
            addressGradient = ImageIO.read(new File("res/address-gradient.png"));
            addressArrow = ImageIO.read(new File("res/address-arrow.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //private ArrayList<Node> parts = new List<Node>[]{"root", "poster", "background"};
    private int[] positions;
    private int armed = -1;
    private OnPartClickListener onPartClickListener;
    private Node activeNetwork;
    private JProgressBar progressBar;

    public AddressBar() {
        addMouseListener(this);
        setMinimumSize(new Dimension(0, 25));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        setPreferredSize(new Dimension(Integer.MAX_VALUE, 25));
        setLayout(null);
        progressBar = new JProgressBar();
        progressBar.putClientProperty("JProgressBar.style", "circular");
        progressBar.setIndeterminate(true);
        progressBar.setBorderPainted(false);
        progressBar.setVisible(false);
        add(progressBar);
    }

    public Node getActiveNetwork() {
        return activeNetwork;
    }

    public void setActiveNetwork(Node activeNetwork) {
        this.activeNetwork = activeNetwork;
        repaint();
    }

    /**
     * Returns the part-click callback registered for this address bar.
     *
     * @return The callback, or null if one is not registered.
     */
    public OnPartClickListener getOnPartClickListener() {
        return onPartClickListener;
    }

    /**
     * Register a callback to be invoked when a part was clicked in the address bar.
     *
     * @param l the callback that will run.
     */
    public void setOnPartClickListener(OnPartClickListener l) {
        onPartClickListener = l;
    }

    public boolean getProgressVisible() {
        return progressBar.isVisible();
    }

    public void setProgressVisible(boolean visible) {
        progressBar.setVisible(visible);
    }

    private java.util.List<Node> getNetworkParts() {
        ArrayList<Node> parts = new ArrayList<Node>();
        if (activeNetwork == null) return parts;
        Node currentNode = activeNetwork;
        parts.add(0, currentNode);
        while (currentNode.getParent() != null) {
            parts.add(0, currentNode.getParent());
            currentNode = currentNode.getParent();
        }
        return parts;
    }

    @Override
    protected void paintComponent(Graphics g) {
        java.util.List<Node> nodes = getNetworkParts();
        positions = new int[nodes.size()];
        Graphics2D g2 = (Graphics2D) g;

        g2.setFont(Theme.SMALL_BOLD_FONT);

        g2.drawImage(addressGradient, 0, 0, getWidth(), 25, null);

        int x = 14;

        for (int i = 0; i < nodes.size(); i++) {
            Node part = nodes.get(i);
            if (i == armed) {
                g2.setColor(Theme.TEXT_ARMED_COLOR);
            } else {
                g2.setColor(Theme.TEXT_NORMAL_COLOR);
            }
            SwingUtils.drawShadowText(g2, part.getName(), x, 16);

            int width = (int) g2.getFontMetrics().stringWidth(part.getName());
            x += width + 5;
            positions[i] = x + 10;
            g2.drawImage(addressArrow, x, 1, null);
            x += 15;
        }
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        int mx = e.getX();
        armed = partIndex(mx);
        repaint();
    }

    public void mouseReleased(MouseEvent e) {
        armed = -1;
        int mx = e.getX();
        int partIndex = partIndex(mx);
        if (partIndex == -1) return;
        java.util.List<Node> nodes = getNetworkParts();
        Node selectedNode = nodes.get(partIndex);
        if (selectedNode != null && onPartClickListener != null)
            onPartClickListener.onPartClicked(selectedNode);
        repaint();
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
        armed = -1;
        repaint();
    }


    private int partIndex(int x) {
        if (positions == null) return -1;
        for (int i = 0; i < positions.length; i++) {
            if (x < positions[i])
                return i;
        }
        return -1;
    }

    @Override
    public void doLayout() {
        final int width = getWidth();
        progressBar.setBounds(width - 23, 3, 20, 20);
    }

    /**
     * Callback listener to be invoked when an address part has been clicked.
     */
    public static interface OnPartClickListener {

        /**
         * Called when a part has been clicked.
         *
         * @param n the part that was clicked.
         */
        public void onPartClicked(Node n);

    }

}
