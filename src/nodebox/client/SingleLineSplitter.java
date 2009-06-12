package nodebox.client;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;

public class SingleLineSplitter extends JSplitPane {

    private Color lineColor;

    public SingleLineSplitter(int orientation, Component newLeftComponent, Component newRightComponent) {
        super(orientation, newLeftComponent, newRightComponent);
        setContinuousLayout(true);
        setDividerSize(1);
        setUI(new PaneSplitterUI());
        setBorder(BorderFactory.createEmptyBorder());
        lineColor = new Color(139, 139, 139);
    }

    public Color getLineColor() {
        return lineColor;
    }

    public void setLineColor(Color lineColor) {
        this.lineColor = lineColor;
        repaint();
    }

    private class PaneSpliterDivider extends BasicSplitPaneDivider {
        private PaneSpliterDivider(BasicSplitPaneUI ui) {
            super(ui);
        }

        @Override
        public void paint(Graphics g) {
            g.setColor(lineColor);
            Rectangle r = g.getClipBounds();
            if (getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
                g.drawLine(r.x, r.y, r.x, r.height);

            } else {
                g.drawLine(r.x, r.y, r.x, r.height);
            }
        }
    }

    private class PaneSplitterUI extends BasicSplitPaneUI {
        @Override
        public BasicSplitPaneDivider createDefaultDivider() {
            return new PaneSpliterDivider(this);
        }
    }


}
