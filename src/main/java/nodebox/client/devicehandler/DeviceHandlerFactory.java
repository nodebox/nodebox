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
        return new OSCDeviceHandler(device.getName(), port, autostart);
    }

    private static DeviceHandler createAudioPlayerDeviceHandler(Device device) {
        String fileName = device.getProperty("filename", "");
        boolean autostart = Boolean.parseBoolean(device.getProperty("autostart", "false"));
        return new AudioPlayerDeviceHandler(device.getName(), fileName, autostart);
    }

    private static DeviceHandler createAudioInputDeviceHandler(Device device) {
        boolean autostart = Boolean.parseBoolean(device.getProperty("autostart", "false"));
        return new AudioInputDeviceHandler(device.getName(), autostart);
    }
}
