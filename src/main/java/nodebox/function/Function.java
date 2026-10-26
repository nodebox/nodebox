package nodebox.function;

import com.google.common.collect.ImmutableList;

/**
 * Function wraps any kind of callable.
 */
public interface Function {

    /**
     * Get the function name.
     *
     * @return The function name.
     */
    public String getName();


    /**
     * Invoke the function and return the result.
     *
     * @param args The list of arguments.
     * @return The result of evaluating the function.
     * @throws Exception The invocation exception.
     */
    public Object invoke(Object... args) throws Exception;

    public ImmutableList<Argument> getArguments();

    /**
     * Whether the result can change between renders even when the arguments are the same, because the
     * function reads the clock, the network or a device, or acts on the outside world. A time-dependent
     * function runs on every render; any other function is cached until its inputs change.
     * <p/>
     * Java functions declare this with {@link TimeDependent}, Python functions with a
     * {@code timeDependent = True} attribute and Clojure functions with {@code ^:time-dependent} metadata.
     */
    public default boolean isTimeDependent() {
        return false;
    }

    public static final class Argument {

        public String name;
        public String type;

        public Argument(String name, String type) {
            this.name = name;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }
    }
}
