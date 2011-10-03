package nodebox.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

public class NodeAttributesDialog  extends JDialog {

    private OKAction okAction = new OKAction();
    private CancelAction cancelAction = new CancelAction();

    public NodeAttributesDialog(NodeBoxDocument document) {
        super(document, document.getActiveNode().getName() + " Metadata");
        NodeAttributesEditor editor = new NodeAttributesEditor(document.getActiveNode());
        getContentPane().add(editor);
        setResizable(false);
        setModal(true);
        setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);

        JButton cancelButton = new JButton(cancelAction);
        JButton okButton = new JButton(okAction);
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
        bottomPanel.add(Box.createHorizontalGlue());
        bottomPanel.add(cancelButton);
        bottomPanel.add(okButton);
        getContentPane().add(bottomPanel, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(okButton);

        KeyStroke escapeStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        getRootPane().registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        }, escapeStroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    public class OKAction extends AbstractAction {
        public OKAction() {
            putValue(NAME, "Ok");
            putValue(ACCELERATOR_KEY, PlatformUtils.getKeyStroke(KeyEvent.VK_ENTER));
        }

        public void actionPerformed(ActionEvent e) {
            //todo: implement
            NodeAttributesDialog.this.dispose();
        }
    }

    public class CancelAction extends AbstractAction {
        public CancelAction() {
            putValue(NAME, "Cancel");
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
        }

        public void actionPerformed(ActionEvent e) {
            //todo: implement
            NodeAttributesDialog.this.dispose();
        }
    }}
