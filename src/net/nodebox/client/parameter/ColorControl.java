package net.nodebox.client.parameter;

import net.nodebox.graphics.Color;
import net.nodebox.node.Parameter;
import net.nodebox.node.ParameterDataListener;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ColorControl extends JComponent implements ChangeListener, ActionListener, ParameterControl, ParameterDataListener {

    private class ColorWell extends JComponent {
        private Color color = new Color();

        public ColorWell() {
            setPreferredSize(new Dimension(40, 30));
        }

        @Override
        public void paint(Graphics g) {
            g.setColor(color.getAwtColor());
            Rectangle r = g.getClipBounds();
            g.fillRect(r.x, r.y, r.width, r.height);
        }

        public Color getColor() {
            return color;
        }

        public void setColor(Color color) {
            this.color = color;
            repaint();
        }
    }

    private Parameter parameter;
    private ColorWell colorWell;

    public ColorControl(Parameter parameter) {
        setLayout(new FlowLayout(FlowLayout.LEADING));
        this.parameter = parameter;
        colorWell = new ColorWell();
        add(colorWell);
        colorWell.setColor(parameter.asColor());
    }

    public void stateChanged(ChangeEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void actionPerformed(ActionEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Parameter getParameter() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setValueForControl(Object v) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void valueChanged(Parameter source, Object newValue) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
