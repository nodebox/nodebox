package nodebox.client;

/**
 * A state that remembers all of the operations done on this document.
 */
public class UndoState {

    private String command;
    private String xml;

    public UndoState(String command, String xml) {
        this.command = command;
        this.xml = xml;
    }

    public String getCommand() {
        return command;
    }

    public String getXml() {
        return xml;
    }
}

