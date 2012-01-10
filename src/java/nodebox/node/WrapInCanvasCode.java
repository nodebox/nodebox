package nodebox.node;

import nodebox.graphics.Canvas;
import nodebox.graphics.Geometry;

public class WrapInCanvasCode implements NodeCode {

    public Object cook(Node node, ProcessingContext context) {
        Object o = Node.ROOT_NODE.cook(node, context);
        // We also wrap null, which is the result if there are no child nodes.
        if (o == null || o instanceof Geometry) {
            Canvas canvas = new Canvas();
            canvas.setOffsetX(node.asFloat(NodeLibrary.CANVAS_X));
            canvas.setOffsetY(node.asFloat(NodeLibrary.CANVAS_Y));
            canvas.setSize(node.asFloat(NodeLibrary.CANVAS_WIDTH), node.asFloat(NodeLibrary.CANVAS_HEIGHT));
            canvas.setBackground(node.asColor(NodeLibrary.CANVAS_BACKGROUND));
            if (o != null) {
                Geometry geo = (Geometry) o;
                canvas.add(geo);
            }
            return canvas;
        } else {
            return o;
        }
    }

    public String getSource() {
        return Node.ROOT_NODE.getSource();
    }

    public String getType() {
        return "java";
    }
}
