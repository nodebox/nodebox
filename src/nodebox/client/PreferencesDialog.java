package nodebox.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class PreferencesDialog extends JDialog {

    private Preferences preferences;

    public PreferencesDialog() {
        super((Frame) null, "Preferences");
        setLocationRelativeTo(null);
        JPanel rootPanel = new JPanel();
        rootPanel.setLayout(new BoxLayout(rootPanel, BoxLayout.Y_AXIS));
        rootPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel label = new JLabel("<html><i>No preferences yet.</i></html>");
        rootPanel.add(label);

        rootPanel.add(Box.createVerticalStrut(10));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 10, 10));
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                doCancel();
            }
        });
        buttonPanel.add(cancelButton);
        rootPanel.add(buttonPanel);
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                doSave();
            }
        });
        buttonPanel.add(saveButton);
        getRootPane().setDefaultButton(saveButton);

        readPreferences();

        setContentPane(rootPanel);
        setResizable(false);
        pack();
    }

    private void readPreferences() {
        this.preferences = Preferences.userNodeForPackage(Application.class);
    }

    public void doCancel() {
        dispose();
    }

    public void doSave() {
        // TODO Re-enable this when there are actual preferences.
        // JOptionPane.showMessageDialog(this, "Please restart NodeBox for the changes to take effect.");
        try {
            preferences.flush();
        } catch (BackingStoreException e) {
            throw new RuntimeException(e);
        }
        dispose();
    }

}
