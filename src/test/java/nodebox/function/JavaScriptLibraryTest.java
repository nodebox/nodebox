package nodebox.function;

import org.junit.Test;
import org.mozilla.javascript.NativeArray;

import static org.junit.Assert.*;

public class JavaScriptLibraryTest {

    @Test
    public void testLoad() {
        JavaScriptLibrary lib = JavaScriptLibrary.loadScript("libraries/math/code/javascript/math.js");
        assertEquals("math", lib.getNamespace());
        assertTrue(lib.hasFunction("add"));
        Function fn = lib.getFunction("add");
        try {
            assertEquals(42.0, fn.invoke(40, 2));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testListOperations() {
        JavaScriptLibrary lib = JavaScriptLibrary.loadScript("libraries/list/code/javascript/list.js");
        assertEquals("list", lib.getNamespace());
        assertTrue(lib.hasFunction("distinct"));
        Function fn = lib.getFunction("distinct");
        try {
            Object o = fn.invoke(toJavaScriptArray(1, 2, 2, 3, 1));
            assertTrue(o instanceof NativeArray);
            assertArrayEquals(new Object[]{1, 2, 3}, fromJavaScriptArray((NativeArray) o));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private NativeArray toJavaScriptArray(Object... objects) {
        return new NativeArray(objects);
    }

    private Object[] fromJavaScriptArray(NativeArray a) {
        Object[] array = new Object[(int) a.getLength()];
        for (Object o : a.getIds()) {
            int index = (Integer) o;
            array[index] = a.get(index, null);
        }
        return array;
    }

}
