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
