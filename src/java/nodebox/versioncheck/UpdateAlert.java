package nodebox.versioncheck;

import nodebox.ui.Platform;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Visual alert that new updates are available.
 */
public class UpdateAlert extends JFrame {

    public static final String RELEASE_NOTES_HEADER = "<html><head><style type=\"text/css\">" + "" +
            "body { font-family: sans-serif; font-size: 10px; } " +
            "ul { list-style-type:none; margin-left: 10px; padding: 0 }" +
            "li { margin-bottom: 5px; }" +
            "</style></head><body>";
    public static final String RELEASE_NOTES_FOOTER = "</body></html>";

    private Updater updater;
    private Appcast appcast;
    private AppcastItem item;
    private JEditorPane notesArea;

    public UpdateAlert(Updater updater, Appcast appcast) throws HeadlessException {
        this.updater = updater;
        this.appcast = appcast;
        this.item = appcast.getLatest();
        Host host = updater.getHost();
        setSize(586, 370);
        setMinimumSize(new Dimension(586, 370));
        JPanel content = new JPanel(new BorderLayout(10, 0));
        content.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        setContentPane(content);

        // Left side: application icon.
        JPanel iconPanel = new JPanel();
        iconPanel.setLayout(new BoxLayout(iconPanel, BoxLayout.Y_AXIS));

        Icon hostIcon = updater.getHostIcon();
        JLabel iconLabel = new JLabel(hostIcon);
        forceSize(iconLabel, 64, 64);
        iconPanel.add(iconLabel);
        iconPanel.add(Box.createVerticalGlue());
        content.add(iconPanel, BorderLayout.WEST);

        // Right side: box with all controls
        JPanel controlsPanel = new JPanel();
        controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.Y_AXIS));
        controlsPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
        content.add(controlsPanel, BorderLayout.CENTER);

        // First row: alert label
        String banner = "A new version of " + host.getName() + " is available!";
        String desc = host.getName() + " " + item.getVersion() + " is now available - you have " + host.getVersion() + ". Would you like to download it now?";

        JLabel alert = new JLabel("<html><b>" + banner + "</b><br><br>" + desc + "<br><b>Release notes:</b></html>");
        alert.setAlignmentX(Component.LEFT_ALIGNMENT);
        alert.setPreferredSize(new Dimension(Short.MAX_VALUE, 80));
        alert.setMaximumSize(new Dimension(Short.MAX_VALUE, 80));
        alert.setVerticalTextPosition(SwingConstants.TOP);
        controlsPanel.add(alert);
        controlsPanel.add(Box.createVerticalStrut(5));

        // Second line: text area.
        notesArea = new JEditorPane("text/html", RELEASE_NOTES_HEADER + item.getDescription() + RELEASE_NOTES_FOOTER);
        notesArea.setEditable(false);
        notesArea.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
        notesArea.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        JScrollPane notesScroll = new JScrollPane(notesArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        notesScroll.setBorder(BorderFactory.createLineBorder(new Color(137, 137, 137)));
        notesScroll.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
        notesScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        controlsPanel.add(notesScroll);
        controlsPanel.add(Box.createVerticalStrut(10));

        // Third line: buttons.
        JPanel buttonRow = new JPanel();
        buttonRow.setLayout(new BoxLayout(buttonRow, BoxLayout.X_AXIS));
        buttonRow.setMaximumSize(new Dimension(Short.MAX_VALUE, 32));
        buttonRow.add(Box.createHorizontalGlue());
        JButton remindMeLater = new JButton("Remind Me Later");
        remindMeLater.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                remindMeLaterClicked();
            }
        });

        JButton downloadButton = new JButton("Download");
        downloadButton.setDefaultCapable(true);
        downloadButton.requestFocus();
        downloadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                downloadClicked();
            }
        });
        buttonRow.add(remindMeLater);
        buttonRow.add(downloadButton);
        buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        controlsPanel.add(buttonRow);
    }

    private void remindMeLaterClicked() {
        setVisible(false);
        dispose();
    }

    private void downloadClicked() {
        setVisible(false);
        dispose();
        // Open download link URL.
        Platform.openURL(appcast.getDownloadLink());
    }

    private void forceSize(Component c, int width, int height) {
        c.setSize(width, height);
        Dimension d = new Dimension(width, height);
        c.setMinimumSize(d);
        c.setMaximumSize(d);
        c.setPreferredSize(d);
    }
}
