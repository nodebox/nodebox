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
    private String undoXml, redoXml;

    public NodeLibraryUndoableEdit(NodeBoxDocument document, String command) {
        this.document = document;
        this.command = command;
        undoXml = saveState();
    }

    @Override
    public String getPresentationName() {
        return command;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();
        if (redoXml == null)
            redoXml = saveState();
        restoreState(undoXml);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();
        restoreState(redoXml);
    }

    public String saveState() {
        return document.getNodeLibrary().toXml();
    }

    public void restoreState(String xml) {
        NodeLibrary nodeLibrary = NodeLibrary.load(document.getNodeLibrary().getName(), xml, document.getManager());
        document.setNodeLibrary(nodeLibrary);
    }

}
