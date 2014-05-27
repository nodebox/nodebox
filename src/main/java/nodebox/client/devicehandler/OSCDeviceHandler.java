package nodebox.client.devicehandler;

import com.google.common.collect.ImmutableList;
import nodebox.node.Device;
import oscP5.OscEventListener;
import oscP5.OscMessage;
import oscP5.OscP5;
import oscP5.OscStatus;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OSCDeviceHandler implements DeviceHandler {

    private String name;
    private OscP5 oscP5;
    private int oscPort;
    private boolean syncWithTimeline;
    private Map<String, List<Object>> oscMessages = new HashMap<String, List<Object>>();
    private boolean paused;

    public OSCDeviceHandler(String name) {
        this(name, -1, false);
    }

    public OSCDeviceHandler(String name, int oscPort, boolean syncWithTimeline) {
        this.name = name;
        this.oscPort = oscPort;
        this.syncWithTimeline = syncWithTimeline;
        oscP5 = null;
        oscMessages.clear();
        paused = false;
    }

    @Override
    public String getName() {
        return name;
    }

    public int getPort() {
        return oscPort;
    }

    @Override
    public boolean isSyncedWithTimeline() {
        return syncWithTimeline;
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isRunning() {
        return oscP5 != null;
    }

    public Map<String, List<Object>> getOscMessages() {
        return oscMessages;
    }

    private static int randomOSCPort() {
        return 1024 + (int) Math.round(Math.random() * 10000);
    }

    @Override
    public void start() {
        if (oscP5 != null)
            stop();
        if (oscPort == -1) return;
        oscMessages.clear();
        oscP5 = new OscP5(new Object(), oscPort);
        oscP5.addListener(new OscEventListener() {
            @Override
            public void oscEvent(OscMessage m) {
                if (! isPaused()) {
                    ImmutableList<Object> arguments = ImmutableList.copyOf(m.arguments());
                    oscMessages.put(m.addrPattern(), arguments);
                }
            }

            @Override
            public void oscStatus(OscStatus ignored) {
            }
        });
    }

    @Override
    public void pause() {
        paused = true;
    }

    @Override
    public void resume() {
        paused = false;
        if (oscP5 == null)
            start();
    }

    @Override
    public void stop() {
        if (oscP5 != null)
            oscP5.stop();
        oscP5 = null;
        paused = false;
    }

    @Override
    public void addData(Map<String, Object> map) {
        map.put(getName() + ".messages", getOscMessages());
    }

    @Override
    public AbstractDeviceControl createControl() {
        return new OSCDeviceControl(this);
    }

    private class OSCDeviceControl extends AbstractDeviceControl {

        private JLabel deviceNameLabel;
        private JTextField portNumberField;
        private JCheckBox syncWithTimelineCheck;
        private JButton startButton;
        private JButton stopButton;
        private JButton clearButton;

        public OSCDeviceControl(OSCDeviceHandler deviceHandler) {
            super(deviceHandler);
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

            Dimension d = new Dimension(550, 30);
            setPreferredSize(d);
            setMaximumSize(d);
            setSize(d);

            deviceNameLabel = new JLabel(deviceHandler.getName());
            portNumberField = new JTextField();
            portNumberField.setText(String.valueOf(getPort()));
            portNumberField.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    changePortNumber();
                }
            }
            );
            portNumberField.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent focusEvent) {
                    changePortNumber();
                }
            });
            portNumberField.setPreferredSize(new Dimension(70, portNumberField.getHeight()));
            portNumberField.setMinimumSize(new Dimension(70, portNumberField.getHeight()));

            syncWithTimelineCheck = new JCheckBox("Sync with Timeline");
            syncWithTimelineCheck.setSelected(isSyncedWithTimeline());
            syncWithTimelineCheck.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent itemEvent) {
                    syncWithTimeline = syncWithTimelineCheck.isSelected();
                    setPropertyValue(Device.TIMELINE_SYNC, String.valueOf(syncWithTimeline));
                }
            });
            startButton = new JButton();

            if (isRunning()) {
                startButton.setText(isPaused() ? "Start" : "Pause");
            } else {
                startButton.setText("Start");
            }
            startButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    if (! isRunning()) {
                        startOSC();
                    } else if (isPaused()) {
                        resumeOSC();
                    } else {
                        pauseOSC();
                    }
                }
            });
            stopButton = new JButton("Stop");
            stopButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    stopOSC();
                }
            });
            stopButton.setEnabled(oscP5 != null);
            clearButton = new JButton("Clear");
            clearButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    oscMessages.clear();
                }
            });
            add(Box.createHorizontalStrut(10));
            add(deviceNameLabel);
            add(Box.createHorizontalStrut(5));
            add(portNumberField);
            add(Box.createHorizontalStrut(5));
            add(syncWithTimelineCheck);
            add(Box.createHorizontalStrut(5));
            add(startButton);
            add(Box.createHorizontalStrut(5));
            add(stopButton);
            add(Box.createHorizontalStrut(5));
            add(clearButton);
            add(Box.createHorizontalGlue());
        }

        private void startOSC() {
            start();
            if (isRunning())
            startButton.setText(isRunning() ? "Pause" : "Start");
            stopButton.setEnabled(oscP5 != null);
        }

        private void resumeOSC() {
            resume();
            startButton.setText(isRunning() ? "Pause" : "Start");
            stopButton.setEnabled(true);
        }

        private void pauseOSC() {
            pause();
            startButton.setText(isRunning() ? "Resume" : "Start");
            stopButton.setEnabled(true);
        }

        private void stopOSC() {
            stop();
            startButton.setText("Start");
            stopButton.setEnabled(false);
        }

        private void changePortNumber() {
            try {
                int newPort = Integer.parseInt(portNumberField.getText());
                stopOSC();
                oscPort = newPort;
                setPropertyValue("port", String.valueOf(newPort));
            } catch (Exception e) {
                // todo: better error handling of invalid port values
                portNumberField.setText(String.valueOf(getPort()));
                return;
            }
        }

    }

}
