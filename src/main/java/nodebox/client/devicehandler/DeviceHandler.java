package nodebox.client.devicehandler;

import java.util.Map;

public interface DeviceHandler {
    public String getName();
    public boolean isSyncedWithTimeline();
    public void start();
    public void pause();
    public void resume();
    public void stop();
    public AbstractDeviceControl createControl();
    public void addData(Map<String, Object> map);
}
