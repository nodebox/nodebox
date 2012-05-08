package nodebox.ui;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;

public class SingleLineSplitPane extends CustomSplitPane {

    public SingleLineSplitPane(int orientation, Component c1, Component c2) {
        super(orientation, c1, c2);
        setDividerSize(3);
    }

    @Override
    protected BasicSplitPaneUI createUI() {
        return new SingleLineSplitPaneUI();
    }

    private class SingleLineSplitPaneUI extends BasicSplitPaneUI {
        public BasicSplitPaneDivider createDefaultDivider() {
            return new BasicSplitPaneDivider(SingleLineSplitPaneUI.this) {
                @Override
                public boolean isOpaque() {
                    return false;
                }

                @Override
                public void paint(Graphics g) {
                    g.setColor(Theme.DEFAULT_SPLIT_COLOR);
                    Rectangle r = g.getClipBounds();
                    if (getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
                        int cx = r.x + r.width / 2;
                        g.drawLine(cx, r.y, cx, r.height);

                    } else {
                        int cy = r.y + r.height / 2;
                        g.drawLine(r.x, cy, r.width, cy);
                    }
                }
            };
        }
    }

}
