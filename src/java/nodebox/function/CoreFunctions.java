package nodebox.function;

import nodebox.node.NodeContext;

/**
 * Core function library that is always available.
 * <p/>
 * It only contains one function, "zero", that takes no arguments and returns 0.
 */
public class CoreFunctions {

    public static final JavaLibrary LIBRARY;
    public static final Function ZERO;

    static {
        LIBRARY = JavaLibrary.ofClass("core", CoreFunctions.class, "zero", "frame");
        ZERO = LIBRARY.getFunction("zero");
    }

    public static double zero() {
        return 0.0;
    }
    
    public static double frame(NodeContext context) {
        return context.getFrame();
    }

}
