package nodebox.client;

import nodebox.node.*;

import javax.swing.*;
import java.awt.*;

public class TextWindow extends AbstractParameterEditor {

    private JTextArea textArea;

    public TextWindow(Parameter parameter) {
        super(parameter);
    }

    public Component getContentArea() {
        textArea = new JTextArea(getParameter().asString());
        textArea.setFont(Theme.EDITOR_FONT);
        textArea.setBorder(null);
        JScrollPane textScroll = new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        textScroll.setBorder(BorderFactory.createEtchedBorder());
        return textScroll;
    }

    public boolean save() {
        textArea.requestFocus();
        try {
            NodeBoxDocument doc = NodeBoxDocument.getCurrentDocument();
            if (doc == null) throw new RuntimeException("No current active document.");
            doc.setParameterValue(getParameter(), textArea.getText());
            return true;
        } catch (IllegalArgumentException ee) {
            JOptionPane.showMessageDialog(this, "Error while saving parameter: " + ee.getMessage(), "Parameter error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    public void valueChanged(Parameter source) {
        // Don't change the expression area if the expression is the same.
        // This would cause an infinite loop of setExpression/valueChanged calls.
        if (textArea.getText().equals(getParameter().asString())) return;
        textArea.setText(getParameter().asString());
    }

    public static void main(String[] args) {
        NodeLibrary testLibrary = new NodeLibrary("test");
        Node node = Node.ROOT_NODE.newInstance(testLibrary, "test");
        Parameter pText = node.addParameter("text", Parameter.Type.STRING);
        AbstractParameterEditor win = new TextWindow(pText);
        win.setVisible(true);
    }

}
