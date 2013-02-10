package nodebox.client;

import javax.swing.*;

public class AbstractDeviceControl extends JComponent implements DeviceControl {
    protected DeviceHandler deviceHandler;

    protected AbstractDeviceControl(DeviceHandler deviceHandler) {
        this.deviceHandler = deviceHandler;
    }
}
