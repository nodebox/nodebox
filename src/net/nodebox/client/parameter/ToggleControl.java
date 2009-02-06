package net.nodebox.client.parameter;

import net.nodebox.client.PlatformUtils;
import net.nodebox.node.Parameter;
import net.nodebox.node.ParameterDataListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ToggleControl extends JComponent implements ParameterControl, ActionListener, ParameterDataListener {

    private Parameter parameter;
    private JCheckBox checkBox;

    public ToggleControl(Parameter parameter) {
        this.parameter = parameter;
        setLayout(new FlowLayout(FlowLayout.LEADING));
        checkBox = new JCheckBox(parameter.getLabel());
        checkBox.putClientProperty("JComponent.sizeVariant", "small");
        checkBox.setOpaque(false);
        checkBox.setPreferredSize(new Dimension(150, 18));
        checkBox.setFont(PlatformUtils.getSmallFont());
        checkBox.addActionListener(this);
        add(checkBox);
        setValueForControl(parameter.getValue());
        parameter.addDataListener(this);
    }

    public Parameter getParameter() {
        return parameter;
    }

    public void setValueForControl(Object v) {
        if (v == null) return;
        int value = (Integer) v;
        checkBox.setSelected(value == 1);
    }

    public void actionPerformed(ActionEvent e) {
        parameter.set(checkBox.isSelected() ? 1 : 0);
    }

    public void valueChanged(Parameter source, Object newValue) {
        setValueForControl(newValue);
    }
}
