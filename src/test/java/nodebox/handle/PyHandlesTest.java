package nodebox.handle;

import nodebox.client.PythonUtils;
import nodebox.function.FunctionLibrary;
import nodebox.function.PythonLibrary;
import nodebox.graphics.GraphicsContext;
import nodebox.graphics.Point;
import org.junit.Test;
import org.python.util.PythonInterpreter;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The handles in the bundled pyhandles module draw and hit-test in screen space, like the Java handles.
 */
public class PyHandlesTest {

    private static final double DELTA = 0.0001;

    private final FunctionLibrary pyvector = PythonLibrary.loadScript("pyvector", "libraries/corevector/pyvector.py");

    private static class StubDelegate implements HandleDelegate {
        public boolean hasInput(String portName) {
            return true;
        }

        public boolean isConnected(String portName) {
            return false;
        }

        public Object getValue(String portName) {
            if (portName.equals("position")) return new Point(10, 20);
            if (portName.equals("distance")) return 50.0;
            return null;
        }

        public void setValue(String nodePath, String portName, Object value) {
        }

        public void silentSet(String portName, Object value) {
        }

        public void startEdits(String command) {
        }

        public void stopEditing() {
        }

        public void updateHandle() {
        }
    }

    /**
     * A graphics context that records the arguments of every line() call and ignores everything else.
     */
    private static GraphicsContext recordLines(final List<double[]> lines) {
        return (GraphicsContext) Proxy.newProxyInstance(GraphicsContext.class.getClassLoader(),
                new Class[]{GraphicsContext.class}, new InvocationHandler() {
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        if (method.getName().equals("line")) {
                            lines.add(new double[]{(Double) args[0], (Double) args[1], (Double) args[2], (Double) args[3]});
                        }
                        Class<?> type = method.getReturnType();
                        if (type == double.class) return 0.0;
                        if (type == boolean.class) return false;
                        return null;
                    }
                });
    }

    private Handle snapHandle() throws Exception {
        Handle handle = (Handle) pyvector.getFunction("handle_snap").invoke();
        handle.setHandleDelegate(new StubDelegate());
        handle.setViewTransform(100, 200, 2);
        return handle;
    }

    @Test
    public void snapHandleDrawsGridInScreenSpace() throws Exception {
        List<double[]> lines = new ArrayList<double[]>();
        snapHandle().draw(recordLines(lines));

        // The first vertical grid line is at document x = -10 + (-100 * 50), from y = -1000 to 1000.
        double[] first = lines.get(0);
        assertEquals(100 + 2 * -5010, first[0], DELTA);
        assertEquals(200 + 2 * -1000, first[1], DELTA);
        assertEquals(100 + 2 * -5010, first[2], DELTA);
        assertEquals(200 + 2 * 1000, first[3], DELTA);
    }

    @Test
    public void scriptHandleWithoutViewTransformStillWorks() {
        // A handle written against the 3.1 interface, which has no setViewTransform.
        PythonUtils.initializePython();
        PythonInterpreter interpreter = new PythonInterpreter();
        interpreter.exec("from nodebox.handle import Handle\n" +
                "class OldHandle(Handle):\n" +
                "    def draw(self, ctx):\n" +
                "        pass\n" +
                "handle = OldHandle()\n");
        Handle handle = (Handle) interpreter.get("handle").__tojava__(Handle.class);
        handle.setViewTransform(100, 200, 2);
    }

    @Test
    public void snapHandleCanBeGrabbedAnywhereOnTheGrid() throws Exception {
        assertTrue(snapHandle().mousePressed(new Point(500, -700)));
    }
}
