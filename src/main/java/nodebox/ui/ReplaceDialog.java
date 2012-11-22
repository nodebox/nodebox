package nodebox.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

public class ReplaceDialog extends JComponent {

    private JDialog dialog;
    private int selectedValue = JOptionPane.CANCEL_OPTION;

    private ReplaceAction saveAction = new ReplaceAction();
    private CancelAction cancelAction = new CancelAction();

    JButton cancelButton, saveButton;

    private File file;


    public ReplaceDialog(File file) {
        this.file = file;
        initInterface();
    }

    private void initInterface() {
        setLayout(new BorderLayout());
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new GridLayout(3, 1, 10, 0));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel infoLabel = new JLabel();
        infoLabel.setText("<html>There is already a file named " +
                file.getName() +
                " in the chosen folder.<br>Do you want to replace it?</html>");
        infoLabel.setFont(Theme.MESSAGE_FONT);
        contentPanel.add(infoLabel);
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        cancelButton = new JButton(cancelAction);
        saveButton = new JButton(saveAction);
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);
        contentPanel.add(buttonPanel);

        add(contentPanel, BorderLayout.CENTER);

        Dimension d = new Dimension(450, 110);
        setPreferredSize(d);
        setMinimumSize(d);
        setMaximumSize(d);
        setSize(d);

    }

    public int show(JFrame frame) {
        dialog = new JDialog(frame, "Replace File", true);
        Container contentPane = dialog.getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(this, BorderLayout.CENTER);
        dialog.setResizable(false);
        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        dialog.getRootPane().setDefaultButton(saveButton);

        KeyStroke escapeStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        dialog.getRootPane().registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dialog.setVisible(false);
            }
        }, escapeStroke, JComponent.WHEN_IN_FOCUSED_WINDOW);

        dialog.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                dialog.setVisible(false);
            }
        });
        dialog.setVisible(true);
        dialog.dispose();
        return selectedValue;
    }

    public class ReplaceAction extends AbstractAction {
        public ReplaceAction() {
            putValue(NAME, "Replace");
            putValue(ACCELERATOR_KEY, Platform.getKeyStroke(KeyEvent.VK_S));
        }

        public void actionPerformed(ActionEvent e) {
            selectedValue = JOptionPane.YES_OPTION;
            dialog.setVisible(false);
        }
    }

    public class CancelAction extends AbstractAction {
        public CancelAction() {
            putValue(NAME, "Cancel");
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
        }

        public void actionPerformed(ActionEvent e) {
            selectedValue = JOptionPane.CANCEL_OPTION;
            dialog.setVisible(false);
        }
    }
}
