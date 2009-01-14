package net.nodebox.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class ExceptionDialog extends JDialog {

    private Throwable exception;

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
        JLabel messageLabel = new JLabel("<html><b>Error: </b>" + exception.getMessage() + " " + extraMessage + "</html>");
        messageLabel.setFont(PlatformUtils.getInfoFont());
        JTextArea textArea = new JTextArea();
        textArea.setFont(PlatformUtils.getEditorFont());
        StringBuffer sb = new StringBuffer();
        for (StackTraceElement ste : exception.getStackTrace()) {
            sb.append(ste.toString());
            sb.append("\n");
        }


        Throwable cause = exception.getCause();
        if (cause != null) {
            sb.append("Caused by: \n");
            sb.append(cause.toString());
            sb.append("\n");
            for (StackTraceElement ste : cause.getStackTrace()) {
                sb.append(ste.toString());
                sb.append("\n");
            }
        }
        textArea.setText(sb.toString());
        textArea.setEditable(false);
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

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
        buttonPanel.add(quitButton);
        // Simple spacer hack
        buttonPanel.add(new JLabel("    "));
        buttonPanel.add(closeButton);
        innerPanel.add(buttonPanel, BorderLayout.SOUTH);
        container.add(innerPanel, BorderLayout.CENTER);
        setSize(600, 400);
        SwingUtils.centerOnScreen(this);
    }

}
