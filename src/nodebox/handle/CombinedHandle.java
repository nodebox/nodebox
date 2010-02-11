package nodebox.handle;

import nodebox.client.Viewer;
import nodebox.graphics.GraphicsContext;
import nodebox.graphics.Point;
import nodebox.node.Node;

import java.util.ArrayList;
import java.util.List;

public class CombinedHandle extends AbstractHandle {
    private List<Handle> handles;

    public CombinedHandle(Node node) {
        super(node);
        handles = new ArrayList<Handle>();
        setVisible(false);
    }

    public void setViewer(Viewer viewer) {
        super.setViewer(viewer);
        for (Handle handle : handles)
            handle.setViewer(viewer);
    }

    public void addHandle(Handle handle) {
        handles.add(handle);
    }

    public boolean mousePressed(Point pt) {
        for (Handle handle : handles) {
            boolean pressed = handle.mousePressed(pt);
            if (pressed)
                return true;
        }
        return false;
    }

    public boolean mouseDragged(Point pt) {
        for (Handle handle : handles) {
            boolean dragged = handle.mouseDragged(pt);
            if (dragged)
                return true;
        }
        return false;
    }

    public boolean mouseReleased(Point pt) {
        for (Handle handle : handles) {
            boolean released = handle.mouseReleased(pt);
            if (released)
                return true;
        }
        return false;
    }

    public void update() {
        for (Handle handle : handles)
            handle.update();

        setVisible(true);
    }

    public void draw(GraphicsContext ctx) {
        for (Handle handle : handles)
            handle.draw(ctx);
    }
}
