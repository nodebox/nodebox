package nodebox.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class PreferencePanel extends JDialog implements ActionListener {

    private final Application application;
    private final Preferences preferences;
    private JCheckBox enableDeviceSupportCheck;

    public PreferencePanel(Application application, Window owner) {
        super(owner, "Preferences");
        this.application = application;
        preferences = Preferences.userNodeForPackage(Application.class);
        JPanel rootPanel = new JPanel();
        rootPanel.setLayout(new BoxLayout(rootPanel, BoxLayout.Y_AXIS));
        rootPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        rootPanel.add(Box.createVerticalStrut(10));

        enableDeviceSupportCheck = new JCheckBox("Enable Device Support");
        rootPanel.add(enableDeviceSupportCheck);

        rootPanel.add(Box.createVerticalStrut(90));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 10, 10));
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        buttonPanel.add(cancelButton);
        rootPanel.add(buttonPanel);
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(this);
        buttonPanel.add(saveButton);
        getRootPane().setDefaultButton(saveButton);

        readPreferences();

        setContentPane(rootPanel);
        setResizable(false);
        pack();
    }

    private boolean isDeviceSupportEnabled() {
        return Boolean.valueOf(preferences.get(application.PREFERENCE_ENABLE_DEVICE_SUPPORT, "false"));
    }

    private void setEnableDeviceSupport(boolean enabled) {
        application.ENABLE_DEVICE_SUPPORT = enabled;
        preferences.put(application.PREFERENCE_ENABLE_DEVICE_SUPPORT, Boolean.toString(enabled));
    }

    private void readPreferences() {
       enableDeviceSupportCheck.setSelected(isDeviceSupportEnabled());
    }

    public void actionPerformed(ActionEvent actionEvent) {
        boolean changed = false;

        if (isDeviceSupportEnabled() != enableDeviceSupportCheck.isSelected()) {
            setEnableDeviceSupport(enableDeviceSupportCheck.isSelected());
            changed = true;
        }
        if (changed) {
            JOptionPane.showMessageDialog(this, "Please restart NodeBox for the changes to take effect.");
            try {
                preferences.flush();
            } catch (BackingStoreException e) {
                throw new RuntimeException(e);
            }
        }
        dispose();
    }
}
