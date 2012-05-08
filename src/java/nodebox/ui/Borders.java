package nodebox.ui;

import javax.swing.border.Border;
import java.awt.*;

public class Borders {
    
    private Borders() {}

    public static Border topBottom(int topSize, Color topColor, int bottomSize, Color bottomColor) {
        return new CssBorder(topSize, topColor, 0, null, bottomSize, bottomColor, 0, null);
    }

    public static Border bottom(int size, Color color) {
        return new CssBorder(0, null, 0, null, size, color, 0, null);
    }
    
    private static class CssBorder implements Border {
        
        private final int topSize, leftSize, bottomSize, rightSize;
        private final Color topColor, leftColor, bottomColor, rightColor;

        private CssBorder(int topSize, Color topColor, int leftSize, Color leftColor, int bottomSize, Color bottomColor, int rightSize, Color rightColor) {
            this.topSize = topSize;
            this.topColor = topColor;
            this.leftSize = leftSize;
            this.leftColor = leftColor;
            this.bottomSize = bottomSize;
            this.bottomColor = bottomColor;
            this.rightSize = rightSize;
            this.rightColor = rightColor;
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            if (topSize > 0) {
                g.setColor(topColor);
                g.fillRect(x, y, width, topSize);
            }
            if (leftSize > 0) {
                g.setColor(leftColor);
                g.fillRect(x+width-leftSize, y, leftSize, height);
            }
            if (bottomSize > 0) {
                g.setColor(bottomColor);
                g.fillRect(x, y+height-bottomSize, width, bottomSize);
            }
            if (rightSize > 0) {
                g.setColor(rightColor);
                g.fillRect(x, y, rightSize, height);
            }
        }

        public Insets getBorderInsets(Component c) {
            return new Insets(topSize, leftSize, bottomSize, rightSize);
        }

        public boolean isBorderOpaque() {
            return true;
        }
    }
    
    
    
}
