package nodebox.client.devicehandler;

import nodebox.client.MinimInputApplet;
import nodebox.node.Device;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Map;

public class AudioInputDeviceHandler implements DeviceHandler {

    private String name;
    private boolean syncWithTimeline;
    private JFrame frame = null;

    private MinimInputApplet applet = null;

    public AudioInputDeviceHandler(String name) {
        this(name, false);
    }

    public AudioInputDeviceHandler(String name, boolean syncWithTimeline) {
        this.name = name;
        this.syncWithTimeline = syncWithTimeline;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isSyncedWithTimeline() {
        return syncWithTimeline;
    }

    @Override
    public void start() {
        if (frame != null) {
            stop();
        }
        frame = new JFrame();
        applet = new MinimInputApplet();
        applet.init();
        frame.add(applet);
    }

    @Override
    public void resume() {
        if (frame == null) {
            start();
        }
    }

    @Override
    public void pause() {
        // Do nothing.
    }

    @Override
    public void stop() {
        if (frame != null) {
            applet.stop();
            applet.dispose();
            frame.dispose();
            frame = null;
        }
    }

    @Override
    public void addData(Map<String, Object> map) {
        if (applet != null && applet.getInput() != null) {
            map.put(getName() + ".source", applet.getInput());
            map.put(getName() + ".beat", applet.getBeatDetect());
        }
    }

    public AbstractDeviceControl createControl() {
        return new AudioInputDeviceControl(this);
    }

    private class AudioInputDeviceControl extends AbstractDeviceControl {
        private JLabel deviceNameLabel;
        private JCheckBox syncWithTimelineCheck;
        private JButton startButton;
        private JButton stopButton;

        public AudioInputDeviceControl(AudioInputDeviceHandler deviceHandler) {
            super(deviceHandler);
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

            Dimension d = new Dimension(500, 60);
            setPreferredSize(d);
            setMaximumSize(d);
            setSize(d);

            deviceNameLabel = new JLabel(deviceHandler.getName());

            add(Box.createHorizontalStrut(10));
            add(deviceNameLabel);
            add(Box.createHorizontalStrut(5));

            syncWithTimelineCheck = new JCheckBox("Sync with Timeline");
            syncWithTimelineCheck.setSelected(isSyncedWithTimeline());
            syncWithTimelineCheck.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent itemEvent) {
                    syncWithTimeline = syncWithTimelineCheck.isSelected();
                    setPropertyValue(Device.TIMELINE_SYNC, String.valueOf(syncWithTimeline));
                }
            });

            startButton = new JButton("Start");
            startButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    start();
                    setButtons();
                }
            });
            stopButton = new JButton("Stop");
            stopButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    stop();
                    setButtons();
                }
            });

            setButtons();

            JPanel startStopPanel = new JPanel();
            startStopPanel.setLayout(new BoxLayout(startStopPanel, BoxLayout.X_AXIS));
            startStopPanel.add(syncWithTimelineCheck);
            startStopPanel.add(Box.createHorizontalStrut(5));
            startStopPanel.add(startButton);
            startStopPanel.add(Box.createHorizontalStrut(5));
            startStopPanel.add(stopButton);
            startStopPanel.add(Box.createHorizontalGlue());

            JPanel mainPanel = new JPanel();
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
            mainPanel.add(startStopPanel);

            add(mainPanel);
            add(Box.createHorizontalGlue());
        }

        private void setButtons() {
            startButton.setEnabled(frame == null);
            stopButton.setEnabled(frame != null);
        }

    }
}
