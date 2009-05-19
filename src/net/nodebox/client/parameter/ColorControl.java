package net.nodebox.client.parameter;

import net.nodebox.client.ColorDialog;
import net.nodebox.client.DocumentFocusListener;
import net.nodebox.client.NodeBoxDocument;
import net.nodebox.client.SwingUtils;
import net.nodebox.node.ParameterValueListener;
import net.nodebox.node.Parameter;
import net.nodebox.node.Node;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ColorControl extends JComponent implements ChangeListener, ParameterControl, ActionListener, DocumentFocusListener, ParameterValueListener {

    private Parameter parameter;
    //private ColorWell colorWell;
    private ColorButton colorButton;
    private ColorDialog colorDialog;

    public ColorControl(Parameter parameter) {
        setLayout(new FlowLayout(FlowLayout.LEADING));
        this.parameter = parameter;

//        colorWell = new ColorWell();
//        colorWell.setColor(parameter.asColor());
//        colorWell.addChangeListener(this);
//        add(colorWell);
        colorButton = new ColorButton();
        add(colorButton);
        setValueForControl(parameter.getValue());
        parameter.getNode().addParameterValueListener(this);
    }

    public Parameter getParameter() {
        return parameter;
    }

    public void stateChanged(ChangeEvent e) {
        setValueFromControl();
    }

    private void setValueFromControl() {
        parameter.setValue(new net.nodebox.graphics.Color(colorDialog.getColor()));
        //parameter.setValue(colorWell.getColor());
    }

    public void setValueForControl(Object v) {
        colorButton.repaint();
        //colorWell.setColor((net.nodebox.graphics.Color) v);
    }

    public void valueChanged(Parameter source) {
        setValueForControl(source.getValue());
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

    private class ColorSwatch extends JComponent {
        private net.nodebox.graphics.Color color = new net.nodebox.graphics.Color();

        private ColorSwatch(net.nodebox.graphics.Color color) {
            setSize(30, 30);
            setBounds(0, 0, 30, 30);
            setPreferredSize(new Dimension(30, 30));
            this.color = color;
        }

        public net.nodebox.graphics.Color getColor() {
            return color;
        }

        public void setColor(net.nodebox.graphics.Color color) {
            this.color = color;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Rectangle r = g.getClipBounds();
            g.setColor(color.getAwtColor());
            g.fillRect(r.x, r.y, r.width, r.height);
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
