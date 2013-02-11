package nodebox.ui;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkArgument;

public class AddressBar extends JPanel implements MouseListener {

    public static final Image addressGradient;
    public static final Image addressArrow;

    private static ImmutableList<String> ROOT_LIST = ImmutableList.of("root");
    public static final int ADDRESS_BAR_HEIGHT = 25;

    static {
        try {
            addressGradient = ImageIO.read(AddressBar.class.getResourceAsStream("/address-gradient.png"));
            addressArrow = ImageIO.read(AddressBar.class.getResourceAsStream("/address-arrow.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ImmutableList<String> segments = ImmutableList.of();
    private int[] positions;
    private int armed = -1;
    private OnSegmentClickListener onSegmentClickListener;
    private String message = "";

    public AddressBar() {
        addMouseListener(this);
        setMinimumSize(new Dimension(0, ADDRESS_BAR_HEIGHT));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, ADDRESS_BAR_HEIGHT));
        setPreferredSize(new Dimension(Integer.MAX_VALUE, ADDRESS_BAR_HEIGHT));
        setLayout(null);
    }

    public ImmutableList<String> getSegments() {
        return segments;
    }

    public void setSegments(Iterable<String> segments) {
        this.segments = ImmutableList.copyOf(segments);
        repaint();
    }

    public void setPath(String path) {
        checkArgument(path.startsWith("/"), "Only absolute paths are supported.");
        if (path.length() == 1) {
            setSegments(ROOT_LIST);
        } else {
            setSegments(Iterables.concat(ROOT_LIST, Splitter.on("/").split(path.substring(1))));
        }
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
        repaint();
    }

    /**
     * Returns the segment-click callback registered for this address bar.
     *
     * @return The callback, or null if one is not registered.
     */
    public OnSegmentClickListener getOnSegmentClickListener() {
        return onSegmentClickListener;
    }

    /**
     * Register a callback to be invoked when a segment was clicked in the address bar.
     *
     * @param l the callback that will run.
     */
    public void setOnSegmentClickListener(OnSegmentClickListener l) {
        onSegmentClickListener = l;
    }

    @Override
    protected void paintComponent(Graphics g) {
        positions = new int[segments.size()];
        Graphics2D g2 = (Graphics2D) g;

        g2.setFont(Theme.SMALL_BOLD_FONT);

        g2.drawImage(addressGradient, 0, 0, getWidth(), 25, null);

        int x = 10;

        int i = 0;
        for (String segment : segments) {
            if (i == armed) {
                g2.setColor(Theme.TEXT_ARMED_COLOR);
            } else {
                g2.setColor(Theme.TEXT_NORMAL_COLOR);
            }
            SwingUtils.drawShadowText(g2, segment, x, 16);

            int width = g2.getFontMetrics().stringWidth(segment);
            x += width + 5;
            positions[i] = x + 10;
            g2.drawImage(addressArrow, x, 1, null);
            x += 15;
            i++;
        }
        if (! message.isEmpty()) {
            g2.setColor(Theme.TEXT_NORMAL_COLOR);
            int w = g2.getFontMetrics().stringWidth(message);
            g2.drawString(message, getWidth() - w - 10, 15);
        }
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        int mx = e.getX();
        armed = partIndex(mx);
        repaint();
    }

    public void mouseReleased(MouseEvent e) {
        armed = -1;
        int mx = e.getX();
        int partIndex = partIndex(mx);
        if (partIndex == -1) return;
        String selectedPart = segments.get(partIndex);
        if (selectedPart != null && onSegmentClickListener != null)
            onSegmentClickListener.onSegmentClicked(pathForIndex(partIndex));
        repaint();
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
        armed = -1;
        repaint();
    }


    private int partIndex(int x) {
        if (positions == null) return -1;
        for (int i = 0; i < positions.length; i++) {
            if (x < positions[i])
                return i;
        }
        return -1;
    }

    private String pathForIndex(int endIndex) {
        return "/" + Joiner.on("/").join(segments.subList(1, endIndex + 1));
    }

    /**
     * Callback listener to be invoked when an address segment has been clicked.
     */
    public static interface OnSegmentClickListener {

        /**
         * Called when a part has been clicked.
         *
         * @param fullPath The full path of the part that was clicked.
         */
        public void onSegmentClicked(String fullPath);

    }

}
