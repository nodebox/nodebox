package nodebox.function;

/**
 * Abstract base class to test inherited methods.
 */
public abstract class AbstractTestFunctions {

    public static String baseReverse(String s) {
        return new StringBuffer(s).reverse().toString();
    }

}
