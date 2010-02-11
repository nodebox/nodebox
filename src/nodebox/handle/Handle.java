package nodebox.handle;


import nodebox.client.Viewer;
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

    public void keyTyped(int keyCode, int modifiers);

    public void keyPressed(int keyCode, int modifiers);

    public void keyReleased(int keyCode, int modifiers);

}
