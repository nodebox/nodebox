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
        JPanel rootPanel = new JPanel(new BorderLayout(10, 10));
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel experimental = new JLabel("Experimental Features");
        experimental.setFont(new Font(Font.DIALOG, Font.BOLD, 13));
        experimental.setMinimumSize(new Dimension(300, 20));
        experimental.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        contentPanel.add(experimental);

        enableDeviceSupportCheck = new JCheckBox("Device Support");
        enableDeviceSupportCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(enableDeviceSupportCheck);

        rootPanel.add(contentPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 10, 10));
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        buttonPanel.add(cancelButton);
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(this);
        buttonPanel.add(saveButton);
        rootPanel.add(buttonPanel, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(saveButton);

        readPreferences();

        setContentPane(rootPanel);
        setMinimumSize(new Dimension(300, 100));
        setResizable(false);
        pack();
    }

    private boolean isDeviceSupportEnabled() {
        return Boolean.valueOf(preferences.get(Application.PREFERENCE_ENABLE_DEVICE_SUPPORT, "false"));
    }

    private void setEnableDeviceSupport(boolean enabled) {
        application.ENABLE_DEVICE_SUPPORT = enabled;
        preferences.put(Application.PREFERENCE_ENABLE_DEVICE_SUPPORT, Boolean.toString(enabled));
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
