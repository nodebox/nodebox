package nodebox.handle;

/**
 * A Handle Delegate can react to changes requested by a Handle.
 */
public interface HandleDelegate {

    public boolean hasInput(String portName);

    public boolean isConnected(String portName);

    /**
     * Get the value from the port on the node.
     *
     * @param portName the name of the port.
     * @return the value from the port.
     */
    public Object getValue(String portName);

    /**
     * Set a value on the node with given the given port name.
     * <p/>
     * This callback is fired whenever we want to set a value and have an error reported back.
     * This method can be called for every drag or move of the mouse, if needed.
     *
     * @param nodePath The path inside the network of the node the port belongs to.
     * @param portName The port this value is linked to.
     * @param value    The new value.
     */
    public void setValue(String nodePath, String portName, Object value);

    /**
     * Set a value on the node without causing an error.
     * <p/>
     * This callback is fired whenever we want to set a value, but ignore every error.
     * For handles, this is the default, since we don't want to mess with error handling.
     * This automatically does the right thing.
     * <p/>
     * For example, on a constrained handle where width / height need to be equal, calling this method
     * will keep them in sync without raising errors that one can't be bigger than the other.
     *
     * @param portName The port this value is linked to.
     * @param value    The new value.
     */
    public void silentSet(String portName, Object value);

    /**
     * Edits are no longer recorded until you call stopEditing. This allows you to batch edits.
     *
     * @param command the command name of the edit batch
     */
    public void startEdits(String command);

    /**
     * Indicates that the user has stopped an editing operation.
     * <p/>
     * Use this when something significant has happened in your code, e.g. when you've drawn a line in the freehand node.
     * <p/>
     * This causes the undo mechanism to create a new undo "step".
     */
    public void stopEditing();

    /**
     * Indicates that the handle needs to be updated.
     * <p/>
     * This calls the update method on the handle, then repaints it.
     * <p/>
     * The handle is updated every time a value is changed.
     * Use this method whenever you want to update the handle state without changing a value,
     * for example to redraw a handle cursor.
     */
    public void updateHandle();

}
