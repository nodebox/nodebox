package nodebox.client;

import nodebox.client.editor.SimpleEditor;
import nodebox.node.Node;
import nodebox.node.Parameter;
import nodebox.node.ProcessingContext;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.Element;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.WeakHashMap;

import static nodebox.base.Preconditions.checkNotNull;

public class EditorPane extends Pane implements CaretListener, ChangeListener, ActionListener {

    private final Map<Parameter, String> inProgressCode = new WeakHashMap<Parameter, String>();

    private final NodeBoxDocument document;
    private Node activeNode;
    private Parameter activeParameter;
    private String codeType = "_code";

    private PaneHeader paneHeader;
    private SimpleEditor editor;
    private EditorSplitPane splitter;
    private JTextArea messages;
    private NButton messagesCheck;
    private NButton reloadButton;
    private boolean ignoringChanges = true;

    public EditorPane(NodeBoxDocument document) {
        this.document = document;
        setLayout(new BorderLayout(0, 0));
        paneHeader = new PaneHeader(this);
        reloadButton = new NButton("Reload", "res/code-reload.png");
        setReloadButtonEnabled(false); // Only enable the button if the code has changed.
        reloadButton.setActionMethod(this, "reload");
        messagesCheck = new NButton(NButton.Mode.CHECK, "Messages");
        messagesCheck.setActionMethod(this, "toggleMessages");
        paneHeader.add(reloadButton);
        paneHeader.add(new Divider());
        paneHeader.add(messagesCheck);
        paneHeader.add(new Divider());
        PaneCodeMenu paneCodeMenu = new PaneCodeMenu();
        paneCodeMenu.addActionListener(this);
        paneHeader.add(paneCodeMenu);
        editor = new SimpleEditor();
        editor.addCaretListener(this);
        editor.addChangeListener(this);
        add(paneHeader, BorderLayout.NORTH);
        messages = new JTextArea();
        messages.setEditable(false);
        messages.setBorder(new TopLineBorder());
        messages.setFont(Theme.EDITOR_FONT);
        messages.setMargin(new Insets(0, 5, 0, 5));
        JScrollPane messagesScroll = new JScrollPane(messages, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        messagesScroll.setBorder(BorderFactory.createEmptyBorder());
        splitter = new EditorSplitPane(JSplitPane.VERTICAL_SPLIT, editor, messagesScroll);
        add(splitter, BorderLayout.CENTER);
    }

    public Pane duplicate() {
        return new EditorPane(document);
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

    public void setActiveNode(Node node) {
        activeNode = node;
        if (activeNode != null) {
            activeParameter = getCodeParameter(activeNode, codeType);
        } else {
            activeParameter = null;
        }
        updateSource();
    }

    public void setCodeType(String codeType) {
        this.codeType = codeType;
        activeParameter = getCodeParameter(activeNode, codeType);
        updateSource();
        paneHeader.repaint();
        clearMessages();
    }

    private void updateSource() {
        if (activeParameter != null) {
            if (hasInProgressCode(activeParameter)) {
                setSource(getInProgressCode(activeParameter));
                setReloadButtonEnabled(true);
            } else {
                Parameter codeParameter = activeNode.getParameter(codeType);
                setSource(codeParameter.asCode().getSource());
                setReloadButtonEnabled(false);
            }
        } else {
            setSource(null);
            setReloadButtonEnabled(false);
        }
    }

    public void reload() {
        // This method can be called from the menu bar, so we need to check if the code has really changed.
        if (!hasInProgressCode(activeParameter)) return;
        document.setActiveNodeCode(activeParameter, getSource());
        removeInProgressCode(activeParameter);
        // The code has been set so there is nothing to reload.
        setReloadButtonEnabled(false);
    }

    private void setReloadButtonEnabled(boolean v) {
        reloadButton.setEnabled(v);
        reloadButton.setWarning(v);
    }

    public String getSource() {
        return editor.getSource();
    }

    public boolean isIgnoringChanges() {
        return ignoringChanges;
    }

    public void setIgnoringChanges(boolean ignoringChanges) {
        this.ignoringChanges = ignoringChanges;
    }

    public void setSource(String source) {
        // Setting the source will trigger change events for every line of source code.
        // Ignore changes temporarily, and re-enable when the source has been set.
        setIgnoringChanges(true);
        if (source == null) {
            editor.setSource("");
            editor.setEnabled(false);
            messages.setEnabled(false);
            messages.setBackground(Theme.MESSAGES_BACKGROUND_COLOR);
            splitter.setShowMessages(false);
        } else {
            editor.setSource(source);
            editor.setEnabled(true);
            messages.setEnabled(true);
            messages.setBackground(Color.white);
            // The source has been set. All subsequent changes are triggered by the user.
            setIgnoringChanges(false);
        }
    }

    public void toggleMessages() {
        setShowMessages(messagesCheck.isChecked());
    }

    private void setShowMessages(boolean v) {
        splitter.setShowMessages(v);
        messagesCheck.setChecked(v);
    }

    /**
     * Set the messages to a given string.
     * This is used for exceptions in the handle code.
     *
     * @param s The string to display.
     */
    public void setMessages(String s) {
        if (s != null && !s.isEmpty()) {
            messages.setText(s);
            setShowMessages(true);
            // Ensure messages are visible.
            splitter.setShowMessages(true);
        } else {
            messages.setText("");
        }
    }

    /**
     * Clear out the text in messages.
     */
    public void clearMessages() {
        setMessages("");
    }

    /**
     * Update the messages after the node has done processing.
     * <p/>
     * This is only used for _code, not for _handles.
     *
     * @param node    The node that was processed.
     * @param context The node's processing context.
     */
    public void updateMessages(Node node, ProcessingContext context) {
        // If we're looking at the handle don't show messages for the node.
        if (codeType.equals("_handle")) return;
        StringBuilder sb = new StringBuilder();

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
            setShowMessages(true);
            // Ensure messages are visible
            splitter.setShowMessages(true);
        } else {
            messages.setText("");
        }
    }

    public void caretUpdate(CaretEvent e) {
        JEditorPane editArea = (JEditorPane) e.getSource();
        int caretPosition = editArea.getCaretPosition();
        Element root = editArea.getDocument().getDefaultRootElement();
        int line = root.getElementIndex(caretPosition) + 1;
        // Subtract the offset of the start of the line from the caret position.
        // Add one because line numbers are zero-based.
        int column = 1 + caretPosition - root.getElement(line - 1).getStartOffset();
        updatePosition(line, column);
    }

    private void updatePosition(int line, int column) {
        splitter.setLocation(line, column);
    }

    /**
     * This method gets called by the editor component if the source has changed.
     *
     * @param changeEvent The type of change.
     */
    public void stateChanged(ChangeEvent changeEvent) {
        // The editor also triggers state changes when using setSource(). We ignore those changes.
        if (isIgnoringChanges()) return;
        setInProgressCode(activeParameter, getSource());
        setReloadButtonEnabled(true);
        document.codeEdited(getSource());
    }

    //// In-progress code ////

    private Parameter getCodeParameter(Node node, String codeType) {
        checkNotNull(node, "Trying to get a code parameter for a null node.");
        Parameter p = node.getParameter(codeType);
        checkNotNull(p, "Parameter %s on node %s could not be found.", codeType, node);
        return p;
    }

    private void setInProgressCode(Parameter p, String source) {
        inProgressCode.put(p, source);
    }

    private String getInProgressCode(Parameter p) {
        return inProgressCode.get(p);
    }

    private void removeInProgressCode(Parameter p) {
        inProgressCode.remove(p);
    }

    private boolean hasInProgressCode(Parameter p) {
        return inProgressCode.containsKey(p);
    }

    public void actionPerformed(ActionEvent e) {
        // We know the event will always come from the pane code type menu.
        setCodeType(e.getActionCommand());
    }

    private class TopLineBorder implements Border {
        public void paintBorder(Component component, Graphics g, int x, int y, int width, int height) {
            g.setColor(Theme.DEFAULT_SPLIT_COLOR);
            g.drawLine(x, y, x + width, y);
        }

        public Insets getBorderInsets(Component component) {
            return new Insets(1, 0, 0, 0);
        }

        public boolean isBorderOpaque() {
            return true;
        }
    }

}
