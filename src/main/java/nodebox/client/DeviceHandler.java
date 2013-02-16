package nodebox.client;

import java.util.Map;

public interface DeviceHandler {
    public String getName();
    public void stop();
    public AbstractDeviceControl createControl();
    public void addData(Map map);
}
