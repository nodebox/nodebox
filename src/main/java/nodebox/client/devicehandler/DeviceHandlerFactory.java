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

    private static boolean isSyncedWithTimeline(Device device) {
        return Boolean.parseBoolean(device.getProperty(Device.TIMELINE_SYNC, "false"));
    }

    private static DeviceHandler createOSCDeviceHandler(Device device) {
        int port = Integer.parseInt(device.getProperty("port", "-1"));
        return new OSCDeviceHandler(device.getName(), port, isSyncedWithTimeline(device));
    }

    private static DeviceHandler createAudioPlayerDeviceHandler(Device device) {
        String fileName = device.getProperty("filename", "");
        return new AudioPlayerDeviceHandler(device.getName(), fileName, isSyncedWithTimeline(device));
    }

    private static DeviceHandler createAudioInputDeviceHandler(Device device) {
        return new AudioInputDeviceHandler(device.getName(), isSyncedWithTimeline(device));
    }
}
