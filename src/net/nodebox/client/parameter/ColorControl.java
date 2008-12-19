package net.nodebox.client.parameter;

import net.nodebox.client.ColorWell;
import net.nodebox.node.Parameter;
import net.nodebox.node.ParameterDataListener;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

public class ColorControl extends JComponent implements ChangeListener, ParameterControl, ParameterDataListener {

    private Parameter parameter;
    private ColorWell colorWell;

    public ColorControl(Parameter parameter) {
        setLayout(new FlowLayout(FlowLayout.LEADING));
        this.parameter = parameter;
        colorWell = new ColorWell();
        colorWell.setColor(parameter.asColor());
        colorWell.addChangeListener(this);
        add(colorWell);
        setValueForControl(parameter.getValue());
        parameter.addDataListener(this);
    }

    public Parameter getParameter() {
        return parameter;
    }

    public void stateChanged(ChangeEvent e) {
        setValueFromControl();
    }

    private void setValueFromControl() {
        parameter.setValue(colorWell.getColor());
    }

    public void setValueForControl(Object v) {
        colorWell.setColor((net.nodebox.graphics.Color) v);
    }

    public void valueChanged(Parameter source, Object newValue) {
        setValueForControl(newValue);
    }

}
