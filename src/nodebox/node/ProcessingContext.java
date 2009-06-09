package nodebox.node;

import java.util.HashMap;
import java.util.Map;

/**
 * The processing context contains metadata about the processing operation.
 * <p/>
 * Note: the context is currently empty. Later on, we will add the frame number etc.
 */
public class ProcessingContext extends HashMap<String, Object> {

    private enum State {
        UPDATING, PROCESSED
    }

    /**
     * A list of parameters that have already been started updating.
     * This set is used to detect cycles.
     */
    private Map<Parameter, State> updatedParameters = new HashMap<Parameter, State>();

    public ProcessingContext() {
        put("FRAME", 1);
    }

    public int getFrame() {
        return (Integer) get("FRAME");
    }

    public void beginUpdating(Parameter parameter) {
        State state = updatedParameters.get(parameter);
        if (state == null) {
            updatedParameters.put(parameter, State.UPDATING);
        } else if (state == State.UPDATING) {
            throw new AssertionError("You should check beforehand if a parameter was updating.");
        } else if (state == State.PROCESSED) {
            throw new RuntimeException("Parameter " + parameter + " has already been processed: infinite recursion detected.");
        }
    }

    public void endUpdating(Parameter parameter) {
        State state = updatedParameters.get(parameter);
        if (state == null) {
            throw new AssertionError("You should have called beginUpdating first.");
        } else if (state == State.UPDATING) {
            updatedParameters.put(parameter, State.PROCESSED);
        } else if (state == State.PROCESSED) {
            throw new AssertionError("You should only call endUpdating once.");
        }
    }

    public boolean isUpdating(Parameter p) {
        return updatedParameters.get(p) == State.UPDATING;
    }

    public boolean hasProcessed(Parameter p) {
        return updatedParameters.get(p) == State.PROCESSED;
    }
}
