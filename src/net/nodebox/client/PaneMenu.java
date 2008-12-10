package net.nodebox.client;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class PaneMenu extends JPopupMenu {

    private Pane pane;
    private Action splitTopBottomAction;
    private Action splitLeftRightAction;

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

    public PaneMenu(Pane pane) {
        this.pane = pane;
        add(new ChangePaneTypeAction("Network", NetworkPane.class));
        add(new ChangePaneTypeAction("Parameters", ParameterPane.class));
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
        splitTopBottomAction.putValue(Action.NAME, "Split top/bottom");
        splitLeftRightAction.putValue(Action.NAME, "Split left/right");
        add(splitTopBottomAction);
        add(splitLeftRightAction);
    }

    public Pane getPane() {
        return pane;
    }

}
