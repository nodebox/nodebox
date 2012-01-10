package nodebox.graphics;

public class GeometryContext extends AbstractGraphicsContext {

    private Geometry geometry;

    public GeometryContext() {
        geometry = new Geometry();
    }

    public GeometryContext(Geometry geometry) {
        this.geometry = geometry;
    }

    public Geometry getGeometry() {
        return geometry;
    }

    @Override
    protected void addPath(Path p) {
        geometry.add(p);
    }

    @Override
    protected void addText(Text t) {
        addPath(t.getPath());
    }

}
