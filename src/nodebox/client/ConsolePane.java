package nodebox.client;

import javax.swing.*;
import java.awt.*;

public class ConsolePane extends Pane {

    private PaneHeader paneHeader;
    private Console console;

    public ConsolePane(NodeBoxDocument document) {
        super(document);
        setLayout(new BorderLayout(0, 0));
        paneHeader = new PaneHeader(this);
        console = new Console(this);
        JScrollPane consoleScroll = new JScrollPane(console, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        consoleScroll.setBorder(BorderFactory.createEmptyBorder());
        add(paneHeader, BorderLayout.NORTH);
        add(consoleScroll, BorderLayout.CENTER);
    }

    public String getPaneName() {
        return "Console";
    }

    public PaneHeader getPaneHeader() {
        return paneHeader;
    }

    public PaneView getPaneView() {
        return console;
    }

    public Pane clone() {
        return new ConsolePane(getDocument());
    }
}
