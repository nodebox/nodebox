package nodebox.client;

import nodebox.ui.Theme;
import org.python.util.PythonInterpreter;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class Console extends JFrame implements WindowListener, FocusListener {

    private static final Color PROMPT_COLOR = new Color(72, 134, 242);
    private static final Color PROMPT_BORDER_TOP_COLOR = new Color(240, 240, 240);
    private static final Color ERROR_COLOR = new Color(255, 0, 0);
    private static final SimpleAttributeSet ATTRIBUTES_REGULAR = new SimpleAttributeSet();
    private static final SimpleAttributeSet ATTRIBUTES_ERROR = new SimpleAttributeSet();
    private static final SimpleAttributeSet ATTRIBUTES_COMMAND = new SimpleAttributeSet();

    static {
        ATTRIBUTES_COMMAND.addAttribute(StyleConstants.ColorConstants.Foreground, PROMPT_COLOR);
        ATTRIBUTES_ERROR.addAttribute(StyleConstants.ColorConstants.Foreground, ERROR_COLOR);
    }

    private static Logger logger = Logger.getLogger("nodebox.client.Console");
    private PythonInterpreter interpreter;
    private ArrayList<String> history = new ArrayList<String>();
    private int historyOffset = 0;
    private String temporarySavedCommand = null;
    private JTextField consolePrompt;
    private Document messagesDocument;

    public Console() {
        super("Console");
        JTextPane consoleMessages = new JTextPane();
        consoleMessages.setMargin(new Insets(2, 20, 2, 5));
        consoleMessages.setFont(Theme.EDITOR_FONT);
        consoleMessages.setEditable(false);
        consoleMessages.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                consolePrompt.requestFocus();
            }
        });
        messagesDocument = consoleMessages.getDocument();
        JScrollPane messagesScroll = new JScrollPane(consoleMessages, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        messagesScroll.setBorder(BorderFactory.createEmptyBorder());

        consolePrompt = new JTextField();
        consolePrompt.setFont(Theme.EDITOR_FONT);
        Keymap defaultKeymap = JTextComponent.getKeymap(JTextComponent.DEFAULT_KEYMAP);
        Keymap keymap = JTextComponent.addKeymap(null, defaultKeymap);
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), new EnterAction());
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), new HistoryUpAction());
        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), new HistoryDownAction());
        consolePrompt.setKeymap(keymap);
        consolePrompt.setBorder(new PromptBorder());

        setLayout(new BorderLayout());
        add(messagesScroll, BorderLayout.CENTER);
        add(consolePrompt, BorderLayout.SOUTH);

        interpreter = new PythonInterpreter();
        consolePrompt.requestFocus();
        addFocusListener(this);
        addWindowListener(this);
    }

    private void addMessage(String s, AttributeSet attributes) {
        try {
            messagesDocument.insertString(messagesDocument.getLength(), s, attributes);
        } catch (BadLocationException e) {
            logger.log(Level.WARNING, "addMessage: bad location (" + s + ")", e);
        }
    }

    private void addMessage(String s) {
        addMessage(s, ATTRIBUTES_REGULAR);
    }

    private void addCommandMessage(String s) {
        addMessage(s, ATTRIBUTES_COMMAND);
    }

    private void addErrorMessage(String s) {
        addMessage(s, ATTRIBUTES_ERROR);
    }

    public void doEnter() {
        String command = getCommand();
        addCommandToHistory(command);
        setCommand("");
        addCommandMessage(command + "\n");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        interpreter.setOut(outputStream);
        interpreter.setErr(errorStream);
        // HACK Indirect way to access the current document.
        NodeBoxDocument document = Application.getInstance().getCurrentDocument();
        interpreter.set("document", document);
        interpreter.set("root", document.getNodeLibrary().getRoot());
        interpreter.set("parent", document.getActiveNetwork());
        interpreter.set("node", document.getActiveNode());
        interpreter.exec("from nodebox.node import *");
        Exception pythonException = null;
        try {
            Object result = interpreter.eval(command);
            if (result != null) {
                addMessage(result.toString() + "\n");
            }
        } catch (Exception e) {
            pythonException = e;
        }
        String os = outputStream.toString();
        if (os.length() > 0) {
            addMessage(os);
            if (!os.endsWith("\n"))
                addMessage("\n");
        }
        if (pythonException != null)
            addErrorMessage(pythonException.toString());
    }

    public String getCommand() {
        return consolePrompt.getText();
    }

    public void setCommand(String command) {
        consolePrompt.setText(command);
    }

    public void moveBackInHistory() {
        if (historyOffset == history.size())
            return;
        if (historyOffset == 0) {
            temporarySavedCommand = getCommand();
        }
        historyOffset++;
        setCommand(history.get(history.size() - historyOffset));
    }

    public void moveForwardInHistory() {
        if (historyOffset == 0)
            return;
        historyOffset--;
        if (historyOffset == 0) {
            checkNotNull(temporarySavedCommand, "temporarySavedCommand is null.");
            setCommand(temporarySavedCommand);
            temporarySavedCommand = null;
        } else {
            setCommand(history.get(history.size() - historyOffset));
        }
    }

    public void addCommandToHistory(String command) {
        history.add(command);
        historyOffset = 0;
    }

    public void focusGained(FocusEvent focusEvent) {
        consolePrompt.requestFocus();
    }

    public void focusLost(FocusEvent focusEvent) {
    }

    private class EnterAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            doEnter();
        }
    }

    private class HistoryUpAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            moveBackInHistory();
        }
    }

    private class HistoryDownAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            moveForwardInHistory();
        }
    }

    //// Window events ////

    public void windowOpened(WindowEvent e) {
    }

    public void windowClosing(WindowEvent e) {
        Application.getInstance().onHideConsole();
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowDeactivated(WindowEvent e) {
    }


    private class PromptBorder implements Border {
        public void paintBorder(Component component, Graphics g, int x, int y, int width, int height) {
            g.setColor(PROMPT_BORDER_TOP_COLOR);
            g.drawLine(0, 0, width, 0);
            g.setColor(PROMPT_COLOR);
            g.drawString(">", 5, 14);
        }

        public Insets getBorderInsets(Component component) {
            return new Insets(3, 20, 3, 0);
        }

        public boolean isBorderOpaque() {
            return true;
        }
    }
}
