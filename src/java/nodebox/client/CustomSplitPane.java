package nodebox.client;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;

/**
 * Better looking split divider.
 */
public class CustomSplitPane extends JSplitPane {

    public CustomSplitPane(int orientation, Component c1, Component c2) {
        super(orientation, c1, c2);
        setUI(createUI());
        setContinuousLayout(true);
        setBorder(null);
        setResizeWeight(0.5);
        setDividerLocation(0.5);
        setDividerSize(7);
    }

    protected BasicSplitPaneUI createUI() {
        return new CustomSplitPaneUI();
    }

    private class CustomSplitPaneUI extends BasicSplitPaneUI {

        @Override
        public BasicSplitPaneDivider createDefaultDivider() {
            return new BasicSplitPaneDivider(CustomSplitPaneUI.this) {
                @Override
                public void paint(Graphics g) {
                    Rectangle r = getBounds();
                    g.setColor(Color.LIGHT_GRAY);
                    g.fillRect(r.x, r.y, r.width, r.height);
                    g.setColor(Color.GRAY);
                    if (getOrientation() == JSplitPane.VERTICAL_SPLIT) {
                        g.drawLine(0, 0, r.width, 0);
                        g.drawLine(0, dividerSize - 1, r.width, dividerSize - 1);
                    } else {
                        g.drawLine(0, 0, 0, r.height);
                        g.drawLine(dividerSize - 1, 0, dividerSize - 1, r.height);
                    }
                }
            };
        }
    }

}
