package nodebox.function;

import org.junit.Test;

import java.lang.reflect.Method;

import static junit.framework.TestCase.*;

public class FunctionsTest {

    @Test
    public void testInheritedMethods() {
        Method m = Functions.findMethod(TestFunctions.class, "baseReverse");
        assertEquals("baseReverse", m.getName());
        assertEquals(AbstractTestFunctions.class, m.getDeclaringClass());
    }

}
