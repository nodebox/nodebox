package nodebox.ui;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class Theme {

    // Default colors
    public static final Color DEFAULT_ARROW_COLOR = new Color(136, 136, 136);
    public static final Color DEFAULT_SHADOW_COLOR = new Color(176, 176, 176);
    public static final Color DEFAULT_SPLIT_COLOR = new Color(139, 139, 139);

    // Viewer
    public static final Color VIEWER_BACKGROUND_COLOR = new Color(232, 232, 232);
    public static final Color SELECTED_TAB_BACKGROUND_COLOR = new Color(198, 198, 198);
    public static final Color TAB_BACKGROUND_COLOR = new Color(210, 210, 210);

    // Network view
    public static final Color NETWORK_BACKGROUND_COLOR = new Color(69, 69, 69);
    public static final Color NETWORK_GRID_COLOR = new Color(85, 85, 85);
    public static final Color NETWORK_SELECTION_COLOR = new Color(200, 200, 200, 100);
    public static final Color NETWORK_SELECTION_BORDER_COLOR = new Color(100, 100, 100, 100);
    public static final Color NETWORK_NODE_NAME_COLOR = new Color(194, 194, 194);
    public static final Color NETWORK_NODE_NAME_SHADOW_COLOR = new Color(23, 23, 23);
    public static final Color CONNECTION_DEFAULT_COLOR = new Color(200, 200, 200);
    public static final Color CONNECTION_CONNECTING_COLOR = new Color(170, 167, 18);
    public static final Color CONNECTION_ACTION_COLOR = new Color(0, 116, 168);

    // Port view
    public static final Color PORT_EXPRESSION_BACKGROUND_COLOR = new Color(255, 255, 240);
    public static final Color PORT_LABEL_BACKGROUND = new Color(153, 153, 153);
    public static final Color PORT_VALUE_BACKGROUND = new Color(196, 196, 196);
    public static final Color DRAGGABLE_NUMBER_HIGHLIGHT_COLOR = new Color(223, 223, 223);

    // Source editor
    public static final Color MESSAGES_BACKGROUND_COLOR = new Color(240, 240, 240);
    public static final Color EDITOR_SPLITTER_DIVIDER_COLOR = new Color(210, 210, 210);
    public static final Color EDITOR_DISABLED_BACKGROUND_COLOR = new Color(240, 240, 240);

    // Node attributes editor
    public static final Color NODE_ATTRIBUTES_PARAMETER_LIST_BACKGROUND_COLOR = new Color(240, 240, 250);
    public static final Color NODE_ATTRIBUTES_PARAMETER_COLOR = new Color(60, 60, 60);

    // Node selection dialog
    public static final Color NODE_SELECTION_BACKGROUND_COLOR = new Color(244, 244, 244);
    public static final Color NODE_SELECTION_ACTIVE_BACKGROUND_COLOR = new Color(224, 224, 224);

    // Text
    public static final Color TEXT_NORMAL_COLOR = new Color(60, 60, 60);
    public static final Color TEXT_ARMED_COLOR = new Color(0, 0, 0);
    public static final Color TEXT_SHADOW_COLOR = new Color(255, 255, 255);
    public static final Color TEXT_DISABLED_COLOR = new Color(98, 112, 130);
    public static final Color TEXT_HEADER_COLOR = new Color(93, 93, 93);
    public static final Color TEXT_WARNING_COLOR = new Color(200, 0, 0);

    // Borders
    public static final Color BORDER_COLOR;
    public static final Border LINE_BORDER;
    public static final Border TOP_BOTTOM_BORDER;
    public static final Border TOP_BORDER;
    public static final Border BOTTOM_BORDER;
    public static final Border PARAMETER_ROW_BORDER;
    public static final Border PARAMETER_NOTES_BORDER;
    public static final Border INNER_SHADOW_BORDER;
    public static final Border EMPTY_BORDER;

    // Fonts
    public static final Font EDITOR_FONT;
    public static final Font MESSAGE_FONT;
    public static final Font NETWORK_FONT;
    public static final Font INFO_FONT;
    public static final Font SMALL_FONT;
    public static final Font SMALL_BOLD_FONT;
    public static final Font SMALL_MONO_FONT;

    public static final int LABEL_WIDTH = 114;

    static {
        // Initialize borders.
        if (Platform.onWindows()) {
            BORDER_COLOR = new Color(100, 100, 100);
        } else if (Platform.onMac()) {
            BORDER_COLOR = new Color(200, 200, 200);
        } else {
            BORDER_COLOR = new Color(200, 200, 200);
        }
        LINE_BORDER = BorderFactory.createLineBorder(BORDER_COLOR);
        Color topColor = new Color(224, 224, 224);
        Color bottomColor = new Color(245, 245, 245);
        TOP_BOTTOM_BORDER = new TopBottomBorder(topColor, bottomColor);
        TOP_BORDER = new TopBorder(new Color(168, 168, 168));
        Color whiteColor = new Color(255, 255, 255);
        BOTTOM_BORDER = new BottomBorder(whiteColor);
        PARAMETER_ROW_BORDER = new RowBorder();
        PARAMETER_NOTES_BORDER = new NotesBorder();
        INNER_SHADOW_BORDER = new InnerShadowBorder();
        EMPTY_BORDER = BorderFactory.createEmptyBorder(0, 0, 0, 0);

        // Initialize fonts.
        if (Platform.onMac()) {
            EDITOR_FONT = new Font("Monaco", Font.PLAIN, 11);
            MESSAGE_FONT = new Font("Lucida Grande", Font.BOLD, 13);
            NETWORK_FONT = new Font("Lucida Grande", Font.PLAIN, 12);
            INFO_FONT = new Font("Lucida Grande", Font.PLAIN, 11);
            SMALL_FONT = new Font("Lucida Grande", Font.PLAIN, 11);
            SMALL_BOLD_FONT = new Font("Lucida Grande", Font.BOLD, 11);
            SMALL_MONO_FONT = new Font("Monaco", Font.PLAIN, 10);
        } else {
            EDITOR_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 11);
            MESSAGE_FONT = new Font("Verdana", Font.BOLD, 11);
            NETWORK_FONT = new Font("Verdana", Font.PLAIN, 11);
            INFO_FONT = new Font("Verdana", Font.PLAIN, 10);
            SMALL_FONT = new Font("Verdana", Font.PLAIN, 10);
            SMALL_BOLD_FONT = new Font("Verdana", Font.BOLD, 10);
            SMALL_MONO_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 10);
        }
    }

    public static class ArrowIcon implements Icon {
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(Theme.DEFAULT_ARROW_COLOR);
            g.drawLine(x + 1, y, x + 1, y);
            g.drawLine(x + 1, y + 1, x + 2, y + 1);
            g.drawLine(x + 1, y + 2, x + 3, y + 2);
            g.drawLine(x + 1, y + 3, x + 4, y + 3);
            g.drawLine(x + 1, y + 4, x + 3, y + 4);
            g.drawLine(x + 1, y + 5, x + 2, y + 5);
            g.drawLine(x + 1, y + 6, x + 1, y + 6);
        }

        public int getIconWidth() {
            return 6;
        }

        public int getIconHeight() {
            return 8;
        }
    }

    public static class TopBottomBorder implements Border {
        private Color topColor;
        private Color bottomColor;

        public TopBottomBorder(Color topColor, Color bottomColor) {
            this.topColor = topColor;
            this.bottomColor = bottomColor;
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            g.setColor(topColor);
            g.drawLine(x, y, x + width, y);
            g.setColor(bottomColor);
            g.drawLine(x, y + height - 1, x + width, y + height - 1);
        }

        public Insets getBorderInsets(Component c) {
            return new Insets(1, 0, 1, 0);
        }

        public boolean isBorderOpaque() {
            return true;
        }
    }

    public static class TopBorder implements Border {
        private Color topColor;

        public TopBorder(Color topColor) {
            this.topColor = topColor;
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            g.setColor(topColor);
            g.drawLine(x, y, x + width, y);
        }

        public Insets getBorderInsets(Component c) {
            return new Insets(1, 0, 0, 0);
        }

        public boolean isBorderOpaque() {
            return true;
        }
    }

    public static class BottomBorder implements Border {
        private Color bottomColor;

        public BottomBorder(Color bottomColor) {
            this.bottomColor = bottomColor;
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            g.setColor(bottomColor);
            g.drawLine(x, y + height - 1, x + width, y + height - 1);
        }

        public Insets getBorderInsets(Component c) {
            return new Insets(0, 0, 1, 0);
        }

        public boolean isBorderOpaque() {
            return true;
        }
    }

    public static class RowBorder implements Border {

        private static final Color LABEL_UP_COLOR = new Color(140, 140, 140);
        private static final Color LABEL_DOWN_COLOR = new Color(166, 166, 166);
        private static final Color PARAMETER_UP_COLOR = new Color(179, 179, 179);
        private static final Color PARAMETER_DOWN_COLOR = new Color(213, 213, 213);

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            // Draw border on the side of the label
            g.setColor(LABEL_UP_COLOR);
            g.fillRect(x, y + height - 2, LABEL_WIDTH - 2, 1);
            g.setColor(LABEL_DOWN_COLOR);
            g.fillRect(x, y + height - 1, LABEL_WIDTH - 2, 1);
            // Draw border on port side
            g.setColor(PARAMETER_UP_COLOR);
            g.fillRect(x + LABEL_WIDTH + 1, y + height - 2, width - LABEL_WIDTH - 1, 1);
            g.setColor(PARAMETER_DOWN_COLOR);
            g.fillRect(x + LABEL_WIDTH + 1, y + height - 1, width - LABEL_WIDTH - 1, 1);
        }

        public Insets getBorderInsets(Component c) {
            return new Insets(4, 0, 4, 0);
        }

        public boolean isBorderOpaque() {
            return true;
        }
    }

    public static class NotesBorder implements Border {

        private static final Color LABEL_UP_COLOR = new Color(140, 140, 140);
        private static final Color LABEL_DOWN_COLOR = new Color(166, 166, 166);
        private static final Color PARAMETER_UP_COLOR = new Color(150, 154, 43);
        private static final Color PARAMETER_DOWN_COLOR = new Color(213, 213, 213);

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            // Draw border on the side of the label
            g.setColor(LABEL_UP_COLOR);
            g.fillRect(x, y + height - 2, LABEL_WIDTH - 2, 1);
            g.setColor(LABEL_DOWN_COLOR);
            g.fillRect(x, y + height - 1, LABEL_WIDTH - 2, 1);
            // Draw border on port side
            g.setColor(PARAMETER_UP_COLOR);
            g.fillRect(x + LABEL_WIDTH, y + height - 2, width - LABEL_WIDTH, 1);
            g.setColor(PARAMETER_DOWN_COLOR);
            g.fillRect(x + LABEL_WIDTH + 1, y + height - 1, width - LABEL_WIDTH - 1, 1);
        }

        public Insets getBorderInsets(Component c) {
            return new Insets(4, 0, 4, 0);
        }

        public boolean isBorderOpaque() {
            return true;
        }
    }

    public static class InsetsBorder implements Border {
        private Insets insets;

        public InsetsBorder(Insets insets) {
            this.insets = insets;
        }

        public InsetsBorder(int x, int y, int width, int height) {
            this.insets = new Insets(x, y, width, height);
        }

        public void paintBorder(Component component, Graphics graphics, int i, int i1, int i2, int i3) {
        }

        public Insets getBorderInsets(Component component) {
            return insets;
        }

        public boolean isBorderOpaque() {
            return true;
        }
    }

    public static class InnerShadowBorder implements Border {

        private static final Color EDGE_COLOR = new Color(166, 166, 166);
        private static final Color HIGHLIGHT_COLOR = new Color(237, 237, 237);
        private static final Color SHADOW_COLOR = new Color(119, 119, 119);

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            g.setColor(EDGE_COLOR);
            g.drawRect(x + 1, y + 1, width - 3, height - 3);
            g.setColor(SHADOW_COLOR);
            g.drawLine(x, y, x + width - 1, y);
            g.drawLine(x, y, x, y + height - 1);
            g.setColor(HIGHLIGHT_COLOR);
            g.drawLine(x, y + height - 1, x + width - 1, y + height - 1);
            g.drawLine(x + width - 1, y, x + width - 1, y + height - 1);
        }

        public Insets getBorderInsets(Component c) {
            return new Insets(2, 2, 2, 2);
        }

        public boolean isBorderOpaque() {
            return true;
        }
    }

}
