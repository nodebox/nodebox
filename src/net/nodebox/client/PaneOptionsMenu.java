package net.nodebox.client;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class PaneOptionsMenu extends JPopupMenu {

    private Pane pane;
    private Action splitTopBottomAction;
    private Action splitLeftRightAction;
    private Action closePaneAction;

    public PaneOptionsMenu(Pane pane) {
        this.pane = pane;
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
