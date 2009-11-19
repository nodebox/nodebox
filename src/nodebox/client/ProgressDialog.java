package nodebox.client;

import javax.swing.*;
import java.awt.*;

public class ProgressDialog extends JDialog {

    private JProgressBar progressBar;
    private JLabel progressLabel;
    private JLabel messageLabel;
    private int tasksCompleted;
    private int taskCount;

    public ProgressDialog(Frame owner, String title, int taskCount) {
        super(owner, title, false);
        getRootPane().putClientProperty("Window.style", "small");
        setResizable(false);
        setLayout(null);
        Container contentPane = getContentPane();
        contentPane.setLayout(null);
        tasksCompleted = 0;
        this.taskCount = taskCount;
        progressBar = new JProgressBar(JProgressBar.HORIZONTAL, 0, taskCount);
        progressBar.setBounds(10, 10, 300, 32);
        contentPane.add(progressBar);
        progressLabel = new JLabel();
        progressLabel.setBounds(320, 10, 50, 32);
        contentPane.add(progressLabel);
        messageLabel = new JLabel();
        messageLabel.setBounds(10, 40, 380, 32);
        contentPane.add(messageLabel);
        updateProgress();
        setSize(400, 100);
        SwingUtils.centerOnScreen(this, owner);
    }

    public void updateProgress() {
        progressBar.setValue(tasksCompleted);
        double percentage = (double) (tasksCompleted) / (double) (taskCount);
        int ip = (int) (percentage * 100);
        progressLabel.setText(ip + " %");
        repaint();
    }

    public void tick() {
        // Increment the tasks completed, but it can never be higher than the number of tasks.
        tasksCompleted = Math.min(tasksCompleted + 1, taskCount);
        updateProgress();
    }

    public void setMessage(String message) {
        messageLabel.setText(message);
        repaint();
    }


}
