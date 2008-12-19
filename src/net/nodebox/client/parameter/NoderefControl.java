package net.nodebox.client.parameter;

import net.nodebox.client.PathDialog;
import net.nodebox.client.PlatformUtils;
import net.nodebox.node.Parameter;
import net.nodebox.node.ParameterDataListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class NoderefControl extends JComponent implements ParameterControl, ActionListener, ParameterDataListener {

    private Parameter parameter;
    private JTextField pathField;
    private JButton chooseButton;

    public NoderefControl(Parameter parameter) {
        this.parameter = parameter;
        setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        pathField = new JTextField();
        pathField.putClientProperty("Jcomponent.sizeVariant", "small");
        pathField.setPreferredSize(new Dimension(150, 19));
        pathField.setEditable(false);
        pathField.setFont(PlatformUtils.getSmallFont());
        chooseButton = new JButton("...");
        chooseButton.putClientProperty("JButton.buttonType", "gradient");
        chooseButton.setPreferredSize(new Dimension(30, 27));
        chooseButton.addActionListener(this);
        add(pathField);
        add(chooseButton);
        setValueForControl(parameter.getValue());
        parameter.addDataListener(this);
    }

    public Parameter getParameter() {
        return parameter;
    }

    public void setValueForControl(Object v) {
        pathField.setText(v.toString());
    }

    public void actionPerformed(ActionEvent e) {
        String newPath = PathDialog.choosePath(parameter.getNode().getRootNetwork(), parameter.asString());
        parameter.setValue(newPath);
    }

    public void valueChanged(Parameter source, Object newValue) {
        setValueForControl(newValue);
    }
}
