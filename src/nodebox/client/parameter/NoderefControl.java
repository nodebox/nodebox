package nodebox.client.parameter;

import nodebox.client.PathDialog;
import nodebox.client.PlatformUtils;
import nodebox.node.Parameter;
import nodebox.node.ParameterValueListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ContainerEvent;

public class NoderefControl extends AbstractParameterControl implements ActionListener {

    private JTextField pathField;

    public NoderefControl(Parameter parameter) {
        super(parameter);
        setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        pathField = new JTextField();
        pathField.putClientProperty("Jcomponent.sizeVariant", "small");
        pathField.setPreferredSize(new Dimension(150, 19));
        pathField.setEditable(false);
        pathField.setFont(PlatformUtils.getSmallFont());
        JButton chooseButton = new JButton("...");
        chooseButton.putClientProperty("JButton.buttonType", "gradient");
        chooseButton.setPreferredSize(new Dimension(30, 27));
        chooseButton.addActionListener(this);
        add(pathField);
        add(chooseButton);
        setValueForControl(parameter.getValue());
    }

    public void setValueForControl(Object v) {
        pathField.setText(v.toString());
    }

    public void actionPerformed(ActionEvent e) {
        String newPath = PathDialog.choosePath(parameter.getNode().getRoot(), parameter.asString());
        parameter.setValue(newPath);
    }
}
