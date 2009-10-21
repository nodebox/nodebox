package nodebox.client;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class PaneSplitter extends NSplitter {

    private static Image splitterHorizontalBackground, splitterVerticalBackground;

    static {
        try {
            splitterHorizontalBackground = ImageIO.read(new File("res/splitter-h-background.png"));
            splitterVerticalBackground = ImageIO.read(new File("res/splitter-v-background.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public PaneSplitter(Orientation orientation, JComponent firstComponent, JComponent secondComponent) {
        super(orientation, firstComponent, secondComponent);
        setPosition(0.5f);
        setDividerSize(7);
    }

    @Override
    protected Divider createDivider() {
        return new PaneSpliterDivider();
    }

    private class PaneSpliterDivider extends Divider {
        @Override
        public void paint(Graphics g) {
            Rectangle r = g.getClipBounds();
            if (getOrientation() == Orientation.HORIZONTAL) {
                g.drawImage(splitterHorizontalBackground, 0, 0, 7, getHeight(), null);
//                g.setColor(Theme.getInstance().getBorderHighlightColor());
//                g.fillRect(r.x, r.y, 1, r.height);
//                g.setColor(Theme.getInstance().getBorderColor());
//                g.fillRect(r.x + 1, r.y, 1, r.height);

            } else {
                g.drawImage(splitterVerticalBackground, 0, 0, getWidth(), 7, null);
//                g.setColor(Theme.getInstance().getBorderHighlightColor());
//                g.fillRect(r.x, r.y, r.width, 1);
//                g.setColor(Theme.getInstance().getBorderColor());
//                g.fillRect(r.x, r.y + 1, r.width, 1);
            }
        }
    }
}
