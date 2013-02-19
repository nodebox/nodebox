package nodebox.client.devicehandler;

import com.google.common.collect.ImmutableMap;
import nodebox.client.KinectWindow;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;

public class KinectDeviceHandler implements DeviceHandler {
    private String name;
    private KinectWindow kinectWindow;

    public KinectDeviceHandler(String name) {
        this.name = name;
        kinectWindow = null;
    }

    public String getName() {
        return name;
    }

    private Map<Integer, Map<String, List<Float>>> getSkeletonData() {
        if (kinectWindow == null) return ImmutableMap.of();
        return kinectWindow.getSkeletonData();
    }

    public void start() {
        if (kinectWindow != null) {
            kinectWindow.setVisible(true);
            return;
        }
        kinectWindow = new KinectWindow();
        kinectWindow.setVisible(true);
    }

    public void stop() {
        if (kinectWindow != null) {
            kinectWindow.stop();
            kinectWindow.dispose();
            kinectWindow = null;
        }
    }

    public void addData(Map map) {
        map.put("kinect.skeletondata", getSkeletonData());
    }

    public AbstractDeviceControl createControl() {
        return new KinectDeviceControl(this);
    }

    private class KinectDeviceControl extends AbstractDeviceControl {

        private JLabel deviceNameLabel;
        private JButton startButton;
        private JButton stopButton;

        public KinectDeviceControl(KinectDeviceHandler deviceHandler) {
            super(deviceHandler);
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

            Dimension d = new Dimension(450, 30);
            setPreferredSize(d);
            setMaximumSize(d);
            setSize(d);

            deviceNameLabel = new JLabel(deviceHandler.getName());
            startButton = new JButton("Start");
            startButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    start();
                }
            });
            stopButton = new JButton("Stop");
            stopButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    stop();
                }
            });
            add(Box.createHorizontalStrut(10));
            add(deviceNameLabel);
            add(Box.createHorizontalStrut(5));
            add(startButton);
            add(Box.createHorizontalStrut(5));
            add(stopButton);
            add(Box.createHorizontalGlue());
        }
    }
}
