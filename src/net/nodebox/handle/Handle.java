package net.nodebox.handle;


import net.nodebox.graphics.GraphicsContext;
import net.nodebox.graphics.Point;
import net.nodebox.node.Node;

public interface Handle {

    //public List<Parameter> getParameters();

    public Node getNode();

    public void draw(GraphicsContext ctx);

    //// Mouse events ////

    public void mouseClicked(Point pt);

    public void mousePressed(Point pt);

    public void mouseReleased(Point pt);

    public void mouseEntered(Point pt);

    public void mouseExited(Point pt);

    public void mouseDragged(Point pt);

    public void mouseMoved(Point pt);


}
