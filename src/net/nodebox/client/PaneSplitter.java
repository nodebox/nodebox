package net.nodebox.client;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;

public class PaneSplitter extends JSplitPane {

    public PaneSplitter(int orientation, Component newLeftComponent, Component newRightComponent) {
        super(orientation, newLeftComponent, newRightComponent);
        setContinuousLayout(true);
        setDividerLocation(0.5);
        setResizeWeight(0.5);
        setDividerSize(2);
        setUI(new PaneSplitterUI());
        setBorder(BorderFactory.createEmptyBorder());
    }

    private class PaneSpliterDivider extends BasicSplitPaneDivider {
        private PaneSpliterDivider(BasicSplitPaneUI ui) {
            super(ui);
        }

        @Override
        public void paint(Graphics g) {
            Rectangle r = g.getClipBounds();
            if (getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
                g.setColor(Theme.getInstance().getBorderHighlightColor());
                g.fillRect(r.x, r.y, 1, r.height);
                g.setColor(Theme.getInstance().getBorderColor());
                g.fillRect(r.x + 1, r.y, 1, r.height);

            } else {
                g.setColor(Theme.getInstance().getBorderHighlightColor());
                g.fillRect(r.x, r.y, r.width, 1);
                g.setColor(Theme.getInstance().getBorderColor());
                g.fillRect(r.x, r.y + 1, r.width, 1);
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
