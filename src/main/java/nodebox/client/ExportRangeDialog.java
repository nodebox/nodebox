package nodebox.client;

import nodebox.ui.ExportFormat;
import nodebox.ui.Platform;
import nodebox.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * Dialog presented when exporting a range of frames.
 */
public class ExportRangeDialog extends JDialog implements ActionListener {

    private boolean dialogSuccessful = false;
    private JTextField fromField;
    private JTextField toField;
    private JTextField directoryField;
    private JComboBox<String> formatBox;

    private String exportPrefix;
    private File exportDirectory;
    private int fromValue;
    private int toValue;
    private ExportFormat format;
    private JTextField prefixField;
    private JButton exportButton;

    public ExportRangeDialog(Frame frame, File exportDirectory) {
        super(frame, "Export Range");
        setModal(true);
        setResizable(false);

        this.exportDirectory = exportDirectory;

        // Main
        setLayout(new BorderLayout(5, 5));
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
        mainPanel.setBorder(new Theme.InsetsBorder(10, 10, 10, 10));
        add(mainPanel, BorderLayout.CENTER);

        // Prefix
        JPanel prefixPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        prefixPanel.add(new JLabel("File Prefix:  "));
        prefixField = new JTextField("export", 20);
        prefixPanel.add(prefixField);
        mainPanel.add(prefixPanel);

        mainPanel.add(Box.createVerticalStrut(10));

        // Directory
        JPanel directoryPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        directoryPanel.add(new JLabel("Directory:  "));
        directoryField = new JTextField(20);
        directoryField.setEditable(false);
        directoryPanel.add(directoryField);
        JButton chooseButton = new JButton("...");
        chooseButton.putClientProperty("JButton.buttonType", "gradient");
        chooseButton.setPreferredSize(new Dimension(30, 27));
        chooseButton.addActionListener(this);
        directoryPanel.add(chooseButton);
        mainPanel.add(directoryPanel);

        mainPanel.add(Box.createVerticalStrut(10));

        // Range
        JPanel rangePanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 10, 0));
        rangePanel.add(new JLabel("From:"));
        fromField = new JTextField("1", 5);
        rangePanel.add(fromField);
        rangePanel.add(new JLabel("To:"));
        toField = new JTextField("100", 5);
        rangePanel.add(toField);
        mainPanel.add(rangePanel);

        // Format
        JPanel formatPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        formatPanel.add(new JLabel("Format:"));
        formatBox = new JComboBox<>();
        formatBox.addItem("SVG");
        formatBox.addItem("PNG");
        formatBox.addItem("PDF");
        formatBox.setSelectedItem("SVG");
        formatPanel.add(formatBox);
        mainPanel.add(formatPanel);

        mainPanel.add(Box.createVerticalGlue());

        // Buttons
        mainPanel.add(Box.createVerticalStrut(10));
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 10, 0));
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                doCancel();
            }
        });
        buttonPanel.add(cancelButton);
        exportButton = new JButton("Export");
        exportButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                doExport();
            }
        });
        if (exportDirectory == null)
            exportButton.setEnabled(false);
        buttonPanel.add(exportButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        pack();
        getRootPane().setDefaultButton(exportButton);

        setExportDirectory(exportDirectory);
    }

    private void doCancel() {
        setVisible(false);
    }

    private void doExport() {
        exportPrefix = prefixField.getText();
        try {
            fromValue = Integer.valueOf(fromField.getText());
        } catch (NumberFormatException e) {
            fromValue = 1;
        }
        try {
            toValue = Integer.valueOf(toField.getText());
        } catch (NumberFormatException e) {
            toValue = 100;
        }
        dialogSuccessful = true;
        setVisible(false);
    }

    public boolean isDialogSuccessful() {
        return dialogSuccessful;
    }

    public String getExportPrefix() {
        return exportPrefix;
    }

    public File getExportDirectory() {
        return exportDirectory;
    }

    private void setExportDirectory(File d) {
        this.exportDirectory = d;
        if (this.exportDirectory == null) {
            directoryField.setText("");
        } else {
            directoryField.setText(this.exportDirectory.getAbsolutePath());
        }
        exportButton.setEnabled(this.exportDirectory != null);
    }

    public int getFromValue() {
        return fromValue;
    }

    public int getToValue() {
        return toValue;
    }

    public ExportFormat getFormat() {
        return ExportFormat.of(formatBox.getSelectedItem().toString());
    }

    /**
     * Called when a directory needs to be chosen.
     *
     * @param e the action event
     */
    public void actionPerformed(ActionEvent e) {
        if (Platform.onMac()) {
            // On Mac, we can use the native FileDialog to choose a directory using a special property.
            FileDialog fileDialog = new FileDialog((Frame) null);
            System.setProperty("apple.awt.fileDialogForDirectories", "true");
            fileDialog.setVisible(true);
            System.setProperty("apple.awt.fileDialogForDirectories", "false");
            String chosenFile = fileDialog.getFile();
            if (chosenFile == null) {
                setExportDirectory(null);
                return;
            }
            String dir = fileDialog.getDirectory();
            File f = new File(dir, chosenFile);
            if (!f.isDirectory()) {
                setExportDirectory(f.getParentFile());
            } else {
                setExportDirectory(f);
            }
        } else {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int retVal = chooser.showOpenDialog(null);
            if (retVal == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                assert file.isDirectory();
                setExportDirectory(file);
            } else {
                setExportDirectory(null);
            }
        }
    }

    public static void main(String[] args) {
        ExportRangeDialog d = new ExportRangeDialog(null, null);
        d.setVisible(true);
    }
}
