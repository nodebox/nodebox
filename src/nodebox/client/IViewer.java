package nodebox.client;

/**
 * Viewer interface.
 */
public interface IViewer {

    public void setEventListener(ViewerEventListener e);

    public void repaint();

    Object getOutputValue();

    void setOutputValue(Object outputValue);

    boolean isHandleEnabled();

    void setHandleEnabled(boolean handleEnabled);
}
