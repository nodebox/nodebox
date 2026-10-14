package nodebox.client;

import nodebox.movie.Movie;
import nodebox.movie.VideoFormat;
import nodebox.ui.Theme;
import nodebox.util.FileUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * Dialog presented when exporting a movie.
 */
public class ExportMovieDialog extends JDialog implements ActionListener {

    private boolean dialogSuccessful = false;
    private JTextField fromField;
    private JTextField toField;
    private JTextField fileField;
    private JComboBox<VideoFormat> formatBox;
    private JComboBox<String> frameRateBox;

    private File exportPath;
    private int fromValue;
    private int toValue;
    private double frameRateValue = Movie.DEFAULT_FRAME_RATE;
    private JButton exportButton;

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
        formatPanel.add(new JLabel("Format/Device: "));
        mainPanel.add(formatPanel);
        formatBox = new JComboBox<>();
        for (VideoFormat format : Movie.VIDEO_FORMATS) {
            formatBox.addItem(format);
        }
        formatBox.setSelectedItem(Movie.DEFAULT_FORMAT);
        formatPanel.add(formatBox);

        mainPanel.add(Box.createVerticalStrut(10));

        // Frame rate
        JPanel frameRatePanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        frameRatePanel.add(new JLabel("Frame Rate: "));
        frameRateBox = new JComboBox<>();
        for (double rate : Movie.FRAME_RATES) {
            frameRateBox.addItem(Movie.formatFrameRate(rate));
        }
        // Editable, so other rates such as 12 or 29.97 can be typed in.
        frameRateBox.setEditable(true);
        frameRateBox.setSelectedItem(Movie.formatFrameRate(Movie.DEFAULT_FRAME_RATE));
        frameRateBox.setPrototypeDisplayValue("29.97");
        frameRatePanel.add(frameRateBox);
        frameRatePanel.add(new JLabel(" fps"));
        mainPanel.add(frameRatePanel);

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

    public static void main(String[] args) {
        ExportMovieDialog d = new ExportMovieDialog(null, null);
        d.setVisible(true);
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
        frameRateValue = parseFrameRate(String.valueOf(frameRateBox.getEditor().getItem()));
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

    public VideoFormat getVideoFormat() {
        return (VideoFormat) formatBox.getSelectedItem();
    }

    /**
     * Parse a typed frame rate. Anything that is not a positive number falls back to the default.
     */
    static double parseFrameRate(String text) {
        try {
            double rate = Double.parseDouble(text.trim().replace(',', '.'));
            if (rate > 0 && rate <= 1000) return rate;
        } catch (NumberFormatException ignored) {
        }
        return Movie.DEFAULT_FRAME_RATE;
    }

    public double getFrameRate() {
        return frameRateValue;
    }

    public int getFromValue() {
        return fromValue;
    }

    public int getToValue() {
        return toValue;
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
}
