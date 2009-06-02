package net.nodebox.client.parameter;

import net.nodebox.client.NButton;
import net.nodebox.node.Parameter;
import net.nodebox.node.ParameterValueListener;

import javax.swing.*;
import java.awt.*;

public class ToggleControl extends JComponent implements ParameterControl, ParameterValueListener {

    private Parameter parameter;
    //private JCheckBox checkBox;
    private NButton checkBox;

    public ToggleControl(Parameter parameter) {
        this.parameter = parameter;
        setLayout(new FlowLayout(FlowLayout.LEADING));
        checkBox = new NButton(NButton.Mode.CHECK, parameter.getLabel());
        checkBox.setActionMethod(this, "toggle");
//        checkBox = new JCheckBox(parameter.getLabel());
//        checkBox.putClientProperty("JComponent.sizeVariant", "small");
//        checkBox.setOpaque(false);
//        checkBox.setPreferredSize(new Dimension(150, 18));
//        checkBox.setFont(PlatformUtils.getSmallFont());
        //checkBox.addActionListener(this);
        add(checkBox);
        setValueForControl(parameter.getValue());
        parameter.getNode().addParameterValueListener(this);
    }

    public Parameter getParameter() {
        return parameter;
    }

    public void setValueForControl(Object v) {
        if (v == null) return;
        int value = (Integer) v;
        checkBox.setChecked(value == 1);
        //checkBox.setSelected(value == 1);
    }

    public void toggle() {
        parameter.set(checkBox.isChecked() ? 1 : 0);

    }

    public void valueChanged(Parameter source) {
        setValueForControl(source.getValue());
    }
}
