package nodebox.handle;

/**
 * A Handle for creating lines consisting of two independent points.
 */
public class LineHandle extends CombinedHandle {

    public LineHandle() {
        addHandle(new PointHandle("point1"));
        addHandle(new PointHandle("point2"));
        update();
    }

}
