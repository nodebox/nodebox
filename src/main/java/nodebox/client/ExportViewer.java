package nodebox.client;

import nodebox.graphics.Grob;

import javax.swing.*;
import java.awt.*;

public class ExportViewer extends JFrame {

    private Viewer viewer;
    private java.util.List<Object> outputValues = null;

    public ExportViewer() {
        super("Exporting...");
        viewer = new Viewer();
        getContentPane().add(viewer);
        setSize(600, 600);
    }

    public void setOutputValues(java.util.List<Object> outputValues) {
        this.outputValues = outputValues;
        viewer.repaint();
    }

    private class Viewer extends JComponent {
        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.translate(getWidth() / 2, getHeight() / 2);
            for (Object outputValue : outputValues) {
                if (outputValue instanceof Grob) {
                    // todo: handle canvas clipping
                    ((Grob) outputValue).draw(g2);
                }
            }
        }
    }
}
