package nodebox.client;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;

public class SingleLineSplitter extends NSplitter {

    private Color lineColor;

    public SingleLineSplitter(Orientation orientation, JComponent newLeftComponent, JComponent newRightComponent) {
        super(orientation, newLeftComponent, newRightComponent);
        setDividerSize(1);
        lineColor = Theme.DEFAULT_SPLIT_COLOR;
    }

    public Color getLineColor() {
        return lineColor;
    }

    public void setLineColor(Color lineColor) {
        this.lineColor = lineColor;
        repaint();
    }

    @Override
    protected Divider createDivider() {
        return new PaneSpliterDivider();
    }

    private class PaneSpliterDivider extends Divider {

        @Override
        public void paintComponent(Graphics g) {
            g.setColor(lineColor);
            Rectangle r = g.getClipBounds();
            if (getOrientation() == Orientation.HORIZONTAL) {
                g.drawLine(r.x, r.y, r.x, r.height);

            } else {
                g.drawLine(r.x, r.y, r.x, r.height);
            }
        }
    }

}
