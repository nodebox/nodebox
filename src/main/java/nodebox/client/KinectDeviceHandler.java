package nodebox.client;

import com.google.common.collect.ImmutableMap;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;

public class KinectDeviceHandler implements DeviceHandler {
    private String name;
    private ProcessingFrame frame;

    public KinectDeviceHandler(String name) {
        this.name = name;
        frame = null;
    }

    public String getName() {
        return name;
    }

    public Map<Integer, Map<String, List<Float>>> getSkeletonData() {
        if (frame == null) return ImmutableMap.of();
        return frame.getSkeletonData();
    }

    public void start() {
        frame = new ProcessingFrame();
        frame.setVisible(true);
    }

    public void stop() {
        frame.stop();
        frame.dispose();
        frame = null;

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
