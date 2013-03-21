package nodebox.client;

import nodebox.client.visualizer.Visualizer;
import nodebox.client.visualizer.VisualizerFactory;
import nodebox.graphics.Grob;
import nodebox.util.ListUtils;

import javax.swing.*;
import java.awt.*;

public class ExportViewer extends JFrame {

    private Viewer viewer;
    private java.util.List<?> outputValues = null;

    public ExportViewer() {
        super("Exporting...");
        viewer = new Viewer();
        getContentPane().add(viewer);
        setSize(600, 600);
    }

    public void setOutputValues(java.util.List<?> outputValues) {
        this.outputValues = outputValues;
        viewer.repaint();
    }

    private class Viewer extends JComponent {
        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.translate(getWidth() / 2, getHeight() / 2);
            Visualizer v = VisualizerFactory.getVisualizer(outputValues, ListUtils.listClass(outputValues));
            // todo: handle canvas clipping
            v.draw(g2, outputValues);
        }
    }
}
