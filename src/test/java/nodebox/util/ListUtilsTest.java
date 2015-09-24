package nodebox.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import nodebox.graphics.IGeometry;
import org.junit.Test;

import static junit.framework.TestCase.assertSame;
import static nodebox.util.ListUtils.listClass;

public class ListUtilsTest {

    @Test
    public void testListClass() {
        assertSame(Integer.class, listClass(Lists.newArrayList(1, 2)));
        assertSame(String.class, listClass(ImmutableList.of("a", "b")));
        assertSame(Number.class, listClass(Lists.<Number>newArrayList(1, 2.0)));
        assertSame(Object.class, listClass(ImmutableList.of()));
        assertSame(Object.class, listClass(Lists.newArrayList(1, null, 2)));
        assertSame(Object.class, listClass(Lists.newArrayList(null, null, 1)));
        assertSame(nodebox.graphics.Path.class, listClass(Lists.newArrayList(new nodebox.graphics.Path(), new nodebox.graphics.Path())));
        assertSame(nodebox.graphics.Geometry.class, listClass(Lists.newArrayList(new nodebox.graphics.Geometry(), new nodebox.graphics.Geometry())));
        assertSame(nodebox.graphics.AbstractGeometry.class, listClass(Lists.<IGeometry>newArrayList(new nodebox.graphics.Path(), new nodebox.graphics.Geometry())));
        assertSame(Object.class, listClass(Lists.<IGeometry>newArrayList(new nodebox.graphics.Geometry(), null, new nodebox.graphics.Path())));
    }

}
