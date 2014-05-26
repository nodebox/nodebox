package nodebox.client.devicehandler;

import java.util.Map;

public interface DeviceHandler {
    public String getName();
    public boolean isAutoStart();
    public void start();
    public void stop();
    public AbstractDeviceControl createControl();
    public void addData(Map<String, Object> map);
}
