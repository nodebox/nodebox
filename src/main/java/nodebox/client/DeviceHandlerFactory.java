package nodebox.client;

import nodebox.node.Device;

public class DeviceHandlerFactory {

    public static DeviceHandler createDeviceHandler(Device device) {
        if (device.getType().equals(Device.TYPE_OSC))
            return createOSCDeviceHandler(device);
        else if (device.getType().equals(Device.TYPE_KINECT))
            return createKinectDeviceHandler(device);
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
        DeviceHandler handler = new KinectDeviceHandler(device.getName());
        return handler;
    }
}
