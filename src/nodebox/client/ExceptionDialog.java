package nodebox.client;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionDialog extends JDialog implements ClipboardOwner {

    private Throwable exception;
    private String log;

    public ExceptionDialog(Frame owner, Throwable exception) {
        this(owner, exception, "");
    }

    public ExceptionDialog(Frame owner, Throwable exception, String extraMessage) {
        super(owner, "Error", true);
        Container container = getContentPane();
        container.setLayout(new BorderLayout(0, 0));
        JPanel innerPanel = new JPanel(new BorderLayout(10, 10));
        innerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        this.exception = exception;
        JLabel messageLabel = new JLabel("<html><b>Error: </b>" + exception.getClass().getName() + ": " + exception.getMessage() + " " + extraMessage + "</html>");
        messageLabel.setFont(Theme.INFO_FONT);
        JTextArea textArea = new JTextArea();
        textArea.setFont(Theme.EDITOR_FONT);
        StringBuffer sb = new StringBuffer();
        StringWriter sw = new StringWriter();
        exception.printStackTrace(new PrintWriter(sw));
        sb.append(sw.toString());
        sb.append(exception.getMessage());
        sb.append("\n");
        for (StackTraceElement ste : exception.getStackTrace()) {
            sb.append(ste.toString());
            sb.append("\n");
        }

        Throwable cause = exception.getCause();
        while (cause != null) {
            sb.append("Caused by: \n");
            sb.append(cause.toString());
            sb.append("\n");
            sb.append(cause.getMessage());
            sb.append("\n");
            for (StackTraceElement ste : cause.getStackTrace()) {
                sb.append(ste.toString());
                sb.append("\n");
            }
            cause = cause.getCause();
        }
        log = sb.toString();
        textArea.setText(sb.toString());
        textArea.setEditable(false);
        textArea.setCaretPosition(0);
        JScrollPane scrollPane = new JScrollPane(textArea);
        innerPanel.add(messageLabel, BorderLayout.NORTH);
        innerPanel.add(scrollPane, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 10, 10));
        JButton quitButton = new JButton("Quit");
        quitButton.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                System.exit(-1);
            }
        });
        JButton copyButton = new JButton("Copy");
        copyButton.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                Clipboard clipboard = getToolkit().getSystemClipboard();
                StringSelection ss = new StringSelection(log);
                clipboard.setContents(ss, ExceptionDialog.this);
            }
        });

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
        buttonPanel.add(quitButton);
        // Simple spacer hack
        buttonPanel.add(new JLabel("    "));
        buttonPanel.add(copyButton);
        buttonPanel.add(new JLabel("    "));
        buttonPanel.add(closeButton);
        innerPanel.add(buttonPanel, BorderLayout.SOUTH);
        container.add(innerPanel, BorderLayout.CENTER);
        setSize(600, 400);
        Window win = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
        SwingUtils.centerOnScreen(this, win);
    }

    public void lostOwnership(Clipboard clipboard, Transferable transferable) {
        // Do nothing
    }
}