package nodebox.node;

import nodebox.graphics.Canvas;
import nodebox.graphics.Geometry;

public class WrapInCanvasCode implements NodeCode {

    public Object cook(Node node, ProcessingContext context) {
        Object o = Node.ROOT_NODE.cook(node, context);
        // We also wrap null, which is the result if there are no child nodes.
        if (o == null || o instanceof Geometry) {
            Canvas canvas = new Canvas();
            canvas.setOffsetX(node.asFloat("canvasX"));
            canvas.setOffsetY(node.asFloat("canvasY"));
            canvas.setSize(node.asFloat("canvasWidth"), node.asFloat("canvasHeight"));
            canvas.setBackground(node.asColor("canvasBackground"));
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
