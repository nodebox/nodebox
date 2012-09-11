package nodebox.ui;

import javax.swing.*;
import java.awt.*;

public class ProgressDialog extends JDialog {

    private JProgressBar progressBar;
    private JLabel progressLabel;
    private JLabel messageLabel;
    private int tasksCompleted;
    private int taskCount;

    public ProgressDialog(Frame owner, String title) {
        super(owner, title, false);
        getRootPane().putClientProperty("Window.style", "small");
        setResizable(false);
        setLayout(null);
        Container contentPane = getContentPane();
        contentPane.setLayout(null);
        tasksCompleted = 0;
        this.taskCount = 0;
        progressBar = new JProgressBar(JProgressBar.HORIZONTAL, 0, taskCount);
        progressBar.setIndeterminate(true);
        progressBar.setBounds(10, 10, 300, 32);
        contentPane.add(progressBar);
        progressLabel = new JLabel();
        progressLabel.setBounds(320, 10, 50, 32);
        progressLabel.setVisible(false);
        contentPane.add(progressLabel);
        messageLabel = new JLabel();
        messageLabel.setBounds(10, 40, 380, 32);
        contentPane.add(messageLabel);
        updateProgress();
        setSize(400, 100);
        setLocationRelativeTo(owner);
    }

    public void setTaskCount(int taskCount) {
        this.taskCount = taskCount;
        this.tasksCompleted = 0;
        progressBar.setIndeterminate(false);
        progressBar.setMaximum(taskCount);
        progressLabel.setVisible(true);

    }

    public void reset() {
        setTaskCount(this.taskCount);
    }

    public void updateProgress() {
        updateProgress(this.tasksCompleted);
    }

    public void updateProgress(int tasksCompleted) {
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
    }


}
