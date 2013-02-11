package nodebox.client;

import nodebox.ui.AddressBar;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

/**
 * The ProgressPanel shows the render state of the network.
 */
public class ProgressPanel extends JComponent {

    public static final Image backgroundImage;
    public static final Icon stopOnIcon, stopOffIcon;
    private static final int PROGRESS_PANEL_HEIGHT = AddressBar.ADDRESS_BAR_HEIGHT;
    private static final int PROGRESS_PANEL_WIDTH = 45;

    static {
        try {
            backgroundImage = ImageIO.read(ProgressPanel.class.getResourceAsStream("/progress-background.png"));
            stopOnIcon = new ImageIcon(ProgressPanel.class.getResource("/progress-stop-on.png"));
            stopOffIcon = new ImageIcon(ProgressPanel.class.getResource("/progress-stop-off.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final JButton stopButton;

    public ProgressPanel(final NodeBoxDocument document) {
        setMinimumSize(new Dimension(0, PROGRESS_PANEL_HEIGHT));
        setMaximumSize(new Dimension(PROGRESS_PANEL_WIDTH, PROGRESS_PANEL_HEIGHT));
        setPreferredSize(new Dimension(PROGRESS_PANEL_WIDTH, PROGRESS_PANEL_HEIGHT));
        setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));

        stopButton = new JButton(stopOffIcon);
        stopButton.setBorderPainted(false);
        stopButton.setBorder(null);
        stopButton.setSize(19, 19);
        stopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                document.stopRendering();
            }
        });
        add(stopButton);
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.drawImage(backgroundImage, 0, 0, getWidth(), PROGRESS_PANEL_HEIGHT, null);
    }

    public void setInProgress(boolean visible) {
        if (visible) {
            stopButton.setIcon(stopOnIcon);
        } else {
            stopButton.setIcon(stopOffIcon);
        }
    }

}
