package nodebox.client.parameter;

import nodebox.client.ColorDialog;
import nodebox.client.NodeBoxDocument;
import nodebox.client.SwingUtils;
import nodebox.client.Theme;
import nodebox.node.Parameter;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ColorControl extends AbstractParameterControl implements ChangeListener, ActionListener {

    private ColorButton colorButton;
    private ColorDialog colorDialog;

    public ColorControl(Parameter parameter) {
        super(parameter);
        setLayout(new FlowLayout(FlowLayout.LEADING, 1, 0));
        colorButton = new ColorButton();
        colorButton.setPreferredSize(new Dimension(40, 19));
        add(colorButton);
        setValueForControl(parameter.getValue());
        setPreferredSize(new Dimension(80, 30));
    }

    public void stateChanged(ChangeEvent e) {
        setValueFromControl();
    }

    private void setValueFromControl() {
        setParameterValue(new nodebox.graphics.Color(colorDialog.getColor()));
    }

    public void setValueForControl(Object v) {
        colorButton.repaint();
        //colorWell.setColor((nodebox.graphics.Color) v);
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
//        final ColorSwatch colorSwatch = new ColorSwatch(parameter.asColor());

        // Install a listener that listens for active node changed events, so we can dispose of the color dialog.
        // TODO: Find a better way to do this. Maybe add color dialogs to the document itself?
        Component component = getParent();
        while (!(component instanceof NodeBoxDocument)) {
            component = component.getParent();
        }

        if (colorDialog == null) {
            colorDialog = new ColorDialog((Frame) SwingUtilities.getWindowAncestor(this));
            colorDialog.setColor(parameter.asColor().getAwtColor());
            int height = colorDialog.getHeight();
            colorDialog.setMinimumSize(new Dimension(400, height));
            colorDialog.setPreferredSize(new Dimension(540, height));
            colorDialog.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
            colorDialog.setSize(540, 375);
            colorDialog.addChangeListener(this);
            colorDialog.setAlwaysOnTop(true);
            SwingUtils.centerOnScreen(colorDialog);
            colorDialog.setVisible(true);
            colorDialog.pack();
        } else {
            colorDialog.setVisible(true);
            colorDialog.requestFocus();
        }
    }

    private class ColorButton extends JButton {
        private ColorButton() {
            addActionListener(ColorControl.this);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Rectangle r = g.getClipBounds();
            if (ColorControl.this.isEnabled()) {
                g.setColor(Color.darkGray);
            } else {
                g.setColor(Theme.PARAMETER_LABEL_BACKGROUND);
            }
            g.fillRect(r.x, r.y, r.width - 1, r.height - 1);
            if (ColorControl.this.isEnabled()) {
                r.grow(1, 1);
            } else {
                r.grow(-5, -5);
            }
            g.setColor(parameter.asColor().getAwtColor());
            g.fillRect(r.x, r.y, r.width - 1, r.height - 1);
        }
    }
}
