package nodebox.client;

import javax.swing.*;
import java.awt.*;

public class DevicesDialog extends JDialog {
    private JPanel controlPanel;

    private NodeBoxDocument document;

    public DevicesDialog(NodeBoxDocument document) {
        super(document, "Devices", true);
        this.document = document;

        getRootPane().putClientProperty("Window.style", "small");
        setLayout(new BorderLayout());

        controlPanel = new JPanel(new GridBagLayout());

        JScrollPane scrollPane = new JScrollPane(controlPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, BorderLayout.CENTER);

        setSize(500, 400);
        setLocationRelativeTo(document);
        rebuildInterface();
    }

    private void rebuildInterface() {
        controlPanel.removeAll();
        int rowIndex = 0;

        for (DeviceHandler deviceHandler : document.getDeviceHandlers()) {
            JComponent control = deviceHandler.createControl();
            GridBagConstraints rowConstraints = new GridBagConstraints();
            rowConstraints.gridx = 0;
            rowConstraints.gridy = rowIndex;
            rowConstraints.fill = GridBagConstraints.HORIZONTAL;
            rowConstraints.weightx = 1.0;

            DeviceRow deviceRow = new DeviceRow(control);
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
    }
}