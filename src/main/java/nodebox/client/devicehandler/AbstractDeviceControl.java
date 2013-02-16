package nodebox.client.devicehandler;

import javax.swing.*;

public class AbstractDeviceControl extends JComponent implements DeviceControl {
    protected DeviceHandler deviceHandler;

    private OnPropertyChangeListener onPropertyChangeListener;

    protected AbstractDeviceControl(DeviceHandler deviceHandler) {
        this.deviceHandler = deviceHandler;
    }

    public void setPropertyChangeListener(OnPropertyChangeListener l) {
        onPropertyChangeListener = l;
    }

    public OnPropertyChangeListener getPropertyChangeListener() {
        return onPropertyChangeListener;
    }

    public void setPropertyValue(String key, String value) {
        if (onPropertyChangeListener != null)
            onPropertyChangeListener.onPropertyChange(deviceHandler.getName(), key, value);
    }

}
