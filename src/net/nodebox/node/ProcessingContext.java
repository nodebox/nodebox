package net.nodebox.node;

import java.util.HashMap;

/**
 * The processing context contains metadata about the processing operation.
 * <p/>
 * Note: the context is currently empty. Later on, we will add the frame number etc.
 */
public class ProcessingContext extends HashMap<String, Object> {

    public ProcessingContext() {
        put("FRAME", 1);
    }

    public int getFrame() {
        return (Integer) get("FRAME");
    }

}
