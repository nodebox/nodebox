package nodebox.graphics;

public interface Colorizable {

    void setFillColor(Color fillColor);

    void setFill(Color c);

    void setStrokeColor(Color strokeColor);

    void setStroke(Color c);

    void setStrokeWidth(double strokeWidth);

    public Colorizable clone();
}
