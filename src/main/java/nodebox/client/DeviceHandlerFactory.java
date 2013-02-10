package nodebox.client;

import nodebox.node.Device;

public class DeviceHandlerFactory {

    public static DeviceHandler createDeviceHandler(Device device) {
        if (device.getType().equals(Device.TYPE_OSC))
            return createOSCDeviceHandler(device);
        return null;
    }

    private static DeviceHandler createOSCDeviceHandler(Device device) {
        int port = Integer.parseInt((String) device.getProperties().get("port"));
        boolean autostart = Boolean.parseBoolean((String) device.getProperties().get("autostart"));
        OSCDeviceHandler handler = new OSCDeviceHandler(device.getName(), port, autostart);
        if (autostart)
            handler.start();
        return handler;
    }
}
