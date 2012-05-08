package nodebox.handle;

import nodebox.graphics.GraphicsContext;
import nodebox.graphics.Point;

import java.util.ArrayList;
import java.util.List;

public class CombinedHandle extends AbstractHandle {
    private List<Handle> handles;

    public CombinedHandle() {
        handles = new ArrayList<Handle>();
        setVisible(false);
    }

    @Override
    public void setHandleDelegate(HandleDelegate delegate) {
        super.setHandleDelegate(delegate);
        for (Handle handle : handles)
            handle.setHandleDelegate(delegate);
    }

    public void addHandle(Handle handle) {
        handles.add(handle);
    }

    public boolean mouseClicked(Point pt) {
        for (Handle handle : handles) {
            boolean clicked = handle.mouseClicked(pt);
            if (clicked)
                return true;
        }
        return false;
    }

    public boolean mousePressed(Point pt) {
        for (Handle handle : handles) {
            boolean pressed = handle.mousePressed(pt);
            if (pressed)
                return true;
        }
        return false;
    }

    public boolean mouseMoved(Point pt) {
        boolean moved = false;
        for (Handle handle : handles) {
            if (handle.mouseMoved(pt))
                moved = true;
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

    public boolean mouseEntered(Point pt) {
        for (Handle handle : handles) {
            boolean entered = handle.mouseEntered(pt);
            if (entered)
                return true;
        }
        return false;
    }

    public boolean mouseExited(Point pt) {
        for (Handle handle : handles) {
            boolean exited = handle.mouseExited(pt);
            if (exited)
                return true;
        }
        return false;
    }

    public boolean keyTyped(int keyCode, int modifiers) {
        for (Handle handle : handles) {
            boolean keyTyed = handle.keyTyped(keyCode, modifiers);
            if (keyTyed)
                return true;
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int modifiers) {
        for (Handle handle : handles) {
            boolean keyPressed = handle.keyPressed(keyCode, modifiers);
            if (keyPressed)
                return true;
        }
        return false;
    }

    public boolean keyReleased(int keyCode, int modifiers) {
        for (Handle handle : handles) {
            boolean keyReleased = handle.keyReleased(keyCode, modifiers);
            if (keyReleased)
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
