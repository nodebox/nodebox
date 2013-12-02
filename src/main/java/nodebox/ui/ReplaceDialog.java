package nodebox.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;

public class ReplaceDialog extends JDialog {

    private boolean shouldReplace = false;

    private ReplaceDialog(File file) {
        super((Frame) null, "Replace " + file.getName());

        setModal(true);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setResizable(false);
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout(10, 10));
        JLabel infoLabel = new JLabel("<html>There is already a file named " +
                file.getName() +
                " in the chosen folder.<br>Do you want to replace it?</html>");
        contentPane.add(infoLabel, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                shouldReplace = false;
                dispose();
            }
        });
        JButton replaceButton = new JButton("Replace");
        replaceButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                shouldReplace = true;
                dispose();
            }
        });
        buttonPanel.add(cancelButton);
        buttonPanel.add(replaceButton);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);
        setContentPane(contentPane);
        contentPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setLocationRelativeTo(null);
        pack();
    }

    /**
     * Show the replace dialog for the given file. Returns true if the file should be replaced, false otherwise.
     *
     * @param file the file to replace.
     * @return true if the file should be replaced.
     */
    public static boolean showForFile(File file) {
        ReplaceDialog dialog = new ReplaceDialog(file);
        dialog.setVisible(true);
        return dialog.shouldReplace;
    }

    public static void main(String[] args) {
        boolean shouldReplace = ReplaceDialog.showForFile(new File("/Users/demo/Projects/deep/nodebox/file.ndbx"));
        System.out.println("shouldReplace = " + shouldReplace);
    }

}
