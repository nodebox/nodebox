package nodebox.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Icons used in NodeBox.
 */
public class Icons {


    public static class MultiArrowIcon extends ArrowIcon {
        public MultiArrowIcon(int direction, Color color) {
            super(direction, color);
        }

        public void paintIcon(Component component, Graphics g, int x, int y) {
            g.setColor(color);
            g.translate(x, y);
            if (direction == NORTH) {
            } else if (direction == SOUTH) {
                g.drawLine(0, 1, 14, 1);
                g.drawLine(1, 2, 13, 2);
                g.drawLine(2, 3, 4, 3);
                g.drawLine(6, 3, 8, 3);
                g.drawLine(10, 3, 12, 3);
                g.drawLine(3, 4, 3, 4);
                g.drawLine(7, 4, 7, 4);
                g.drawLine(11, 4, 11, 4);
            }
            g.translate(-x, -y);
        }

        public int getIconWidth() {
            return 15;
        }

    }

    public static abstract class ColoredIcon implements Icon {
        protected Color color;

        protected ColoredIcon() {
            color = Color.black;
        }

        protected ColoredIcon(Color color) {
            this.color = color;
        }

        public int getIconWidth() {
            return 7;
        }

        public int getIconHeight() {
            return 7;
        }
    }

    public static class PlusIcon extends ColoredIcon {

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(color);
            g.translate(x, y);
            g.drawLine(0, 3, 5, 3);
            g.drawLine(0, 4, 5, 4);
            g.drawLine(2, 1, 2, 6);
            g.drawLine(3, 1, 3, 6);
        }
    }

    public static class MinusIcon extends ColoredIcon {

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(color);
            g.translate(x, y);
            g.drawLine(0, 3, 5, 3);
            g.drawLine(0, 4, 5, 4);
        }

    }

    public static class ArrowIcon implements Icon {
        public static final int NORTH = 0;
        public static final int EAST = 1;
        public static final int SOUTH = 2;
        public static final int WEST = 3;
        protected int direction;
        protected Color color;

        public ArrowIcon(int direction) {
            this(direction, Color.black);
        }

        public ArrowIcon(int direction, Color color) {
            this.direction = direction;
            this.color = color;
        }

        public void paintIcon(Component component, Graphics g, int x, int y) {
            g.setColor(color);
            g.translate(x, y);
            if (direction == NORTH) {
                g.drawLine(3, 1, 3, 1);
                g.drawLine(2, 2, 4, 2);
                g.drawLine(1, 3, 5, 3);
                g.drawLine(0, 4, 6, 4);
            } else if (direction == EAST) {
                g.drawLine(1, 0, 1, 6);
                g.drawLine(2, 1, 2, 5);
                g.drawLine(3, 2, 3, 4);
                g.drawLine(4, 3, 4, 3);
            } else if (direction == SOUTH) {
                g.drawLine(0, 1, 6, 1);
                g.drawLine(1, 2, 5, 2);
                g.drawLine(2, 3, 4, 3);
                g.drawLine(3, 4, 3, 4);
            } else if (direction == WEST) {
                g.drawLine(4, 0, 4, 6);
                g.drawLine(3, 1, 3, 5);
                g.drawLine(2, 2, 2, 4);
                g.drawLine(1, 3, 1, 3);
            }
            g.translate(-x, -y);

//
//            g.setColor(color);
//            g.translate(x,y);
//            g.drawLine(0,0,6,0);
//            g.drawLine(1,1, 5, 1);
//            g.drawLine(2,2, 4, 2);
//            g.drawLine(3,3, 3, 3);
//            g.translate(-x, -y);
        }

        public int getIconWidth() {
            return 7;
        }

        public int getIconHeight() {
            return 7;
        }
    }

}
