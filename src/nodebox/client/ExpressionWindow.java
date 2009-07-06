package nodebox.client;

import nodebox.node.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class ExpressionWindow extends JFrame implements ParameterValueListener {

    private Parameter parameter;
    private JTextArea expressionArea;
    private JTextArea errorArea;

    public ExpressionWindow(Parameter parameter) {
        JPanel content = new JPanel(new BorderLayout(5, 5));
        content.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        this.parameter = parameter;

        expressionArea = new JTextArea(parameter.getExpression());
        expressionArea.setFont(PlatformUtils.getEditorFont());
        expressionArea.setBorder(null);
        JScrollPane expressionScroll = new JScrollPane(expressionArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        expressionScroll.setBorder(BorderFactory.createEtchedBorder());

        errorArea = new JTextArea();
        errorArea.setFont(PlatformUtils.getEditorFont());
        errorArea.setBorder(null);
        errorArea.setBackground(new Color(240, 240, 240));
        errorArea.setEditable(false);
        errorArea.setForeground(new Color(200, 0, 0));
        JScrollPane errorScroll = new JScrollPane(errorArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        errorScroll.setBorder(BorderFactory.createEtchedBorder());

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, expressionScroll, errorScroll);
        split.setBorder(null);
        split.setDividerLocation(150);
        split.setDividerSize(2);

        JPanel buttonRow = new JPanel();
        buttonRow.setLayout(new BoxLayout(buttonRow, BoxLayout.LINE_AXIS));
        JButton cancelButton = new JButton(new CancelAction());
        JButton applyButton = new JButton(new SaveAction());
        JButton saveButton = new JButton(new SaveAndCloseAction());
        buttonRow.add(cancelButton);
        buttonRow.add(Box.createHorizontalGlue());
        buttonRow.add(applyButton);
        buttonRow.add(saveButton);
        
        content.add(split, BorderLayout.CENTER);
        content.add(buttonRow, BorderLayout.SOUTH);
        setSize(500, 300);
        setContentPane(content);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        parameter.getNode().addParameterValueListener(this);
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        parameter.getNode().removeParameterValueListener(this);
    }

    public Parameter getParameter() {
        return parameter;
    }

    public void valueChanged(Parameter source) {
        // Don't change the expression area if the expression is the same.
        // This would cause an infinite loop of setExpression/valueChanged calls.
        if (expressionArea.getText().equals(parameter.getExpression())) return;
        expressionArea.setText(parameter.getExpression());
    }

    private class CancelAction extends AbstractAction {
        private CancelAction() {
            super("Cancel");
        }

        public void actionPerformed(ActionEvent e) {
            ExpressionWindow.this.dispose();
        }
    }

    private boolean saveExpression() {
        expressionArea.requestFocus();
        try {
            parameter.setExpression(expressionArea.getText());
            errorArea.setText("");
            return true;
        } catch (ExpressionError ee) {
            errorArea.setText(ee.getCause().toString());
            return false;
        }
    }

    private class SaveAction extends AbstractAction {
        private SaveAction() {
            super("Save");
        }

        public void actionPerformed(ActionEvent e) {
            saveExpression();
        }
    }

    private class SaveAndCloseAction extends AbstractAction {
        private SaveAndCloseAction() {
            super("Save & Close");
        }

        public void actionPerformed(ActionEvent e) {
            if (saveExpression()) {
                dispose();
            }
        }
    }

    public static void main(String[] args) {
        NodeLibrary testLibrary = new NodeLibrary("test");
        Node node = Node.ROOT_NODE.newInstance(testLibrary, "test");
        Parameter pX = node.addParameter("x", Parameter.Type.FLOAT);
        ExpressionWindow win = new ExpressionWindow(pX);
        win.setVisible(true);
    }

}
