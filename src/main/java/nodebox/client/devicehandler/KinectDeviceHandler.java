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

    private String fileName = "";
    private boolean useFile = false;

    private String currentView = "Depth";

    private KinectWindow kinectWindow;

    public KinectDeviceHandler(String name) {
        this.name = name;
        kinectWindow = null;
    }

    public String getName() {
        return name;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public boolean shouldUseFile() {
        return useFile;
    }

    public void setUseFile(boolean useFile) {
        this.useFile = useFile;
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

    public void setCurrentView(String view) {
        this.currentView = view;
        if (kinectWindow != null)
            kinectWindow.setView(view);
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
        kinectWindow.setView(currentView);
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
        private JCheckBox useFileCheck;
        private JTextField fileNameField;
        private JButton fileButton;
        private JButton clearButton;
        private JComboBox viewBox;


        public KinectDeviceControl(KinectDeviceHandler deviceHandler) {
            super(deviceHandler);
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

            Dimension d = new Dimension(450, 80);
            setPreferredSize(d);
            setMaximumSize(d);
            setSize(d);

            deviceNameLabel = new JLabel(deviceHandler.getName());
            add(Box.createHorizontalStrut(10));
            add(deviceNameLabel);
            add(Box.createHorizontalStrut(5));

            JPanel recordingPanel = new JPanel();
            recordingPanel.setLayout(new BoxLayout(recordingPanel, BoxLayout.X_AXIS));

            fileNameField = new JTextField();
            fileNameField.setText(getFileName());

            fileButton = new JButton("File...");
            fileButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                }
            });

            clearButton = new JButton("Clear");
            clearButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    setFileName("");
                    fileNameField.setText("");
                    setPropertyValue("filename", "");
                }
            });

            useFileCheck = new JCheckBox("Use File");
            useFileCheck.setSelected(shouldUseFile());
            useFileCheck.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent itemEvent) {
                    setUseFile(useFileCheck.isSelected());
                    setPropertyValue("useFile", String.valueOf(useFile));
                }
            });

            recordingPanel.add(useFileCheck);
            recordingPanel.add(Box.createHorizontalStrut(5));
            recordingPanel.add(fileNameField);
            recordingPanel.add(Box.createHorizontalStrut(5));
            recordingPanel.add(fileButton);
            recordingPanel.add(Box.createHorizontalStrut(5));
            recordingPanel.add(clearButton);
            recordingPanel.add(Box.createHorizontalGlue());

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

            viewBox = new JComboBox();
            viewBox.addItem("Depth");
            viewBox.addItem("Color");
            viewBox.addItem("Scene");
            viewBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    String view = (String) viewBox.getSelectedItem();
                    setCurrentView(view);
                }
            });
            viewBox.setSelectedItem(currentView);

            JPanel startStopPanel = new JPanel();
            startStopPanel.setLayout(new BoxLayout(startStopPanel, BoxLayout.X_AXIS));
            startStopPanel.add(startButton);
            startStopPanel.add(Box.createHorizontalStrut(5));
            startStopPanel.add(stopButton);
            startStopPanel.add(Box.createHorizontalStrut(5));
            startStopPanel.add(new JLabel("View:"));
            startStopPanel.add(viewBox);
            startStopPanel.add(Box.createHorizontalGlue());

            JPanel mainPanel = new JPanel();
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
            mainPanel.add(recordingPanel);
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
