package nodebox.client.port;

import nodebox.client.NodeBoxDocument;
import nodebox.node.NodeLibrary;
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
        NodeBoxDocument doc = NodeBoxDocument.getCurrentDocument();
        File libraryFile = doc.getDocumentDirectory();
        File dataDirectory = new File(libraryFile, port.isImageWidget() ? NodeLibrary.FILE_TYPE_IMAGES : NodeLibrary.FILE_TYPE_DATA);

        String openPath = "";
        if (! doc.isTemporary() && dataDirectory.exists()) {
            File fileDirectory = new File(dataDirectory, fileField.getText()).getParentFile();
            if (fileDirectory.exists())
                openPath = fileDirectory.getAbsolutePath();
        }

        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        File f = FileUtils.showOpenDialog(frame, openPath, acceptedExtensions(), acceptedDescription());
        if (f != null) {
            String relativePath = FileUtils.getRelativeLink(f, dataDirectory);
            if (relativePath.startsWith("..")) {
                // The path is not a subpath of the project directory,
                // so the file needs to be imported into the project.
                FileUtils.createDirectoryIfMissing(dataDirectory);
                FileUtils.copyFile(f, new File(dataDirectory, f.getName()));
                setPortValue(f.getName());
            } else {
                setPortValue(relativePath);
            }
        }
    }

}
