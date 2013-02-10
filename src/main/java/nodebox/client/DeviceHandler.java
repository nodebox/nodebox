package nodebox.client;

public interface DeviceHandler {
    public String getName();
    public void stop();
    public AbstractDeviceControl createControl();
}
