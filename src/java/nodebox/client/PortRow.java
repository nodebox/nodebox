package nodebox.client;

import nodebox.node.Port;
import nodebox.ui.ShadowLabel;
import nodebox.ui.Theme;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;

public class PortRow extends JComponent implements MouseListener, ActionListener {

    private static Image popupButtonImage;

    static {
        try {
            popupButtonImage = ImageIO.read(new File("res/options-button.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private NodeBoxDocument document;
    private Port port;
    private JLabel label;
    private JComponent control;
    private JPopupMenu popupMenu;

    private static final int TOP_PADDING = 2;
    private static final int BOTTOM_PADDING = 2;

    public PortRow(NodeBoxDocument document, Port port, JComponent control) {
        this.document = document;
        this.port = port;
        addMouseListener(this);

        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

        Dimension labelSize = new Dimension(PortView.LABEL_WIDTH, 16);

        label = new ShadowLabel(port.getLabel());
        label.setToolTipText(port.getName());
        label.setBorder(null);
        label.setPreferredSize(labelSize);
        label.setMinimumSize(labelSize);

        this.control = control;
        control.setBorder(BorderFactory.createEmptyBorder(TOP_PADDING, 0, BOTTOM_PADDING, 0));

        popupMenu = new JPopupMenu();
        popupMenu.add(new RevertToDefaultAction());

        add(this.label);
        add(Box.createHorizontalStrut(10));
        add(this.control);
        add(Box.createHorizontalGlue());
        // Compensate for the popup button.
        add(Box.createHorizontalStrut(30));
        setBorder(Theme.PARAMETER_ROW_BORDER);
    }

    public Port getPort() {
        return port;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(400, control.getPreferredSize().height + TOP_PADDING + BOTTOM_PADDING);
    }

    //// Mouse listeners ////

    public void mouseClicked(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        if (e.getX() < this.getWidth() - 20) return;
        popupMenu.show(this, this.getWidth() - 20, 20);
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    @Override
    protected void paintComponent(Graphics g) {
        // Height aligns to 30px high control, such as float, string, color, etc.
        g.drawImage(popupButtonImage, getWidth() - 20, 4, null);

    }

    //// Port context menu ////

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        control.setEnabled(enabled);
        label.setEnabled(enabled);
    }

    /**
     * A user clicked the expression editor button. Show the expression window.
     *
     * @param e the event
     */
    public void actionPerformed(ActionEvent e) {
//        NodeBoxDocument doc = NodeBoxDocument.getCurrentDocument();
//        if (doc == null) throw new RuntimeException("No current active document.");
//        ExpressionWindow window = new ExpressionWindow(port);
//        window.setLocationRelativeTo(this);
//        window.setVisible(true);
//        doc.addPortEditor(window);
    }

    //// Action classes ////

    private class RevertToDefaultAction extends AbstractAction {
        private RevertToDefaultAction() {
            putValue(Action.NAME, "Revert to Default");
        }

        public void actionPerformed(ActionEvent e) {
            document.revertPortToDefault(port);
            // Reverting to default could cause an expression to be set/cleared.
            // This triggers an valueChanged event, where we check if our expression field is
            // still up-to-date.
        }
    }

}
