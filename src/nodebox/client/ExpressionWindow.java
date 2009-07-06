package nodebox.client;

import nodebox.node.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class ExpressionWindow extends AbstractParameterEditor {

    private JTextArea expressionArea;
    private JTextArea errorArea;

    public ExpressionWindow(Parameter parameter) {
        super(parameter);
    }

    public Component getContentArea() {
        expressionArea = new JTextArea(getParameter().getExpression());
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
        return split;
    }

    public void valueChanged(Parameter source) {
        // Don't change the expression area if the expression is the same.
        // This would cause an infinite loop of setExpression/valueChanged calls.
        if (expressionArea.getText().equals(getParameter().getExpression())) return;
        expressionArea.setText(getParameter().getExpression());
    }

    public boolean save() {
        expressionArea.requestFocus();
        try {
            getParameter().setExpression(expressionArea.getText());
            errorArea.setText("");
            return true;
        } catch (ExpressionError ee) {
            errorArea.setText(ee.getCause().toString());
            return false;
        }
    }

    public static void main(String[] args) {
        NodeLibrary testLibrary = new NodeLibrary("test");
        Node node = Node.ROOT_NODE.newInstance(testLibrary, "test");
        Parameter pX = node.addParameter("x", Parameter.Type.FLOAT);
        AbstractParameterEditor win = new ExpressionWindow(pX);
        win.setVisible(true);
    }

}
