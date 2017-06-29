package nodebox.client;

import com.google.common.collect.ImmutableMap;
import nodebox.ui.ExportFormat;
import nodebox.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Map;

/**
 * Dialog presented when exporting a single image.
 */
public class ExportDialog extends JDialog {

    private boolean dialogSuccessful = false;
    private JComboBox<String> formatBox;
    private JComboBox<Delimiter> delimiterBox;
    private JCheckBox quotesBox;

    private class Delimiter {
        private char delimiter;
        private String label;

        public Delimiter(char delimiter, String label) {
            this.delimiter = delimiter;
            this.label = label;
        }

        public String toString() {
            return label;
        }
    }

    public ExportDialog(Frame frame) {
        super(frame, "Export");
        setModal(true);
        setResizable(false);

        // Main
        setLayout(new BorderLayout(5, 5));
        final JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
        mainPanel.setBorder(new Theme.InsetsBorder(10, 10, 10, 10));
        add(mainPanel, BorderLayout.CENTER);

        // Format
        final JPanel formatPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 10, 10));
        formatPanel.add(new JLabel("Format:"));
        formatBox = new JComboBox<>();
        formatBox.addItem("SVG");
        formatBox.addItem("PNG");
        formatBox.addItem("PDF");
        formatBox.addItem("CSV");
        formatBox.setSelectedItem("SVG");
        formatPanel.add(formatBox);

        final JPanel delimiterPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 10, 10));
        delimiterPanel.add(new JLabel("Delimiter:"));
        delimiterBox = new JComboBox<>();
        delimiterBox.addItem(new Delimiter(',', ","));
        delimiterBox.addItem(new Delimiter(';', ";"));
        delimiterBox.addItem(new Delimiter(':', ":"));
        delimiterBox.addItem(new Delimiter('\t', "Tab"));
        delimiterBox.addItem(new Delimiter(' ', "Space"));
        delimiterPanel.add(delimiterBox);

        final JPanel quotesPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 10, 10));
        quotesPanel.add(new JLabel("Quotes:"));
        quotesBox = new JCheckBox("Escape output using quotes", true);
        quotesPanel.add(quotesBox);

        mainPanel.add(formatPanel);
        mainPanel.add(delimiterPanel);
        mainPanel.add(quotesPanel);
        delimiterPanel.setVisible(false);
        quotesPanel.setVisible(false);

        mainPanel.add(Box.createVerticalGlue());

        // Buttons
        mainPanel.add(Box.createVerticalStrut(10));
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 10, 0));
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doCancel();
            }
        });
        buttonPanel.add(cancelButton);
        JButton nextButton = new JButton("Next");
        nextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doNext();
            }
        });
        buttonPanel.add(nextButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        pack();
        getRootPane().setDefaultButton(nextButton);


        formatBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    boolean visible = e.getItem().toString().equals("CSV");
                    delimiterPanel.setVisible(visible);
                    quotesPanel.setVisible(visible);
                    mainPanel.validate();
                    ExportDialog.this.pack();
                }
            }
        });
    }

    private void doCancel() {
        setVisible(false);
    }

    private void doNext() {
        dialogSuccessful = true;
        setVisible(false);
    }

    public boolean isDialogSuccessful() {
        return dialogSuccessful;
    }

    public ExportFormat getFormat() {
        return ExportFormat.of(formatBox.getSelectedItem().toString());
    }

    public Map<String, ?> getExportOptions() {
        if (formatBox.getSelectedItem().equals("CSV")) {
            Delimiter d = (Delimiter) delimiterBox.getSelectedItem();
            return ImmutableMap.of("delimiter", d.delimiter, "quotes", quotesBox.isSelected());
        } else {
            return ImmutableMap.of();
        }
    }

    public static void main(String[] args) {
        ExportDialog d = new ExportDialog(null);
        d.setVisible(true);
    }
}
