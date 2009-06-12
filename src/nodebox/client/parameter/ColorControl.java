package nodebox.client.parameter;

import nodebox.client.ColorDialog;
import nodebox.client.DocumentFocusListener;
import nodebox.client.NodeBoxDocument;
import nodebox.client.SwingUtils;
import nodebox.node.Node;
import nodebox.node.Parameter;
import nodebox.node.ParameterValueListener;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ContainerEvent;

public class ColorControl extends AbstractParameterControl implements ChangeListener, ActionListener, DocumentFocusListener {

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
        parameter.setValue(new nodebox.graphics.Color(colorDialog.getColor()));
        //parameter.setValue(colorWell.getColor());
    }

    public void setValueForControl(Object v) {
        colorButton.repaint();
        //colorWell.setColor((nodebox.graphics.Color) v);
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
        NodeBoxDocument doc = (NodeBoxDocument) component;
        doc.addDocumentFocusListener(this);

        if (colorDialog == null) {
            colorDialog = new ColorDialog((Frame) SwingUtilities.getWindowAncestor(this));
            colorDialog.setColor(parameter.asColor().getAwtColor());
            colorDialog.setSize(500, 340);
            colorDialog.addChangeListener(this);
            colorDialog.setAlwaysOnTop(true);
            SwingUtils.centerOnScreen(colorDialog);
            colorDialog.setVisible(true);
        } else {
            colorDialog.setVisible(true);
            colorDialog.requestFocus();
        }
    }

    public void currentNodeChanged(Node node) {
    }

    public void focusedNodeChanged(Node node) {
        if (colorDialog != null) {
            colorDialog.dispose();
        }
    }

    private class ColorButton extends JButton {
        private ColorButton() {
            addActionListener(ColorControl.this);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Rectangle r = g.getClipBounds();
            g.setColor(parameter.asColor().getAwtColor());
            g.fillRect(r.x, r.y, r.width, r.height);
            g.setColor(Color.darkGray);
            g.drawRect(r.x, r.y, r.width - 1, r.height - 1);
        }
    }
}
