package nodebox.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;


public class PaneCodeMenu extends PaneMenu {

    private PaneCodePopup paneCodePopup;
    private String menuName = "Code";

    public PaneCodeMenu() {
        paneCodePopup = new PaneCodePopup();
    }

    @Override
    public String getMenuName() {
        return menuName;
    }

    private void setMenuName(String name) {
        menuName = name;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        Rectangle bounds = getBounds();
        paneCodePopup.show(this, 5, bounds.y + bounds.height - 4);
    }

    private class PaneCodePopup extends JPopupMenu {
        public PaneCodePopup() {
            add(new ChangePaneCodeAction("Code", "_code"));
            add(new ChangePaneCodeAction("Handle", "_handle"));
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
            setMenuName(codeName);
            fireActionEvent(codeType);
        }
    }

}
