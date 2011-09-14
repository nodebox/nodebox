package nodebox.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;


public class PaneCodeMenu extends PaneMenu {
    private PaneCodePopup paneCodePopup;

    public PaneCodeMenu(Pane pane) {
        super(pane);
        paneCodePopup = new PaneCodePopup();
    }

    @Override
    public String getMenuName() {
        // TODO Implement
        return "Code";
    }

    @Override
    public void mousePressed(MouseEvent e) {
        Rectangle bounds = getBounds();
//        paneTypePopup.show(this, bounds.x, bounds.y + bounds.height - 4);
        paneCodePopup.show(this, 5, bounds.y + bounds.height - 4);
    }

    private class PaneCodePopup extends JPopupMenu {
        public PaneCodePopup() {
            add(new ChangePaneCodeAction("Code", "_code"));
            add(new ChangePaneCodeAction("Handle", "_code"));
            // ((EditorPane) getPane()).onCodeParameterChanged("Code", "_code");
        }
    }

    private class ChangePaneCodeAction extends AbstractAction {

        private String codeName;
        private String codeType;

        private ChangePaneCodeAction(String name, String codeType) {
            super(name);
            this.codeName = name;
            this.codeType = codeType;
        }

        public void actionPerformed(ActionEvent e) {
            ((EditorPane) getPane()).onCodeParameterChanged(codeType);
        }
    }
}
