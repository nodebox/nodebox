package nodebox.client.devicehandler;

import nodebox.node.Device;

public class DeviceHandlerFactory {

    public static DeviceHandler createDeviceHandler(Device device) {
        if (device.getType().equals(Device.TYPE_OSC))
            return createOSCDeviceHandler(device);
        else if (device.getType().equals(Device.TYPE_KINECT))
            return createKinectDeviceHandler(device);
        else if (device.getType().equals(Device.TYPE_AUDIOPLAYER))
            return createAudioPlayerDeviceHandler(device);
        return null;
    }

    private static DeviceHandler createOSCDeviceHandler(Device device) {
        int port = Integer.parseInt(device.getProperty("port", "-1"));
        boolean autostart = Boolean.parseBoolean(device.getProperty("autostart", "false"));
        OSCDeviceHandler handler = new OSCDeviceHandler(device.getName(), port, autostart);
        if (autostart)
            handler.start();
        return handler;
    }

    private static DeviceHandler createKinectDeviceHandler(Device device) {
        boolean depthEnabled = Boolean.parseBoolean(device.getProperty("depthEnabled", "true"));
        boolean rgbEnabled = Boolean.parseBoolean(device.getProperty("rgbEnabled", "false"));
        boolean sceneEnabled = Boolean.parseBoolean(device.getProperty("sceneEnabled", "false"));
        boolean skeletonEnabled = Boolean.parseBoolean(device.getProperty("skeletonEnabled", "false"));
        KinectDeviceHandler handler = new KinectDeviceHandler(device.getName());
        handler.enableDepth(depthEnabled);
        handler.enableRGB(rgbEnabled);
        handler.enableScene(sceneEnabled);
        handler.enableSkeleton(skeletonEnabled);
        return handler;
    }

    private static DeviceHandler createAudioPlayerDeviceHandler(Device device) {
        String fileName = device.getProperty("filename", "");
        boolean autostart = Boolean.parseBoolean(device.getProperty("autostart", "false"));
        AudioPlayerDeviceHandler handler = new AudioPlayerDeviceHandler(device.getName(), fileName, autostart);
        if (autostart)
            handler.start();
        return handler;
    }
}
