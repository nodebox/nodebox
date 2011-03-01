package nodebox.client;

import nodebox.node.Node;
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

    private class UndoState {
        private String xml;
        private String activeNetworkPath;
        private String activeNodeName;
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
        UndoState state = new UndoState();
        state.xml = document.getNodeLibrary().toXml();
        state.activeNetworkPath = document.getActiveNetworkPath();
        Node activeNode = document.getActiveNode();
        if (activeNode == null) {
            state.activeNodeName = null;
        } else {
            state.activeNodeName = activeNode.getName();
        }
        return state;
    }

    public void restoreState(UndoState state) {
        NodeLibrary nodeLibrary = NodeLibrary.load(document.getNodeLibrary().getName(), state.xml, document.getManager());
        document.setNodeLibrary(nodeLibrary);
        document.setActiveNetwork(state.activeNetworkPath);
        if (state.activeNodeName != null) {
            Node child = document.getActiveNetwork().getChild(state.activeNodeName);
            if (child != null) {
                document.setActiveNode(child);
            }
        }
    }

}
