package nodebox.client;

import nodebox.node.Node;
import nodebox.node.Parameter;
import nodebox.node.Port;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

public class NodeAttributesDialog  extends JDialog {

    private NodeBoxDocument document;
    private Node node;

    private OKAction okAction = new OKAction();
    private CancelAction cancelAction = new CancelAction();

    private boolean changed = false;

    public NodeAttributesDialog(NodeBoxDocument document) {
        super(document, document.getActiveNode().getName() + " Metadata");

        this.document = document;
        this.node = document.getActiveNode();

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
                dispose();
            }
        }, escapeStroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    public Node getNode() {
        return document.getActiveNode();
    }

    public void addParameter(Node node, String parameterName) {
        onChanged();
        document.addParameter(node, parameterName);
    }

    public void removeParameter(Node node, String parameterName) {
        onChanged();
        document.removeParameter(node, parameterName);
    }

    public void setNodeExported(boolean exported) {
        onChanged();
        document.setNodeExported(node, exported);
    }

    public void setPortName(Port port, String name) {
        //document.setPortName(port, name);
    }

    public void setPortCardinality(Port port, Port.Cardinality cardinality) {
        //document.setPortCardinality(port, cardinality);
    }

    public void setParameterLabel(Parameter parameter, String label) {
        onChanged();
        document.setParameterLabel(parameter, label);
    }

    public void setParameterHelpText(Parameter parameter, String helpText) {
        onChanged();
        document.setParameterHelpText(parameter, helpText);
    }

    public void setParameterWidget(Parameter parameter, Parameter.Widget widget) {
        onChanged();
        document.setParameterWidget(parameter, widget);
    }

    public void setParameterValue(Parameter parameter, Object value) {
        onChanged();
        document.setParameterValue(parameter, value);
    }

    public void setParameterEnableExpression(Parameter parameter, String enableExpression) {
        onChanged();
        document.setParameterEnableExpression(parameter, enableExpression);
    }

    public void setParameterBoundingMethod(Parameter parameter, Parameter.BoundingMethod method) {
        onChanged();
        document.setParameterBoundingMethod(parameter, method);
    }

    public void setParameterMinimumValue(Parameter parameter, Float minimumValue) {
        onChanged();
        document.setParameterMinimumValue(parameter, minimumValue);
    }

    public void setParameterMaximumValue(Parameter parameter, Float maximumValue) {
        onChanged();
        document.setParameterMaximumValue(parameter, maximumValue);
    }

    public void setParameterDisplayLevel(Parameter parameter, Parameter.DisplayLevel displayLevel) {
        onChanged();
        document.setParameterDisplayLevel(parameter, displayLevel);
    }

    public void addParameterMenuItem(Parameter parameter, String key, String label) {
        onChanged();
        document.addParameterMenuItem(parameter, key, label);
    }

    public void removeParameterMenuItem(Parameter parameter, Parameter.MenuItem menuItem) {
        onChanged();
        document.removeParameterMenuItem(parameter, menuItem);
    }

    public void moveParameterItemDown(Parameter parameter, int index) {
        onChanged();
        document.moveParameterItemDown(parameter, index);
    }

    public void moveParameterItemUp(Parameter parameter, int index) {
        onChanged();
        document.moveParameterItemUp(parameter, index);
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
            putValue(ACCELERATOR_KEY, PlatformUtils.getKeyStroke(KeyEvent.VK_ENTER));
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
