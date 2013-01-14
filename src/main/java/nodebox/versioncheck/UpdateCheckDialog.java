package nodebox.versioncheck;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * This dialog is displayed when the system is checking for updates.
 */
public class UpdateCheckDialog extends JDialog implements ActionListener {
    private Updater updater;

    public UpdateCheckDialog(Frame owner, Updater updater) {
        super(owner, "Checking for Updates...");
        this.updater = updater;
        Dimension d = new Dimension(384, 127);
        setSize(d);
        setResizable(false);
        JPanel contentPanel = new JPanel(new BorderLayout(10, 0));
        setContentPane(contentPanel);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        // Left side: application icon.
        JPanel iconPanel = new JPanel();
        iconPanel.setLayout(new BoxLayout(iconPanel, BoxLayout.Y_AXIS));

        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) img.getGraphics();
        Icon icon = new ImageIcon(updater.getHost().getIconFile());
        float factor = 64f / icon.getIconHeight();
        g.scale(factor, factor);
        icon.paintIcon(this, g, 0, 0);
        ImageIcon scaledIcon = new ImageIcon(img);
        JLabel iconLabel = new JLabel(scaledIcon);
        forceSize(iconLabel, 64, 64);
        iconPanel.add(iconLabel);
        iconPanel.add(Box.createVerticalGlue());
        contentPanel.add(iconPanel, BorderLayout.WEST);

        // Right side: box with all controls
        JPanel controlsPanel = new JPanel();
        controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.Y_AXIS));
        controlsPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
        contentPanel.add(controlsPanel, BorderLayout.CENTER);

        // First row: status text
        JLabel alert = new JLabel("<html><b>Checking for Updates...</b></html>");
        alert.setAlignmentX(Component.LEFT_ALIGNMENT);
        alert.setPreferredSize(new Dimension(Short.MAX_VALUE, 80));
        alert.setMaximumSize(new Dimension(Short.MAX_VALUE, 80));
        alert.setVerticalTextPosition(SwingConstants.TOP);
        controlsPanel.add(alert);
        controlsPanel.add(Box.createVerticalStrut(5));

        // Second line: progress bar
        JProgressBar progressBar = new JProgressBar(JProgressBar.HORIZONTAL);
        progressBar.setIndeterminate(true);
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        progressBar.setPreferredSize(new Dimension(Short.MAX_VALUE, 20));
        progressBar.setMaximumSize(new Dimension(Short.MAX_VALUE, 20));
        controlsPanel.add(progressBar);
        controlsPanel.add(Box.createVerticalStrut(5));

        // Last line: button bar
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(this);
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(cancelButton);
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonPanel.setPreferredSize(new Dimension(Short.MAX_VALUE, 20));
        buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 20));
        controlsPanel.add(buttonPanel);
    }

    private void forceSize(Component c, int width, int height) {
        c.setSize(width, height);
        Dimension d = new Dimension(width, height);
        c.setMinimumSize(d);
        c.setMaximumSize(d);
        c.setPreferredSize(d);
    }

    public static void main(String[] args) {
        Updater updater = new Updater(new MockHost());
        UpdateCheckDialog d = new UpdateCheckDialog(null, updater);
        d.setVisible(true);
    }

    public void actionPerformed(ActionEvent e) {
        updater.cancelUpdateCheck();
        setVisible(false);
    }

    private static class MockHost implements Host {
        public String getName() {
            return "MockBox";
        }

        public Version getVersion() {
            return new Version("1.0");
        }

        public URL getIconFile() {
            try {
                return new URL("file:src/test/files/mockboxlogo.png");
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        public String getAppcastURL() {
            return "http://www.example.com/appcast.xml";
        }
    }

}
