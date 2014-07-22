package nodebox.client;

import nodebox.node.Node;
import nodebox.node.Port;
import nodebox.node.MenuItem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import nodebox.ui.Platform;

public class NodeAttributesDialog  extends JDialog {

    private NodeBoxDocument document;

    private OKAction okAction = new OKAction();
    private CancelAction cancelAction = new CancelAction();

    private boolean changed = false;

    public NodeAttributesDialog(NodeBoxDocument document) {
        super(document, document.getActiveNode().getName() + " Metadata");

        this.document = document;

        NodeAttributesEditor editor = new NodeAttributesEditor(this);
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
                if (changed) {
                    NodeAttributesDialog.this.document.stopEdits();
                    NodeAttributesDialog.this.document.undo();
                }
                dispose();
            }
        }, escapeStroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    public Node getNode() {
        return document.getActiveNode();
    }

    public void addPort(Node node, String portName, String portType) {
        onChanged();
        document.addPort(node, portName, portType);
    }

    public void removePort(Node node, String portName) {
        onChanged();
        document.removePort(node, portName);
    }

    /*public void setNodeExported(boolean exported) {
        onChanged();
        document.setNodeExported(node, exported);
    }

    public void setPortName(Port port, String name) {
        //document.setPortName(port, name);
    }

    public void setPortCardinality(Port port, Port.Cardinality cardinality) {
        //document.setPortCardinality(port, cardinality);
    }*/

    public void setNodeCategory(String category) {
        onChanged();
        document.setNodeCategory(getNode(), category);
    }

    public void setNodeDescription(String description) {
        onChanged();
        document.setNodeDescription(getNode(), description);
    }

    public void setNodeImage(String image) {
        onChanged();
        document.setNodeImage(getNode(), image);
    }

    public void setNodeOutputType(String outputType) {
        onChanged();
        document.setNodeOutputType(getNode(), outputType);
    }

    public void setNodeOutputRange(Port.Range range) {
        onChanged();
        document.setNodeOutputRange(getNode(), range);
    }

    public void setNodeFunction(String function) {
        onChanged();
        document.setNodeFunction(getNode(), function);
    }

    public void setNodeHandle(String handle) {
        onChanged();
        document.setNodeHandle(getNode(), handle);
    }

    public void setPortLabel(String port, String label) {
        onChanged();
        document.setPortLabel(port, label);
    }

    public void setPortDescription(String port, String description) {
        onChanged();
        document.setPortDescription(port, description);
    }

    public void setPortWidget(String port, Port.Widget widget) {
        onChanged();
        document.setPortWidget(port, widget);
    }

    public void setPortRange(String port, Port.Range range) {
        onChanged();
        document.setPortRange(port, range);
    }

    public void setPortValue(String port, Object value) {
        onChanged();
        document.setValue(document.getActiveNodePath(), port, value);
    }

    public void setPortMinimumValue(String port, Double minimumValue) {
        onChanged();
        document.setPortMinimumValue(port, minimumValue);
    }

    public void setPortMaximumValue(String port, Double maximumValue) {
        onChanged();
        document.setPortMaximumValue(port, maximumValue);
    }

    public void addPortMenuItem(String port, String key, String label) {
        onChanged();
        document.addPortMenuItem(port, key, label);
    }

    public void removePortMenuItem(String port, MenuItem menuItem) {
        onChanged();
        document.removePortMenuItem(port, menuItem);
    }

    public void movePortMenuItemDown(String port, int itemIndex) {
        onChanged();
        document.movePortMenuItemDown(port, itemIndex);
    }

    public void movePortMenuItemUp(String port, int itemIndex) {
        onChanged();
        document.movePortMenuItemUp(port, itemIndex);
    }

    public void updatePortMenuItem(String port, int itemIndex, String key, String label) {
        onChanged();
        document.updatePortMenuItem(port, itemIndex, key, label);
    }

    private void onChanged() {
        if (! changed) {
            document.startEdits("Node Metadata");
            changed = true;
        }
    }

    public class OKAction extends AbstractAction {
        public OKAction() {
            putValue(NAME, "Ok");
            putValue(ACCELERATOR_KEY, Platform.getKeyStroke(KeyEvent.VK_ENTER));
        }

        public void actionPerformed(ActionEvent e) {
            if (changed) {
                document.stopEdits();
            }
            NodeAttributesDialog.this.dispose();
        }
    }

    public class CancelAction extends AbstractAction {
        public CancelAction() {
            putValue(NAME, "Cancel");
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
        }

        public void actionPerformed(ActionEvent e) {
            if (changed) {
                document.stopEdits();
                document.undo();
            }
            NodeAttributesDialog.this.dispose();
        }
    }}
