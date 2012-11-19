package nodebox.ui;

import java.awt.Point;

public interface Zoom {
    public void zoom(double scaleDelta);
    public boolean containsPoint(Point point);
}
