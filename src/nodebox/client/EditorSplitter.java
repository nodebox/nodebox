package nodebox.client;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class EditorSplitter extends NSplitter {

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

    public EditorSplitter(NSplitter.Orientation orientation, JComponent firstComponent, JComponent secondComponent) {
        super(orientation, firstComponent, secondComponent);
        if (orientation != Orientation.VERTICAL)
            throw new UnsupportedOperationException("Only vertical split is supported.");
        setDividerSize(DIVIDER_SIZE);
        setPosition(0.5f);
        positionString = "";
    }

    public void setLocation(int line, int column) {
        positionString = String.format(Locale.US, "Line: %4d  Column: %4d", line, column);
        repaint();
    }

    @Override
    protected Divider createDivider() {
        return new EditorSpliterDivider();
    }

    private class EditorSpliterDivider extends NSplitter.Divider {

        @Override
        public void paintComponent(Graphics g) {
            int width = getWidth();
            //int height = getHeight();
            //Rectangle r = g.getClipBounds();
            if (getOrientation() == Orientation.HORIZONTAL) {
                throw new AssertionError("Horizontal split is not implemented.");

            } else {
                g.drawImage(editorSplitterBackground, 0, 0, width, DIVIDER_SIZE, null);
                if (EditorSplitter.this.isEnabled()) {
                    g.drawImage(editorSplitterHandle, width - 3 - editorSplitterHandleWidth, 1, null);
                }
                // If the divider is at the bottom, make sure the dark line of the divider doesn't get drawn.
                // We do this by overdrawing the bottom line with the same color as the middle of the divider.
                if (EditorSplitter.this.getHeight() - getAbsolutePosition() <= DIVIDER_SIZE) {
                    g.setColor(Theme.EDITOR_SPLITTER_DIVIDER_COLOR);
                    g.fillRect(0, getDividerSize() - 1, width, 1);
                }
                g.setFont(Theme.SMALL_BOLD_FONT);
                g.setColor(Theme.TEXT_NORMAL_COLOR);
                SwingUtils.drawShadowText((Graphics2D) g, positionString, 5, 13);
            }
        }
    }

}
