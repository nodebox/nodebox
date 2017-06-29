package nodebox.graphics;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class CSVRenderTest {

    @Test
    public void testPath() {
        Path p = new Path();
        p.cornerRect(10, 20, 30, 40);
        Set<String> keys = CSVRenderer.keySet(p);
        assertEquals(ImmutableSet.of("d", "fill", "stroke", "stroke-width", "x", "y", "width", "height"), keys);
        Map<String, ?> m = CSVRenderer.objectAsMap(p);
        assertEquals(m.get("x"), 10.0);
        assertEquals(m.get("y"), 20.0);
        assertEquals(m.get("width"), 30.0);
        assertEquals(m.get("height"), 40.0);
    }

    @Test
    public void testNumber() {
        Double d = 42.0;
        assertEquals(ImmutableSet.of("value"), CSVRenderer.keySet(d));
        assertEquals(ImmutableMap.of("value", 42.0), CSVRenderer.objectAsMap(d));
    }

    @Test
    public void testNull() {
        assertEquals(ImmutableSet.of("value"), CSVRenderer.keySet(null));
        assertEquals(ImmutableMap.of("value", "null"), CSVRenderer.objectAsMap(null));
    }

    @Test
    public void testMap() {
        Map<String, Integer> data = ImmutableMap.of("alpha", 1, "beta", 2, "gamma", 3);
        assertEquals(ImmutableSet.of("alpha", "beta", "gamma"), CSVRenderer.keySet(data));
        assertEquals(data, CSVRenderer.objectAsMap(data));
        List<Map<String, Integer>> objects = ImmutableList.of(data);
        String s = CSVRenderer.renderToString(objects, ';', true);
        assertEquals("\"alpha\";\"beta\";\"gamma\"\n\"1\";\"2\";\"3\"\n", s);
    }

    @Test
    public void testEscaping() {
        List<String> objects = ImmutableList.of("How \"are\" you?");
        assertEquals(ImmutableSet.of("value"), CSVRenderer.keySet(objects.get(0)));
        String s1 = CSVRenderer.renderToString(objects, ';', true);
        assertEquals("\"value\"\n\"How \"\"are\"\" you?\"\n", s1);
        String s2 = CSVRenderer.renderToString(objects, ';', false);
        assertEquals("value\nHow \"are\" you?\n", s2);
    }

}
