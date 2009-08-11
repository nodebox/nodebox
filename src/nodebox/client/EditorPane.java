package nodebox.client;

import nodebox.client.editor.SimpleEditor;
import nodebox.node.*;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.Element;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.PrintWriter;
import java.io.StringWriter;

public class EditorPane extends Pane implements DirtyListener, ComponentListener, CaretListener {

    private PaneHeader paneHeader;
    private SimpleEditor editor;
    private Node node;
    private EditorSplitter splitter;
    private JTextArea messages;
    private NButton messagesCheck;

    public EditorPane(NodeBoxDocument document) {
        this();
        setDocument(document);
    }

    public EditorPane() {
        setLayout(new BorderLayout(0, 0));
        paneHeader = new PaneHeader(this);
        NButton reloadButton = new NButton("Reload", "res/code-reload.png");
        reloadButton.setActionMethod(this, "reload");
        messagesCheck = new NButton(NButton.Mode.CHECK, "Messages");
        messagesCheck.setActionMethod(this, "toggleMessages");
        paneHeader.add(reloadButton);
        paneHeader.add(new Divider());
        paneHeader.add(messagesCheck);
        editor = new SimpleEditor();
        editor.addCaretListener(this);
        add(paneHeader, BorderLayout.NORTH);
        messages = new JTextArea();
        messages.setEditable(false);
        messages.setFont(PlatformUtils.getEditorFont());
        messages.setMargin(new Insets(0, 5, 0, 5));
        JScrollPane messagesScroll = new JScrollPane(messages, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        messagesScroll.setBorder(BorderFactory.createEmptyBorder());
        splitter = new EditorSplitter(JSplitPane.VERTICAL_SPLIT, editor, messagesScroll);
        splitter.setEnabled(false);
        add(splitter, BorderLayout.CENTER);
        addComponentListener(this);
    }

    @Override
    public void setDocument(NodeBoxDocument document) {
        super.setDocument(document);
        if (document == null) return;
        setNode(document.getActiveNode());
    }

    public Pane clone() {
        return new EditorPane(getDocument());
    }

    public String getPaneName() {
        return "Source";
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        if (this.node == node && node != null) return;
        Node oldNode = this.node;
        if (oldNode != null) {
            oldNode.removeDirtyListener(this);
        }
        this.node = node;
        Parameter pCode = null;
        if (node != null) {
            pCode = node.getParameter("_code");
            node.addDirtyListener(this);
        }
        if (pCode == null) {
            editor.setSource("");
            editor.setEnabled(false);
            messages.setEnabled(false);
            messages.setBackground(new Color(240, 240, 240));
            splitter.setDividerLocation(1.0);
            updateMessages(node, null);
        } else {
            String code = pCode.asCode().getSource();
            editor.setSource(code);
            editor.setEnabled(true);
            messages.setEnabled(true);
            messages.setBackground(Color.white);
            updateMessages(node, null);
        }
    }

    public boolean reload() {
        if (node == null) return false;
        Parameter pCode = node.getParameter("_code");
        if (pCode == null) return false;
        NodeCode code = new PythonCode(editor.getSource());
        pCode.set(code);
        return true;
    }

    public void toggleMessages() {
        setMessages(messagesCheck.isChecked());
    }

    private void setMessages(boolean v) {
        if (v) {
            splitter.setEnabled(true);
            splitter.setDividerLocation(getHeight() - 200);
        } else {
            splitter.setEnabled(false);
            splitter.setDividerLocation(1.0);
        }
        messagesCheck.setChecked(v);
    }

    @Override
    public void focusedNodeChanged(Node activeNode) {
        setNode(activeNode);
    }

    public void nodeDirty(Node node) {
        // Ignore this event.
    }

    public void nodeUpdated(Node node, ProcessingContext context) {
        updateMessages(node, context);
    }

    private void updateMessages(Node node, ProcessingContext context) {
        StringBuffer sb = new StringBuffer();

        if (node != null) {
            // Add the error messages.
            if (node.hasError()) {
                StringWriter sw = new StringWriter();
                Throwable t = node.getError();
                t.printStackTrace(new PrintWriter(sw));
                sb.append(t.toString());
                //sb.append(t.getMessage()).append("\n");
                Throwable cause = t.getCause();
                while (cause != null) {
                    sb.append("Caused by: \n");
                    sb.append(cause.toString()).append("\n");
                    // Sometimes the cause is stored recursively.
                    // Break out of the loop instead of triggering infinite recursion.
                    if (cause.getCause() == cause) {
                        break;
                    }
                    cause = cause.getCause();
                }
            }
            // Add the stdout messages.
            if (context != null && context.getOutput().length() > 0) {
                sb.append(context.getOutput());
            }
        }
        if (sb.length() > 0) {
            messages.setText(sb.toString());
            setMessages(true);
            // Ensure messages are visible
            if (splitter.getHeight() - splitter.getDividerLocation() < 100) {
                splitter.setDividerLocation(0.6);
            }
        } else {
            messages.setText("");
        }
    }

    //// Component events /////

    public void componentResized(ComponentEvent e) {
        splitter.setDividerLocation(splitter.getHeight());
    }

    public void componentMoved(ComponentEvent e) {
    }

    public void componentShown(ComponentEvent e) {
    }

    public void componentHidden(ComponentEvent e) {
    }

    public void caretUpdate(CaretEvent e) {
        JEditorPane editArea = (JEditorPane) e.getSource();
        int caretpos = editArea.getCaretPosition();
        Element root = editArea.getDocument().getDefaultRootElement();
        int linenum = root.getElementIndex(caretpos) + 1;
        // Subtract the offset of the start of the line from the caret position.
        // Add one because line numbers are zero-based.
        int columnnum = 1 + caretpos - root.getElement(linenum - 1).getStartOffset();
        updatePosition(linenum, columnnum);
    }

    private void updatePosition(int linenum, int columnnum) {
        splitter.setLocation(linenum, columnnum);
    }

    private class ReloadAction extends AbstractAction {
        private ReloadAction() {
            super("Reload");
            ImageIcon icon = new ImageIcon("res/code-reload.png", "Reload");
            putValue(Action.SMALL_ICON, icon);
        }

        public void actionPerformed(ActionEvent e) {
            if (node == null) return;
            Parameter pCode = node.getParameter("_code");
            if (pCode == null) return;
            NodeCode code = new PythonCode(editor.getSource());
            pCode.set(code);
        }
    }
}
