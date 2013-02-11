package nodebox.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * SaveDialog
 */
public class SaveDialog extends JComponent {

    public static final Font messageFont = Theme.MESSAGE_FONT;
    public static final Font infoFont = Theme.INFO_FONT;

    private JDialog dialog;
    private int selectedValue;

    private DontSaveAction dontSaveAction = new DontSaveAction();
    private SaveAction saveAction = new SaveAction();
    private CancelAction cancelAction = new CancelAction();

    JButton dontSaveButton, cancelButton, saveButton;


    public SaveDialog() {
        initInterface();
    }

    private void initInterface() {
        setLayout(new BorderLayout());
        //Icon dialogIcon = Application.getInstance().getImageIcon();
        //JLabel iconLabel = new JLabel(dialogIcon);
        //iconLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new GridLayout(3, 1, 10, 0));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel messageLabel = new JLabel("Do you want to save changes to this document before closing?");
        messageLabel.setFont(messageFont);
        contentPanel.add(messageLabel);
        JLabel infoLabel = new JLabel("If you don't save, your changes will be lost.");
        infoLabel.setFont(infoFont);
        contentPanel.add(infoLabel);
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        dontSaveButton = new JButton(dontSaveAction);
        cancelButton = new JButton(cancelAction);
        saveButton = new JButton(saveAction);
        buttonPanel.add(dontSaveButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);
        contentPanel.add(buttonPanel);

        //add(iconLabel, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);

        setSize(400, 250);
    }

    public int show(JFrame frame) {
        dialog = new JDialog(frame, "Save Changes", true);
        Container contentPane = dialog.getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(this, BorderLayout.CENTER);
        dialog.setResizable(false);
        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        dialog.getRootPane().setDefaultButton(saveButton);
        dialog.setVisible(true);
        dialog.dispose();
        return selectedValue;
    }

    public class DontSaveAction extends AbstractAction {
        public DontSaveAction() {
            putValue(NAME, "Don't Save");
            putValue(ACCELERATOR_KEY, Platform.getKeyStroke(KeyEvent.VK_D));
        }

        public void actionPerformed(ActionEvent e) {
            selectedValue = JOptionPane.NO_OPTION;
            dialog.setVisible(false);
        }
    }

    public class SaveAction extends AbstractAction {
        public SaveAction() {
            putValue(NAME, "Save");
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
