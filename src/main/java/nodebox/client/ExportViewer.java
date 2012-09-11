package nodebox.client;

import nodebox.graphics.Grob;

import javax.swing.*;
import java.awt.*;

public class ExportViewer extends JFrame {

    private Viewer viewer;
    private Object outputValue = null;

    public ExportViewer() {
        super("Exporting...");
        viewer = new Viewer();
        getContentPane().add(viewer);
        setSize(600, 600);
    }

    public void setOutputValue(Object outputValue) {
        this.outputValue = outputValue;
        viewer.repaint();
    }

    private class Viewer extends JComponent {
        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.translate(getWidth() / 2, getHeight() / 2);
            if (outputValue instanceof Grob) {
                if (outputValue instanceof nodebox.graphics.Canvas)
                    g2.clip(((Grob) outputValue).getBounds().getRectangle2D());
                ((Grob) outputValue).draw(g2);
            }
        }
    }
}
