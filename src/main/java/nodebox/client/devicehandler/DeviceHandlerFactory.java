package nodebox.client.devicehandler;

import nodebox.node.Device;

public class DeviceHandlerFactory {

    public static DeviceHandler createDeviceHandler(Device device) {
        if (device.getType().equals(Device.TYPE_OSC))
            return createOSCDeviceHandler(device);
        else if (device.getType().equals(Device.TYPE_AUDIOPLAYER))
            return createAudioPlayerDeviceHandler(device);
        else if (device.getType().equals(Device.TYPE_AUDIOINPUT))
            return createAudioInputDeviceHandler(device);
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

    private static DeviceHandler createAudioPlayerDeviceHandler(Device device) {
        String fileName = device.getProperty("filename", "");
        boolean autostart = Boolean.parseBoolean(device.getProperty("autostart", "false"));
        AudioPlayerDeviceHandler handler = new AudioPlayerDeviceHandler(device.getName(), fileName, autostart);
        if (autostart)
            handler.start();
        return handler;
    }

    private static DeviceHandler createAudioInputDeviceHandler(Device device) {
        boolean autostart = Boolean.parseBoolean(device.getProperty("autostart", "false"));
        AudioInputDeviceHandler handler = new AudioInputDeviceHandler(device.getName(), autostart);
        if (autostart)
            handler.start();
        return handler;
    }
}
