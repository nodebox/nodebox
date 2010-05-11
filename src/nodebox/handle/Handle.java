package nodebox.handle;


import nodebox.client.Viewer;
import nodebox.graphics.CanvasContext;
import nodebox.graphics.GraphicsContext;
import nodebox.graphics.Point;
import nodebox.node.Node;

public interface Handle {

    public void setViewer(Viewer viewer);

    public Viewer getViewer();

    //public List<Parameter> getParameters();

    public Node getNode();

    public void update();

    public void draw(GraphicsContext ctx);

    public void setVisible(boolean visible);

    public boolean isVisible();

    //// Mouse events ////

    public boolean mouseClicked(Point pt);

    public boolean mousePressed(Point pt);

    public boolean mouseReleased(Point pt);

    public boolean mouseEntered(Point pt);

    public boolean mouseExited(Point pt);

    public boolean mouseDragged(Point pt);

    public boolean mouseMoved(Point pt);

    public boolean keyTyped(int keyCode, int modifiers);

    public boolean keyPressed(int keyCode, int modifiers);

    public boolean keyReleased(int keyCode, int modifiers);

}
