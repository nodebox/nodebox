package nodebox.graphics;


import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.awt.geom.Rectangle2D;

import static nodebox.graphics.SVGRenderer.*;
import static org.junit.Assert.assertEquals;

public class SVGRendererTest {

    @Test
    public void testRenderPath() {
        SVGRenderer.Element el;

        Path p = new Path();
        p.line(10, 20, 30, 40);
        el = renderPath(p);
        assertElementEquals("<path d=\"M10,20L30,40\"/>", el);

        p.close();
        el = renderPath(p);
        assertElementEquals("<path d=\"M10,20L30,40Z\"/>", el);
    }

    @Test
    public void testColor() {
        Path p = new Path();
        p.line(10, 20, 30, 40);
        p.setFill(new Color("#334455"));
        SVGRenderer.Element el = renderPath(p);
        assertElementEquals("<path d=\"M10,20L30,40\" fill=\"#334455\"/>", el);
    }

    @Test
    public void testRenderSVG() {
        Path p = new Path();
        p.line(10, 20, 30, 40);
        String svg = renderToString(ImmutableList.of(p), new Rectangle2D.Float(0, 0, 800, 600));
        assertEquals(XML_DECLARATION + "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"800\" height=\"600\" viewBox=\"0 0 800 600\">\n" +
                "    <path d=\"M10,20L30,40\"/>\n" +
                "</svg>", svg);
    }

    private void assertElementEquals(String expected, SVGRenderer.Element el) {
        assertEquals(expected, el.toString());
    }

}
