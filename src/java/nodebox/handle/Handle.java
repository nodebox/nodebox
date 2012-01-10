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

    public boolean keyTyped(int keyCode, int modifiers);

    public boolean keyPressed(int keyCode, int modifiers);

    public boolean keyReleased(int keyCode, int modifiers);

    //// Node update methods ////

    /**
     * Set a value on the node.
     * <p/>
     * This callback is fired whenever we want to set a value and have an error reported back.
     * This method can be called for every drag or move of the mouse, if needed.
     *
     * @param parameterName The parameter this value is linked to.
     * @param value         The ne value.
     */
    public void setValue(String parameterName, Object value);

    /**
     * Set a value on the node without causing an error.
     * <p/>
     * This callback is fired whenever we want to set a value, but ignore every error.
     * For handles, this is the default, since we don't want to mess with error handling.
     * This automatically does the right thing.
     * <p/>
     * For example, on a constrained handle where width / height need to be equal, calling this method
     * will keep them in sync without raising errors that one can't be bigger than the other.
     *
     * @param parameterName The parameter this value is linked to.
     * @param value         The ne value.
     */
    public void silentSet(String parameterName, Object value);

    /**
     * Indicates that the undo mechanism should create a new undo "step".
     * <p/>
     * Use this when something significant has happened in your code, e.g. when you've drawn a line in the freehand node.
     */
    public void stopCombiningEdits();

    /**
     * Indicates that the handle needs to be repainted.
     * <p/>
     * The handle is repainted every time a value is changed.
     * Use this method whenever you want to repaint the handle without changing a value.
     */
    public void updateHandle();

    //// Event listener ////

    public HandleDelegate getHandleDelegate();

    public void setHandleDelegate(HandleDelegate delegate);

}
