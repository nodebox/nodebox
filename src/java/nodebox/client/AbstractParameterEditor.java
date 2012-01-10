package nodebox.client;

import nodebox.node.Parameter;
import nodebox.node.ParameterValueListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public abstract class AbstractParameterEditor extends JFrame implements ParameterValueListener, ParameterEditor {

    private Parameter parameter;

    public AbstractParameterEditor(Parameter parameter) {
        this.parameter = parameter;
        setTitle(parameter.getAbsolutePath());

        JPanel content = new JPanel(new BorderLayout(5, 5));
        content.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel buttonRow = new JPanel();
        buttonRow.setLayout(new BoxLayout(buttonRow, BoxLayout.LINE_AXIS));
        JButton cancelButton = new JButton(new CancelAction());
        JButton applyButton = new JButton(new SaveAction());
        JButton saveButton = new JButton(new SaveAndCloseAction());
        buttonRow.add(cancelButton);
        buttonRow.add(Box.createHorizontalGlue());
        buttonRow.add(applyButton);
        buttonRow.add(saveButton);

        content.add(getContentArea(), BorderLayout.CENTER);
        content.add(buttonRow, BorderLayout.SOUTH);
        setSize(500, 300);
        setContentPane(content);
    }

    public abstract Component getContentArea();

    /**
     * Save the editor changes to the parameter.
     *
     * @return false if an error occurred.
     */
    public abstract boolean save();

    public abstract void valueChanged(Parameter source);

    public Parameter getParameter() {
        return parameter;
    }

    private class CancelAction extends AbstractAction {
        private CancelAction() {
            super("Cancel");
        }

        public void actionPerformed(ActionEvent e) {
            AbstractParameterEditor.this.dispose();
        }
    }

    private class SaveAction extends AbstractAction {
        private SaveAction() {
            super("Save");
        }

        public void actionPerformed(ActionEvent e) {
            save();
        }
    }

    private class SaveAndCloseAction extends AbstractAction {
        private SaveAndCloseAction() {
            super("Save & Close");
        }

        public void actionPerformed(ActionEvent e) {
            if (save()) {
                dispose();
            }
        }
    }

}
