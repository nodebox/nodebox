package nodebox.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Dialog presented when exporting a movie.
 */
public class ExportMovieDialog extends JDialog implements ActionListener {

    private boolean dialogSuccessful = false;
    private JTextField fromField;
    private JTextField toField;
    private JTextField fileField;
    private JComboBox formatBox;
    private JComboBox qualityBox;

    private File exportPath;
    private int fromValue;
    private int toValue;
    private JButton exportButton;
    private static final Map<String, Movie.CompressionQuality> compressionQualityMap;

    static {
        compressionQualityMap = new HashMap<String, Movie.CompressionQuality>();
        compressionQualityMap.put("Good", Movie.CompressionQuality.MEDIUM);
        compressionQualityMap.put("Better", Movie.CompressionQuality.HIGH);
        compressionQualityMap.put("Best", Movie.CompressionQuality.BEST);
    }

    public ExportMovieDialog(Frame frame, File exportPath) {
        super(frame, "Export Movie");
        setModal(true);
        setResizable(false);

        this.exportPath = exportPath;

        // Main
        setLayout(new BorderLayout(5, 5));
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
        mainPanel.setBorder(new Theme.InsetsBorder(10, 10, 10, 10));
        add(mainPanel, BorderLayout.CENTER);

        // Directory
        JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        filePanel.add(new JLabel("File:  "));
        fileField = new JTextField(20);
        fileField.setEditable(false);
        filePanel.add(fileField);
        JButton chooseButton = new JButton("...");
        chooseButton.putClientProperty("JButton.buttonType", "gradient");
        chooseButton.setPreferredSize(new Dimension(30, 27));
        chooseButton.addActionListener(this);
        filePanel.add(chooseButton);
        mainPanel.add(filePanel);

        mainPanel.add(Box.createVerticalStrut(10));

        // Format
        JPanel formatPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        formatPanel.add(new JLabel("Format:  "));
        mainPanel.add(formatPanel);
        formatBox = new JComboBox();
        formatBox.addItem(".mov H264");
        formatBox.addItem(".avi H264");
        formatBox.addItem(".mp4 H264");
        formatBox.setSelectedItem(".mov H264");
        formatPanel.add(formatBox);

        mainPanel.add(Box.createVerticalStrut(10));

        // Quality
        JPanel qualityPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        qualityPanel.add(new JLabel("Quality:  "));
        mainPanel.add(qualityPanel);
        qualityBox = new JComboBox();
        qualityBox.addItem("Good");
        qualityBox.addItem("Better");
        qualityBox.addItem("Best");
        qualityBox.setSelectedItem("Best");
        qualityPanel.add(qualityBox);

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
        if (exportPath == null)
            exportButton.setEnabled(false);
        buttonPanel.add(exportButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        pack();

        getRootPane().setDefaultButton(exportButton);

        if (this.exportPath != null && this.exportPath.isFile())
            setExportPath(this.exportPath);
    }

    private void doCancel() {
        setVisible(false);
    }

    private void doExport() {
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

    public File getExportPath() {
        return exportPath;
    }

    private void setExportPath(File d) {
        this.exportPath = d;
        if (this.exportPath == null)
            fileField.setText("");
        else
            fileField.setText(this.exportPath.getAbsolutePath());
        exportButton.setEnabled(this.exportPath != null);
    }

    public int getFromValue() {
        return fromValue;
    }

    public int getToValue() {
        return toValue;
    }

    public MovieFormat getFormat() {
        String selected = formatBox.getSelectedItem().toString();
        String name = selected.split(" ")[0].substring(1);
        return MovieFormat.of(name);
    }

    public Movie.CompressionQuality getQuality() {
        return compressionQualityMap.get(qualityBox.getSelectedItem().toString());
    }

    /**
     * Called when an output file needs to be chosen.
     *
     * @param e the action event
     */
    public void actionPerformed(ActionEvent e) {
        String path = exportPath == null ? null : exportPath.getAbsolutePath();
        File chosenFile = FileUtils.showSaveDialog(NodeBoxDocument.getCurrentDocument(), path, "mov,avi,mp4", "Movie files");
        setExportPath(chosenFile != null ? chosenFile : this.exportPath);
    }

    public static void main(String[] args) {
        ExportMovieDialog d = new ExportMovieDialog(null, null);
        d.setVisible(true);
    }
}
