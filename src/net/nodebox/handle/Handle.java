package net.nodebox.handle;


import net.nodebox.graphics.GraphicsContext;
import net.nodebox.node.Node;

import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public interface Handle extends MouseListener, MouseMotionListener {

    //public List<Parameter> getParameters();

    public Node getNode();

    public void draw(GraphicsContext ctx);

}
