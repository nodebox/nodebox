package nodebox.client.devicehandler;

public interface DeviceControl {
    public void setPropertyChangeListener(OnPropertyChangeListener l);

    public OnPropertyChangeListener getPropertyChangeListener();

    public static interface OnPropertyChangeListener {
        public void onPropertyChange(String deviceName, String key, String newValue);
    }
}
