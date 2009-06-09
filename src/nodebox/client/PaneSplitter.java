package nodebox.client;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class PaneSplitter extends JSplitPane {

    private static Image splitterHorizontalBackground, splitterVerticalBackground;

    static {
        try {
            splitterHorizontalBackground = ImageIO.read(new File("res/splitter-h-background.png"));
            splitterVerticalBackground = ImageIO.read(new File("res/splitter-v-background.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public PaneSplitter(int orientation, Component newLeftComponent, Component newRightComponent) {
        super(orientation, newLeftComponent, newRightComponent);
        setContinuousLayout(true);
        setDividerLocation(0.5);
        setResizeWeight(0.5);
        setDividerSize(7);
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
                g.drawImage(splitterHorizontalBackground, r.x, r.y, 7, r.height, null);
//                g.setColor(Theme.getInstance().getBorderHighlightColor());
//                g.fillRect(r.x, r.y, 1, r.height);
//                g.setColor(Theme.getInstance().getBorderColor());
//                g.fillRect(r.x + 1, r.y, 1, r.height);

            } else {
                g.drawImage(splitterVerticalBackground, r.x, r.y, r.width, 7, null);
//                g.setColor(Theme.getInstance().getBorderHighlightColor());
//                g.fillRect(r.x, r.y, r.width, 1);
//                g.setColor(Theme.getInstance().getBorderColor());
//                g.fillRect(r.x, r.y + 1, r.width, 1);
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
