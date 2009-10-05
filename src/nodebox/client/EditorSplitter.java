package nodebox.client;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class EditorSplitter extends JSplitPane {

    private static Image editorSplitterBackground, editorSplitterHandle;
    private static int editorSplitterHandleWidth;
    private static final int DIVIDER_SIZE = 16;

    static {
        try {
            // The height of this image should be equal to DIVIDER_SIZE.
            editorSplitterBackground = ImageIO.read(new File("res/editor-splitter-background.png"));
            editorSplitterHandle = ImageIO.read(new File("res/editor-splitter-handle.png"));
            editorSplitterHandleWidth = editorSplitterHandle.getWidth(null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String positionString;

    public EditorSplitter(int orientation, Component newLeftComponent, Component newRightComponent) {
        super(orientation, newLeftComponent, newRightComponent);
        if (orientation != JSplitPane.VERTICAL_SPLIT)
            throw new UnsupportedOperationException("Only vertical split is supported.");
        setContinuousLayout(true);
        setDividerLocation(0.5);
        setResizeWeight(0.5);
        setDividerSize(DIVIDER_SIZE);
        setUI(new EditorSplitterUI());
        setBorder(BorderFactory.createEmptyBorder());
        positionString = "";
    }

    public void setLocation(int line, int column) {
        positionString = String.format(Locale.US, "Line: %4d  Column: %4d", line, column);
        repaint();
    }

    private class EditorSpliterDivider extends BasicSplitPaneDivider {
        private EditorSpliterDivider(BasicSplitPaneUI ui) {
            super(ui);
        }

        @Override
        public void paint(Graphics g) {
            Rectangle r = g.getClipBounds();
            if (getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
                throw new AssertionError("Horizontal split is not implemented.");

            } else {
                g.drawImage(editorSplitterBackground, r.x, r.y, r.width, DIVIDER_SIZE, null);
                if (EditorSplitter.this.isEnabled()) {
                    g.drawImage(editorSplitterHandle, r.x + r.width - 3 - editorSplitterHandleWidth, r.y + 1, null);
                }
                // If the divider is at the bottom, make sure the dark line of the divider doesn't get drawn.
                // We do this by overdrawing the bottom line with the same color as the middle of the divider.
                if (EditorSplitter.this.getHeight() - getDividerLocation() <= DIVIDER_SIZE) {
                    g.setColor(new Color(210, 210, 210));
                    g.fillRect(r.x, r.y + r.height - 1, r.width, 1);
                }
                g.setFont(SwingUtils.FONT_BOLD);
                g.setColor(SwingUtils.COLOR_NORMAL);
                SwingUtils.drawShadowText((Graphics2D) g, positionString, r.x + 5, r.y + 13);
            }
        }
    }

    private class EditorSplitterUI extends BasicSplitPaneUI {
        @Override
        public BasicSplitPaneDivider createDefaultDivider() {
            return new EditorSpliterDivider(this);
        }
    }

}
