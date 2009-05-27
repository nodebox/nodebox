package net.nodebox.client;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class PaneOptionsMenu extends JPopupMenu {

    private Pane pane;
    private Action splitTopBottomAction;
    private Action splitLeftRightAction;
    private Action closePaneAction;

    private class ChangePaneTypeAction extends AbstractAction {

        private Class paneType;

        private ChangePaneTypeAction(String name, Class paneType) {
            super(name);
            this.paneType = paneType;
        }

        public void actionPerformed(ActionEvent e) {
            getPane().changePaneType(paneType);

        }
    }

    public PaneOptionsMenu(Pane pane) {
        this.pane = pane;
        add(new ChangePaneTypeAction("Network", NetworkPane.class));
        add(new ChangePaneTypeAction("Parameters", ParameterPane.class));
        add(new ChangePaneTypeAction("Viewer", ViewerPane.class));
        add(new ChangePaneTypeAction("Source", EditorPane.class));
        add(new ChangePaneTypeAction("Console", ConsolePane.class));
        add(new ChangePaneTypeAction("Log", LoggingPane.class));
        addSeparator();
        splitTopBottomAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                getPane().splitTopBottom();
            }
        };
        splitLeftRightAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                getPane().splitLeftRight();
            }
        };
        closePaneAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                getPane().close();
            }
        };
        splitTopBottomAction.putValue(Action.NAME, "Split top/bottom");
        splitLeftRightAction.putValue(Action.NAME, "Split left/right");
        closePaneAction.putValue(Action.NAME, "Close pane");
        add(splitTopBottomAction);
        add(splitLeftRightAction);
        add(closePaneAction);
    }

    public Pane getPane() {
        return pane;
    }

}
