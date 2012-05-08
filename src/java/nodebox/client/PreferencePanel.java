package nodebox.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.prefs.Preferences;

public class PreferencePanel extends JDialog {

    private final Application application;
    private final Preferences preferences;


    public PreferencePanel(Application application, Window owner) {
        super(owner, "Preferences");
        this.application = application;
        preferences = Preferences.userNodeForPackage(Application.class);
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JPanel row = new JPanel();
        row.setAlignmentX(Component.RIGHT_ALIGNMENT);
        panel.add(row);
        JButton closeButton = new JButton("Close");
        closeButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        panel.add(Box.createVerticalGlue());
        panel.add(closeButton);
        setContentPane(panel);
        setSize(400, 200);
    }
}
