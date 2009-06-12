package nodebox.client.parameter;

import nodebox.client.FileUtils;
import nodebox.client.PlatformUtils;
import nodebox.node.Parameter;
import nodebox.node.ParameterValueListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ContainerEvent;
import java.io.File;

public class FileControl extends AbstractParameterControl implements ActionListener {

    private JTextField fileField;
    private JButton chooseButton;

    public FileControl(Parameter parameter) {
        super(parameter);
        setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        fileField = new JTextField();
        fileField.putClientProperty("Jcomponent.sizeVariant", "small");
        fileField.setPreferredSize(new Dimension(150, 19));
        fileField.setEditable(false);
        fileField.setFont(PlatformUtils.getSmallFont());
        chooseButton = new JButton("...");
        chooseButton.putClientProperty("JButton.buttonType", "gradient");
        chooseButton.setPreferredSize(new Dimension(30, 27));
        chooseButton.addActionListener(this);
        add(fileField);
        add(chooseButton);
        setValueForControl(parameter.getValue());
    }

    public void setValueForControl(Object v) {
        fileField.setText(v.toString());
    }

    public String acceptedExtensions() {
        return "*";
    }

    public String acceptedDescription() {
        return "All files";
    }

    public void actionPerformed(ActionEvent e) {
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        File f = FileUtils.showOpenDialog(frame, parameter.asString(), acceptedExtensions(), acceptedDescription());
        if (f != null) {
            parameter.setValue(f.getAbsolutePath());
        }
    }

}
