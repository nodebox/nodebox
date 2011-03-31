package nodebox.client;

import nodebox.client.editor.SimpleEditor;
import nodebox.node.*;
import nodebox.node.event.NodeUpdatedEvent;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.Element;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.PrintWriter;
import java.io.StringWriter;

public class EditorPane extends Pane implements ComponentListener, CaretListener, NodeEventListener, ChangeListener {

    private PaneHeader paneHeader;
    private SimpleEditor editor;
    private Node node;
    private EditorSplitter splitter;
    private JTextArea messages;
    private NButton messagesCheck;
    private String codeName, codeType;
    private boolean codeChanged = false;
    private boolean fireCodeChange = true;
    private NButton reloadButton;

    public EditorPane(NodeBoxDocument document) {
        super(document);
        document.getNodeLibrary().addListener(this);
        setLayout(new BorderLayout(0, 0));
        paneHeader = new PaneHeader(this);
        reloadButton = new NButton("Reload", "res/code-reload.png");
        reloadButton.setEnabled(false); // Only enable the button if the code has changed.
        reloadButton.setActionMethod(this, "reload");
        messagesCheck = new NButton(NButton.Mode.CHECK, "Messages");
        messagesCheck.setActionMethod(this, "toggleMessages");
        paneHeader.add(reloadButton);
        paneHeader.add(new Divider());
        paneHeader.add(messagesCheck);
        paneHeader.add(new Divider());
        paneHeader.add(new PaneCodeMenu(this));
        editor = new SimpleEditor();
        editor.addCaretListener(this);
        editor.addChangeListener(this);
        editor.setUndoManager(getDocument().getUndoManager());
        add(paneHeader, BorderLayout.NORTH);
        messages = new JTextArea();
        messages.setEditable(false);
        messages.setFont(Theme.EDITOR_FONT);
        messages.setMargin(new Insets(0, 5, 0, 5));
        JScrollPane messagesScroll = new JScrollPane(messages, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        messagesScroll.setBorder(BorderFactory.createEmptyBorder());
        splitter = new EditorSplitter(NSplitter.Orientation.VERTICAL, editor, messagesScroll);
        splitter.setEnabled(false);
        splitter.setPosition(1.0f);
        add(splitter, BorderLayout.CENTER);
        addComponentListener(this);
        setNode(document.getActiveNode());
    }

    public Pane clone() {
        return new EditorPane(getDocument());
    }

    public String getPaneName() {
        return "Source";
    }

    public PaneHeader getPaneHeader() {
        return paneHeader;
    }

    public PaneView getPaneView() {
        return editor;
    }

    public void setCodeType(String name, String codeType) {
        this.codeName = name;
        this.codeType = codeType;
        paneHeader.repaint();
        if (node != null) {
            setCode();
        }
    }

    public String getCodeName() {
        return codeName;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        if (this.node == node && node != null) return;
        if (codeChanged && this.node != null) {
            Parameter param = this.node.getParameter(codeType);
            getDocument().setChangedCodeForParameter(param, editor.getSource());
        }
        this.node = node;
        setCode();
    }

    private void setCode() {
        codeChanged = false;
        Parameter pCode = null;
        if (node != null) {
            pCode = node.getParameter(codeType);
        }
        if (pCode == null) {
            editor.setSource("");
            editor.setEnabled(false);
            messages.setEnabled(false);
            messages.setBackground(Theme.MESSAGES_BACKGROUND_COLOR);
            splitter.setPosition(1.0f);
            setCodeChanged(false);
            updateMessages(node, null);
        } else {
            String code = getDocument().getChangedCodeForParameter(pCode);
            boolean changed = code != null;
            if (! changed)
                code = pCode.asCode().getSource();
            fireCodeChange = false;
            editor.setSource(code);
            editor.setEnabled(true);
            setCodeChanged(changed);
            fireCodeChange = true;
            messages.setEnabled(true);
            messages.setBackground(Color.white);
            updateMessages(node, null);
        }
    }

    public boolean reload() {
        if (node == null) return false;
        Parameter pCode = node.getParameter(codeType);
        if (pCode == null) return false;
        NodeCode code = new PythonCode(editor.getSource());
        pCode.set(code);
        if (codeType.equals("_handle"))
            getDocument().setActiveNode(node); // to make Viewer reload handle
        setCodeChanged(false);
        getDocument().removeChangedCodeForParameter(pCode);
        return true;
    }

    public void toggleMessages() {
        setMessages(messagesCheck.isChecked());
    }

    private void setMessages(boolean v) {
        if (splitter.isEnabled() == v) return;
        if (v) {
            splitter.setEnabled(true);
            splitter.setPosition(0.6f);
        } else {
            splitter.setEnabled(false);
            splitter.setPosition(1.0f);
        }
        messagesCheck.setChecked(v);
    }

    @Override
    public void focusedNodeChanged(Node activeNode) {
        setNode(activeNode);
    }


    public void receive(NodeEvent event) {
        if (event.getSource() != this.node) return;
        if (event instanceof NodeUpdatedEvent) {
            updateMessages(event.getSource(), ((NodeUpdatedEvent) event).getContext());
        }
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
            if (splitter.getPosition() > 0.9f) {
                splitter.setPosition(0.6f);
            }
        } else {
            messages.setText("");
        }
    }

    //// Component events /////

    public void componentResized(ComponentEvent e) {
        // splitter.setDividerLocation(splitter.getHeight());
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

    public void stateChanged(ChangeEvent changeEvent) {
        // The document has changed.
        setCodeChanged(node != null);
    }

    private void setCodeChanged(boolean changed) {
        codeChanged = changed;
        reloadButton.setEnabled(changed);
        reloadButton.setWarning(changed);

        if (fireCodeChange) {
            getDocument().fireCodeChanged(node, true);
        }
    }
}
