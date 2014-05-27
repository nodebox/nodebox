package nodebox.client;

import nodebox.client.devicehandler.DeviceControl;
import nodebox.client.devicehandler.DeviceHandler;
import nodebox.ui.ActionHeader;
import nodebox.ui.InsetLabel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class DevicesDialog extends JDialog implements DeviceControl.OnPropertyChangeListener {
    private static final ImageIcon plusIcon = new ImageIcon(CodeLibrariesDialog.class.getResource("/action-plus-arrow.png"));

    private JPanel controlPanel;

    private NodeBoxDocument document;

    public DevicesDialog(NodeBoxDocument document) {
        super(document, "Devices", false);
        this.document = document;

        setLayout(new BorderLayout());

        ActionHeader actionHeader = new ActionHeader();
        actionHeader.setLayout(new BoxLayout(actionHeader, BoxLayout.LINE_AXIS));
        InsetLabel actionHeaderLabel = new InsetLabel("Devices");
        actionHeader.add(Box.createHorizontalStrut(10));
        actionHeader.add(actionHeaderLabel);
        actionHeader.add(Box.createHorizontalGlue());
        final DevicesPopupMenu devicesPopup = new DevicesPopupMenu();
        final JButton addDeviceButton = new JButton(plusIcon);
        addDeviceButton.setBorder(null);
        addDeviceButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                devicesPopup.show(addDeviceButton, -20, 21);
            }
        });
        actionHeader.add(addDeviceButton);
        actionHeader.add(Box.createHorizontalStrut(10));
        add(actionHeader, BorderLayout.NORTH);

        controlPanel = new JPanel(new GridBagLayout());

        JScrollPane scrollPane = new JScrollPane(controlPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, BorderLayout.CENTER);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                DevicesDialog.this.setVisible(false);
            }
        });
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 5, 5));
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);

        setSize(560, 400);
        setLocationRelativeTo(document);
        rebuildInterface();
    }

    public void rebuildInterface() {
        controlPanel.removeAll();
        int rowIndex = 0;

        for (final DeviceHandler deviceHandler : document.getDeviceHandlers()) {
            JComponent control = deviceHandler.createControl();
            ((DeviceControl) control).setPropertyChangeListener(this);

            GridBagConstraints rowConstraints = new GridBagConstraints();
            rowConstraints.gridx = 0;
            rowConstraints.gridy = rowIndex;
            rowConstraints.fill = GridBagConstraints.HORIZONTAL;
            rowConstraints.weightx = 1.0;

            ActionListener removeDeviceListener = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    document.removeDevice(deviceHandler.getName());
                    rebuildInterface();
                }
            };

            DeviceRow deviceRow = new DeviceRow(control, removeDeviceListener);
            controlPanel.add(deviceRow, rowConstraints);

            rowIndex++;
        }
        JLabel filler = new JLabel();
        GridBagConstraints fillerConstraints = new GridBagConstraints();
        fillerConstraints.gridx = 0;
        fillerConstraints.gridy = rowIndex;
        fillerConstraints.fill = GridBagConstraints.BOTH;
        fillerConstraints.weighty = 1.0;
        fillerConstraints.gridwidth = GridBagConstraints.REMAINDER;
        controlPanel.add(filler, fillerConstraints);
        validate();
        repaint();
    }

    public void onPropertyChange(String deviceName, String key, String newValue) {
        document.setDeviceProperty(deviceName, key, newValue);
    }

    private class DevicesPopupMenu extends JPopupMenu {
        private DevicesPopupMenu() {
            add(new AddDeviceHandlerAction("osc", "osc", "OSC"));
            add(new AddDeviceHandlerAction("audioplayer", "audio", "Audio Player"));
            add(new AddDeviceHandlerAction("audioinput", "audio", "Audio Input"));
        }
    }

    private class AddDeviceHandlerAction extends AbstractAction {
        private String type;
        private String name;

        private AddDeviceHandlerAction(String type, String name, String label) {
            super(label);
            this.type = type;
            this.name = name;
        }

        public void actionPerformed(ActionEvent actionEvent) {
            document.addDevice(type, name);
            rebuildInterface();
        }
    }
}