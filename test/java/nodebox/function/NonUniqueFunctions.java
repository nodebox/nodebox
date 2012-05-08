package nodebox.function;

/**
 * A function library containing special functions for testing.
 */
public class NonUniqueFunctions {

    public static final FunctionLibrary LIBRARY;

    static {
        LIBRARY = JavaLibrary.ofClass("non-unique", NonUniqueFunctions.class, "myFunction");
    }

    public static int myFunction(int ignored) {
        return 0;
    }

    public static int myFunction(int ignored1, int ignored2) {
        return 0;
    }
}
