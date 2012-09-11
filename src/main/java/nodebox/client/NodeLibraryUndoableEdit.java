package nodebox.client;

import nodebox.node.NodeLibrary;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * An undoable edit happening to the node library.
 */
public class NodeLibraryUndoableEdit extends AbstractUndoableEdit {

    private NodeBoxDocument document;
    private String command;
    private UndoState undoState, redoState;

    /**
     * The UndoState captures the current state of the document.
     * <p/>
     * Because the NodeLibrary and all objects below it are immutable the UndoState just has to retain a reference
     * to the given NodeLibrary.
     */
    private class UndoState {
        private final NodeLibrary nodeLibrary;
        private final String activeNetworkPath;
        private final String activeNodeName;

        private UndoState(NodeLibrary nodeLibrary, String activeNetworkPath, String activeNodeName) {
            this.nodeLibrary = nodeLibrary;
            this.activeNetworkPath = activeNetworkPath;
            this.activeNodeName = activeNodeName;
        }
    }

    public NodeLibraryUndoableEdit(NodeBoxDocument document, String command) {
        this.document = document;
        this.command = command;
        undoState = saveState();
    }

    @Override
    public String getPresentationName() {
        return command;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();
        if (redoState == null)
            redoState = saveState();
        restoreState(undoState);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();
        restoreState(redoState);
    }

    public UndoState saveState() {
        return new UndoState(document.getNodeLibrary(), document.getActiveNetworkPath(), document.getActiveNodeName());
    }

    public void restoreState(UndoState state) {
        document.restoreState(state.nodeLibrary, state.activeNetworkPath, state.activeNodeName);
    }

}
