package nodebox.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class InterruptibleProgressDialog extends ProgressDialog {

    private Thread thread;

    public InterruptibleProgressDialog(Frame owner, String title) {
        super(owner, title);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        setAlwaysOnTop(true);
        JButton cancelButton = new JButton("Cancel");
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
