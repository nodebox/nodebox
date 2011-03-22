package nodebox.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class InterruptableProgressDialog extends ProgressDialog {

    private JButton cancelButton;
    private Thread thread;

    public InterruptableProgressDialog(Frame owner, String title, int taskCount) {
        super(owner, title, taskCount);
        cancelButton = new JButton("Cancel");
        cancelButton.setBounds(270, 50, 80, 32);
        getContentPane().add(cancelButton);
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                if (thread != null)
                    thread.interrupt();
            }
        });
    }

    public Thread getThread() {
        return thread;
    }

    public void setThread(Thread thread) {
        this.thread = thread;
    }
}
