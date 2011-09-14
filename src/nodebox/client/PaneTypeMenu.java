package nodebox.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

public class PaneTypeMenu extends PaneMenu {

    private PaneTypePopup paneTypePopup;

    public PaneTypeMenu(Pane pane) {
        super(pane);
        paneTypePopup = new PaneTypePopup();
        setEnabled(Application.ENABLE_PANE_CUSTOMIZATION);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        Rectangle bounds = getBounds();
//        paneTypePopup.show(this, bounds.x, bounds.y + bounds.height - 4);
        paneTypePopup.show(this, 5, bounds.y + bounds.height - 4);
    }

    @Override
    public String getMenuName() {
        return getPane().getPaneName();
    }

    private class PaneTypePopup extends JPopupMenu {
        public PaneTypePopup() {
            add(new ChangePaneTypeAction("Network", NetworkPane.class));
            add(new ChangePaneTypeAction("Parameters", ParameterPane.class));
            add(new ChangePaneTypeAction("Viewer", ViewerPane.class));
            add(new ChangePaneTypeAction("Source", EditorPane.class));
            add(new ChangePaneTypeAction("Console", ConsolePane.class));
        }
    }

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
}
