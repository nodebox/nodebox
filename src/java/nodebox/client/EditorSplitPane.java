package nodebox.client;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class EditorSplitPane extends CustomSplitPane {

    private static Image editorSplitterBackground, editorSplitterHandle;
    private static int editorSplitterHandleWidth;
    private static final int DIVIDER_SIZE = 15;

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

    private String positionString = "";
    private boolean showMessages = false;

    public EditorSplitPane(int orientation, Component c1, Component c2) {
        super(orientation, c1, c2);
        setDividerSize(DIVIDER_SIZE);
        c2.setVisible(false);
        setEnabled(false);
    }

    public void setLocation(int line, int column) {
        positionString = String.format(Locale.US, "Line: %4d  Column: %4d", line, column);
        repaint();
    }

    public boolean isShowMessages() {
        return showMessages;
    }

    public void setShowMessages(boolean showMessages) {
        if (showMessages == this.showMessages)
            return;
        this.showMessages = showMessages;
        if (showMessages) {
            setEnabled(true);
            setDividerLocation(0.6);
            setResizeWeight(0.6);
            getBottomComponent().setVisible(true);
        } else {
            setEnabled(false);
            setDividerLocation(1.0);
            setResizeWeight(1.0);
            getBottomComponent().setVisible(false);
        }
    }

    @Override
    protected BasicSplitPaneUI createUI() {
        return new EditorSplitPaneUI();
    }

    private class EditorSplitPaneUI extends BasicSplitPaneUI {
        public BasicSplitPaneDivider createDefaultDivider() {
            return new BasicSplitPaneDivider(EditorSplitPaneUI.this) {
                @Override
                public void paint(Graphics g) {

                    int width = getWidth();
                    //int height = getHeight();
                    //Rectangle r = g.getClipBounds();
                    if (getOrientation() == JSplitPane.HORIZONTAL_SPLIT)
                        throw new AssertionError("Horizontal split is not implemented.");
                    g.drawImage(editorSplitterBackground, 0, 0, width, DIVIDER_SIZE, null);
                    if (isShowMessages()) {
                        g.drawImage(editorSplitterHandle, width - 3 - editorSplitterHandleWidth, 1, null);
                    }
                    g.setFont(Theme.SMALL_BOLD_FONT);
                    g.setColor(Theme.TEXT_NORMAL_COLOR);
                    SwingUtils.drawShadowText((Graphics2D) g, positionString, 5, 13);
                }
            };
        }

    }


}
