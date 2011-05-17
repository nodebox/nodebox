package nodebox.node;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The processing context contains metadata about the processing operation.
 * <p/>
 * Note: the context is currently empty. Later on, we will add the frame number etc.
 */
public class ProcessingContext {

    public static final String FRAME = "FRAME";
    public static final String TOP = "TOP";
    public static final String LEFT = "LEFT";
    public static final String BOTTOM = "BOTTOM";
    public static final String RIGHT = "RIGHT";
    public static final String WIDTH = "WIDTH";
    public static final String HEIGHT = "HEIGHT";

    private static ThreadLocal<ProcessingContext> currentContext = new ThreadLocal<ProcessingContext>();

    static void setCurrentContext(ProcessingContext context) {
        currentContext.set(context);
    }

    public static ProcessingContext getCurrentContext() {
        return currentContext.get();
    }

    private HashMap<String, Object> valueMap = new HashMap<String, Object>();
    private ByteArrayOutputStream outputBytes;
    private ByteArrayOutputStream errorBytes;
    private PrintStream outputStream;
    private PrintStream errorStream;
    private Node node;

    private enum State {
        UPDATING, PROCESSED
    }

    /**
     * A list of parameters that have already been started updating.
     * This set is used to detect cycles.
     */
    private Map<Parameter, State> updatedParameters = new HashMap<Parameter, State>();

    public ProcessingContext() {
        put(FRAME, 1f);
        putBounds(0f, 0f, 1000f, 1000f);
        outputBytes = new ByteArrayOutputStream();
        outputStream = new PrintStream(outputBytes);
        errorBytes = new ByteArrayOutputStream();
        errorStream = new PrintStream(errorBytes);
    }

    public ProcessingContext(Node node) {
        this();
        this.node = node;
        float frame = 1f;
        float canvasX = 0f;
        float canvasY = 0f;
        float canvasWidth = 1000f;
        float canvasHeight = 1000f;
        if (node != null) {
            frame = node.getLibrary().getFrame();
            Node root = node.getLibrary().getRootNode();
            canvasX = root.asFloat(NodeLibrary.CANVAS_X);
            canvasY = root.asFloat(NodeLibrary.CANVAS_Y);
            canvasWidth = root.asFloat(NodeLibrary.CANVAS_WIDTH);
            canvasHeight = root.asFloat(NodeLibrary.CANVAS_HEIGHT);
        }
        put(FRAME, frame);
        putBounds(canvasX, canvasY, canvasWidth, canvasHeight);
    }

    private void putBounds(float x, float y, float width, float height) {
        put(WIDTH, width);
        put(HEIGHT, height);
        put(TOP, y - height / 2);
        put(LEFT, x - width / 2);
        put(BOTTOM, y + height / 2);
        put(RIGHT, x + width / 2);
    }

    //// Current node ////

    /**
     * Get the node that is currently processing.
     *
     * @return the current node.
     */
    public Node getNode() {
        return node;
    }

    void setNode(Node node) {
        this.node = node;
    }

    //// Map operations ////

    public void put(String key, Object value) {
        valueMap.put(key, value);
    }

    public Object get(String key) {
        return valueMap.get(key);
    }

    public boolean containsKey(String key) {
        return valueMap.containsKey(key);
    }

    public Set<String> keySet() {
        return valueMap.keySet();
    }

    //// Map shortcuts ////

    public float getFrame() {
        return (Float) get("FRAME");
    }

    //// Output/error streams  ////

    public PrintStream getOutputStream() {
        return outputStream;
    }

    public PrintStream getErrorStream() {
        return errorStream;
    }

    public String getOutput() {
        return outputBytes.toString();
    }

    public String getError() {
        return errorBytes.toString();
    }

    // TODO: These are no longer used. Check and remove.

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
