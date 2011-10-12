package nodebox.client;

import java.awt.*;
import java.awt.event.FocusListener;

public class ConsolePane extends Pane implements FocusListener {

    private PaneHeader paneHeader;
    private Console console;

    public ConsolePane() {
        setLayout(new BorderLayout(0, 0));
        paneHeader = new PaneHeader(this);
        console = new Console(this);
        add(paneHeader, BorderLayout.NORTH);
        add(console, BorderLayout.CENTER);
        setMainComponent(console);
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

    public Pane duplicate() {
        return new ConsolePane();
    }

}
