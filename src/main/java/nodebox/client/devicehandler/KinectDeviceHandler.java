package nodebox.client.devicehandler;

import com.google.common.collect.ImmutableMap;
import nodebox.client.KinectWindow;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.Map;
import SimpleOpenNI.*;

public class KinectDeviceHandler implements DeviceHandler {
    private String name;

    private boolean depthEnabled = false;
    private boolean rgbEnabled = false;
    private boolean sceneEnabled = false;
    private boolean skeletonEnabled = false;

    private KinectWindow kinectWindow;

    public KinectDeviceHandler(String name) {
        this.name = name;
        kinectWindow = null;
    }

    public String getName() {
        return name;
    }

    public boolean isDepthEnabled() {
        return depthEnabled;
    }

    public boolean isRGBEnabled() {
        return rgbEnabled;
    }

    public boolean isSceneEnabled() {
        return sceneEnabled;
    }

    public boolean isSkeletonEnabled() {
        return skeletonEnabled;
    }

    public void enableDepth(boolean enable) {
        depthEnabled = enable;
    }

    public void enableRGB(boolean enable) {
        rgbEnabled = enable;
    }

    public void enableScene(boolean enable) {
        sceneEnabled = enable;
    }

    public void enableSkeleton(boolean enable) {
        skeletonEnabled = enable;
    }

    private Map<Integer, Map<String, List<Float>>> getSkeletonData() {
        if (kinectWindow == null) return ImmutableMap.of();
        return kinectWindow.getSkeletonData();
    }

    private SimpleOpenNI getContext() {
        if (kinectWindow == null) return null;
        return kinectWindow.getContext();
    }

    public void start() {
        if (kinectWindow != null) {
            kinectWindow.setVisible(true);
            return;
        }
        kinectWindow = new KinectWindow(isDepthEnabled(), isRGBEnabled(), isSceneEnabled(), isSkeletonEnabled());
        kinectWindow.setVisible(true);
    }

    public boolean isRunning() {
        return kinectWindow != null;
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
        if (getContext() != null)
            map.put("kinect.context", getContext());
    }

    public AbstractDeviceControl createControl() {
        return new KinectDeviceControl(this);
    }

    private class KinectDeviceControl extends AbstractDeviceControl {

        private JLabel deviceNameLabel;
        private JButton startButton;
        private JButton stopButton;
        private JCheckBox depthCheck;
        private JCheckBox rgbCheck;
        private JCheckBox sceneCheck;
        private JCheckBox skeletonCheck;

        public KinectDeviceControl(KinectDeviceHandler deviceHandler) {
            super(deviceHandler);
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

            Dimension d = new Dimension(450, 60);
            setPreferredSize(d);
            setMaximumSize(d);
            setSize(d);

            deviceNameLabel = new JLabel(deviceHandler.getName());
            startButton = new JButton("Start");
            startButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    start();
                    checkValidInputs();
                }
            });
            stopButton = new JButton("Stop");
            stopButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    if (kinectWindow != null) {
                        String message = "If you stop the Kinect now, it cannot be used until you restart NodeBox. Do you really want to do this?";
                        String title = "Are you sure?";
                        int reply = JOptionPane.showConfirmDialog(null, message, title, JOptionPane.YES_NO_OPTION);
                        if (reply == JOptionPane.YES_OPTION) {
                            stop();
                            checkValidInputs();
                        }
                    }
                }
            });
            add(Box.createHorizontalStrut(10));
            add(deviceNameLabel);
            add(Box.createHorizontalStrut(5));

            JPanel mainPanel = new JPanel();
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

            JPanel startStopPanel = new JPanel();
            startStopPanel.setLayout(new BoxLayout(startStopPanel, BoxLayout.X_AXIS));

            startStopPanel.add(startButton);
            startStopPanel.add(Box.createHorizontalStrut(5));
            startStopPanel.add(stopButton);

            JPanel enablePanel = new JPanel();
            enablePanel.setLayout(new BoxLayout(enablePanel, BoxLayout.X_AXIS));

            depthCheck = new JCheckBox("Depth");
            depthCheck.setSelected(isDepthEnabled());
            depthCheck.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent itemEvent) {
                    enableDepth(depthCheck.isSelected());
                    setPropertyValue("depthEnabled", String.valueOf(depthEnabled));
                }
            });

            enablePanel.add(depthCheck);
            enablePanel.add(Box.createHorizontalStrut(5));

            rgbCheck = new JCheckBox("Color");
            rgbCheck.setSelected(isRGBEnabled());
            rgbCheck.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent itemEvent) {
                    enableRGB(rgbCheck.isSelected());
                    setPropertyValue("rgbEnabled", String.valueOf(rgbEnabled));
                }
            });

            enablePanel.add(rgbCheck);
            enablePanel.add(Box.createHorizontalStrut(5));

            sceneCheck = new JCheckBox("Scene");
            sceneCheck.setSelected(isSceneEnabled());
            sceneCheck.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent itemEvent) {
                    enableScene(sceneCheck.isSelected());
                    setPropertyValue("sceneEnabled", String.valueOf(sceneEnabled));
                }
            });

            enablePanel.add(sceneCheck);
            enablePanel.add(Box.createHorizontalStrut(5));

            skeletonCheck = new JCheckBox("Skeleton");
            skeletonCheck.setSelected(isSkeletonEnabled());
            skeletonCheck.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent itemEvent) {
                    enableSkeleton(skeletonCheck.isSelected());
                    setPropertyValue("skeletonEnabled", String.valueOf(skeletonEnabled));
                }
            });

            enablePanel.add(skeletonCheck);

            mainPanel.add(enablePanel);
            mainPanel.add(startStopPanel);

            checkValidInputs();
            add(mainPanel);
            add(Box.createHorizontalGlue());
        }

        private void checkValidInputs() {
            boolean enabled = ! isRunning();
            depthCheck.setEnabled(enabled);
            rgbCheck.setEnabled(enabled);
            sceneCheck.setEnabled(enabled);
            skeletonCheck.setEnabled(enabled);
            startButton.setText(isRunning() ? "Show" : "Start");
            stopButton.setEnabled(! enabled);
        }
    }
}
