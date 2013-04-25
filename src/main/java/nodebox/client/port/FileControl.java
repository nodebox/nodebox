package nodebox.client.port;

import nodebox.client.NodeBoxDocument;
import nodebox.node.Port;
import nodebox.ui.Theme;
import nodebox.util.FileUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class FileControl extends AbstractPortControl implements ActionListener {

    private JTextField fileField;
    private JButton chooseButton;

    public FileControl(String nodePath, Port port) {
        super(nodePath, port);
        setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        fileField = new JTextField();
        fileField.putClientProperty("JComponent.sizeVariant", "small");
        fileField.setPreferredSize(new Dimension(150, 19));
        fileField.setEditable(false);
        fileField.setFont(Theme.SMALL_BOLD_FONT);
        chooseButton = new JButton("...");
        chooseButton.putClientProperty("JButton.buttonType", "gradient");
        chooseButton.setPreferredSize(new Dimension(30, 27));
        chooseButton.addActionListener(this);
        add(fileField);
        add(chooseButton);
        setValueForControl(port.getValue());
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        chooseButton.setEnabled(enabled);
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
        File f = FileUtils.showOpenDialog(frame, port.stringValue(), acceptedExtensions(), acceptedDescription());
        if (f != null) {
            File libraryFile = NodeBoxDocument.getCurrentDocument().getDocumentFile();
            if (libraryFile != null) {
                String relativePath = nodebox.util.FileUtils.getRelativePath(f, libraryFile.getParentFile());
                setPortValue(relativePath);
            } else {
                setPortValue(f.getAbsolutePath());
            }
        }
    }

}
