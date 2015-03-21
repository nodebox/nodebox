package nodebox.client.port;

import nodebox.client.NodeBoxDocument;
import nodebox.node.Port;
import nodebox.ui.ColorDialog;
import nodebox.ui.NButton;
import nodebox.ui.Theme;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ColorControl extends AbstractPortControl implements ChangeListener, ActionListener {

    private ColorButton colorButton;
    private ColorDialog colorDialog;
    private NButton alphaCheck;

    public ColorControl(String nodePath, Port port) {
        super(nodePath, port);
        setLayout(new FlowLayout(FlowLayout.LEADING, 1, 0));
        alphaCheck = new NButton(NButton.Mode.CHECK, "");
        alphaCheck.setActionMethod(this, "setTransparent");
        add(alphaCheck);
        colorButton = new ColorButton();
        colorButton.setPreferredSize(new Dimension(40, 19));
        colorButton.setFocusable(false);
        add(colorButton);
        setValueForControl(port.getValue());
        setPreferredSize(new Dimension(80, 30));
    }

    public void stateChanged(ChangeEvent e) {
        setValueFromControl();
    }

    private void setValueFromControl() {
        setPortValue(new nodebox.graphics.Color(colorDialog.getColor()));
    }

    public void setValueForControl(Object v) {
        colorButton.color = ((nodebox.graphics.Color) v).getAwtColor();
        colorButton.repaint();
        if (! ((nodebox.graphics.Color) v).isVisible() && ! alphaCheck.isChecked()) {
            alphaCheck.setChecked(true);
            alphaCheck.setEnabled(false);
        }
        else if (((nodebox.graphics.Color) v).isVisible() && alphaCheck.isChecked()) {
            alphaCheck.setChecked(false);
            alphaCheck.setEnabled(true);
        }
    }

    public void setTransparent() {
        if (alphaCheck.isEnabled()) {
            nodebox.graphics.Color transparent = new nodebox.graphics.Color(0, 0, 0, 0);
            setValueForControl(transparent);
            setPortValue(transparent);
            alphaCheck.setEnabled(false);
            if (colorDialog != null)
                colorDialog.setColor(transparent.getAwtColor());
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        colorButton.setEnabled(enabled);
        if (!enabled && colorDialog != null)
            colorDialog.dispose();
    }

    public void actionPerformed(ActionEvent e) {
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        final JDialog d = new JDialog(frame, "Choose Color", true);
        d.getContentPane().setLayout(new BorderLayout(10, 10));
//        final ColorSwatch colorSwatch = new ColorSwatch(port.asColor());

        // Install a listener that listens for active node changed events, so we can dispose of the color dialog.
        // TODO: Find a better way to do this. Maybe add color dialogs to the document itself?
        Component component = getParent();
        while (!(component instanceof NodeBoxDocument)) {
            component = component.getParent();
        }

        if (colorDialog == null) {
            colorDialog = new ColorDialog((Frame) SwingUtilities.getWindowAncestor(this));
            int height = colorDialog.getHeight();
            colorDialog.setMinimumSize(new Dimension(400, height));
            colorDialog.setPreferredSize(new Dimension(540, height));
            colorDialog.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
            colorDialog.setSize(540, 375);
            colorDialog.addChangeListener(this);
            colorDialog.setAlwaysOnTop(true);
            colorDialog.setLocationRelativeTo(null);
            colorDialog.setVisible(true);
            colorDialog.pack();
        } else {
            colorDialog.setVisible(true);
            colorDialog.requestFocus();
        }
        colorDialog.setColor(colorButton.color);
    }

    private class ColorButton extends JButton {
        Color color = Color.BLACK;

        private ColorButton() {
            addActionListener(ColorControl.this);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Rectangle r = g.getClipBounds();
            if (ColorControl.this.isEnabled()) {
                g.setColor(Color.darkGray);
            } else {
                g.setColor(Theme.PORT_LABEL_BACKGROUND);
            }
            g.fillRect(r.x, r.y, r.width - 1, r.height - 1);
            if (ColorControl.this.isEnabled()) {
                r.grow(1, 1);
            } else {
                r.grow(-5, -5);
            }
            g.setColor(color);
            g.fillRect(r.x, r.y, r.width - 1, r.height - 1);
        }
    }

}
