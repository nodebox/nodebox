package nodebox.ui;

import nodebox.client.Application;

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

    private String log;

    public ExceptionDialog(Frame owner, Throwable exception) {
        this(owner, exception, "", true);
    }

    public ExceptionDialog(Frame owner, Throwable exception, String extraMessage) {
        this(owner, exception, extraMessage, true);
    }

    public ExceptionDialog(Frame owner, Throwable exception, String extraMessage, boolean showQuitButton) {
        super(owner, "Error", true);
        Container container = getContentPane();
        container.setLayout(new BorderLayout(0, 0));
        JPanel innerPanel = new JPanel(new BorderLayout(10, 10));
        innerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel messageLabel = new JLabel("<html><b>Error: </b>" + exception.getMessage() + " " + extraMessage + "</html>");
        JTextArea textArea = new JTextArea();
        textArea.setFont(Theme.EDITOR_FONT);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        log = sw.toString();
        textArea.setText(log);
        textArea.setEditable(false);
        textArea.setCaretPosition(0);
        JScrollPane scrollPane = new JScrollPane(textArea);
        innerPanel.add(messageLabel, BorderLayout.NORTH);
        innerPanel.add(scrollPane, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 10, 10));
        if (showQuitButton) {
            JButton quitButton = new JButton("Quit");
            quitButton.addActionListener(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    setVisible(false);
                    Application.getInstance().quit();
                }
            });
            buttonPanel.add(quitButton);
        }
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
        // Simple spacer hack
        buttonPanel.add(copyButton);
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

    public static Throwable getRootCause(Throwable e) {
        if (e.getCause() == null) return e;
        if (e.getCause() == e) return e;
        return getRootCause(e.getCause());
    }

}