package net.nodebox.client;

import org.python.util.PythonInterpreter;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Console extends JTextPane {

    private Pane pane;
    private static Logger logger = Logger.getLogger("net.nodebox.client.Console");
    private PythonInterpreter interpreter;
    public static final String PROMPT = ">>> ";
    private ArrayList<String> inputHistory = new ArrayList<String>();

    public Console(Pane pane) {
        this.pane = pane;
        this.setMargin(new Insets(0, 5, 0, 5));
        setFont(PlatformUtils.getEditorFont());
        StyleContext styles = new StyleContext();
        StyledDocument doc = new DefaultStyledDocument(styles);
        setDocument(doc);
        Keymap defaultKeymap = JTextComponent.getKeymap(JTextComponent.DEFAULT_KEYMAP);
        Keymap keymap = JTextComponent.addKeymap(null, defaultKeymap);
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), new EnterAction());
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), new HistoryUpAction());
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), new HistoryDownAction());
        setKeymap(keymap);
        interpreter = new PythonInterpreter();
        newPrompt();
    }

    private void newPrompt() {
        addString(PROMPT);
    }

    public String getLastLine() {
        Element rootElement = getDocument().getDefaultRootElement();
        if (rootElement.getElementCount() == 0) return "";
        Element lastElement = rootElement.getElement(rootElement.getElementCount() - 1);
        try {
            int start = lastElement.getStartOffset() + PROMPT.length();
            int length = lastElement.getEndOffset() - start;
            return getDocument().getText(start, length);
        } catch (BadLocationException e) {
            logger.log(Level.WARNING, "Bad location", e);
            return "";
        }
    }

    private void addString(String s) {
        try {
            getDocument().insertString(getDocument().getLength(), s, null);
        } catch (BadLocationException e) {
            logger.log(Level.WARNING, "addString: bad location (" + s + ")", e);
        }
    }

    public void doEnter() {
        String lastLine = getLastLine();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        interpreter.setOut(outputStream);
        interpreter.setErr(errorStream);
        interpreter.set("document", pane.getDocument());
        Exception pythonException = null;
        try {
            interpreter.exec(lastLine);
        } catch (Exception e) {
            pythonException = e;
            logger.log(Level.INFO, "Error on exec", e);
        }
        addString("\n");
        String os = outputStream.toString();
        if (os.length() > 0)
            addString(os);
        if (!os.endsWith("\n"))
            addString("\n");
        if (pythonException != null)
            addString(pythonException.toString() + "\n");
        newPrompt();
    }

    public void doHistoryUp() {
        // TODO: Implement
        Toolkit.getDefaultToolkit().beep();
    }

    public void doHistoryDown() {
        // TODO: Implement
        Toolkit.getDefaultToolkit().beep();
    }

    public void handleLine(String string) {
        Element[] elements = getDocument().getRootElements();
    }

    private class EnterAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            doEnter();
        }
    }

    private class HistoryUpAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            doHistoryUp();
        }
    }

    private class HistoryDownAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            doHistoryDown();
        }
    }
}
