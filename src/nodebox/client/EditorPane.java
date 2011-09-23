package nodebox.client;

import nodebox.client.editor.SimpleEditor;
import nodebox.node.Node;
import nodebox.node.ProcessingContext;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.Element;
import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;

public class EditorPane extends Pane implements CaretListener, ChangeListener {

    private PaneHeader paneHeader;
    private SimpleEditor editor;
    private EditorSplitPane splitter;
    private JTextArea messages;
    private NButton messagesCheck;
    private NButton reloadButton;
    private Delegate delegate;

    public EditorPane() {
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
        // TODO Use a listener for the PaneCodeMenu.
        paneHeader.add(new PaneCodeMenu(this));
        editor = new SimpleEditor();
        editor.addCaretListener(this);
        editor.addChangeListener(this);
        //editor.setUndoManager(getDocument().getUndoManager());
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
        return new EditorPane();
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

    public void onCodeParameterChanged(String codeParameter) {
        delegate.codeParameterChanged(this, codeParameter);
        paneHeader.repaint();
    }

    public void reload() {
        delegate.codeReloaded(this, editor.getSource());
    }

    public String getSource() {
        return editor.getSource();
    }

    public void setSource(String source) {
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
        }
    }

    public void toggleMessages() {
        setMessages(messagesCheck.isChecked());
    }

    private void setMessages(boolean v) {
        splitter.setShowMessages(v);
        messagesCheck.setChecked(v);
    }

    public void updateMessages(Node node, ProcessingContext context) {
        // TODO Replace with StringBuilder
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

    public void stateChanged(ChangeEvent changeEvent) {
        // TODO: This gets fired way too much, for example, once for every line when using setSource(s).
        // The document has changed.
        reloadButton.setEnabled(true);
        reloadButton.setWarning(true);
        // TODO Re-enable the delegate call when the method is not called so often.
        //delegate.codeEdited(this, getSource());
    }

    public Delegate getDelegate() {
        return delegate;
    }

    public void setDelegate(Delegate delegate) {
        this.delegate = delegate;
    }

    /**
     * A callback interface for listening to code edits.
     */
    public static interface Delegate {

        /**
         * Callback method invoked when code was edited.
         *
         * @param editorPane The editor pane that triggered the event.
         * @param source     The new source code.
         */
        public void codeEdited(EditorPane editorPane, String source);

        /**
         * Callback method invoked when code was reloaded.
         *
         * @param editorPane The editor pane that triggered the event.
         * @param source     The new source code.
         */
        public void codeReloaded(EditorPane editorPane, String source);

        /**
         * Callback method invoked when the code parameter was changed.
         * The code parameter is the name of the parameter that contains the code.
         * This is either "_code" or "_handle".
         *
         * @param editorPane    The editor pane that triggered the event.
         * @param codeParameter The name of the code parameter.
         */
        public void codeParameterChanged(EditorPane editorPane, String codeParameter);
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
