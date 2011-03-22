package nodebox.client;

import nodebox.graphics.Grob;
import nodebox.node.Node;

import javax.swing.*;
import java.awt.*;

public class ExportViewer extends JFrame {
    private Node network;
    private Viewer viewer;

    public ExportViewer(Node exportNetwork) {
        super("Exporting...");
        network = exportNetwork;
        viewer = new Viewer();
        getContentPane().add(viewer);
        setSize(600, 600);
    }

    public void updateFrame() {
        viewer.repaint();
    }

    private class Viewer extends JComponent {
        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Object outputValue = network.getOutputValue();
            g2.translate(getWidth() / 2, getHeight() / 2);
            if (outputValue instanceof Grob) {
                if (outputValue instanceof nodebox.graphics.Canvas)
                    g2.clip(((Grob) outputValue).getBounds().getRectangle2D());
                ((Grob) outputValue).draw(g2);
            }
        }
    }
}
