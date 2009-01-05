package net.nodebox.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class EditorDocument extends JFrame {
    private final static String WINDOW_MODIFIED = "windowModified";

    public EditorDocument() {
        JPanel rootPanel = new JPanel(new BorderLayout());
        EditorViewer viewer = new EditorViewer();
        JTextArea code = new JTextArea();
        JScrollPane codeScroll = new JScrollPane(code);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, viewer, codeScroll);
        split.setDividerLocation(0.5);
        rootPanel.add(split, BorderLayout.CENTER);
        setContentPane(rootPanel);
        initMenu();
        setSize(1100, 800);
    }

    private void initMenu() {
        JMenuBar menuBar = new JMenuBar();
        JMenu pythonMenu = new JMenu("Python");
        pythonMenu.add(new RunAction());
        menuBar.add(pythonMenu);
        setJMenuBar(menuBar);
    }


    private class EditorViewer extends JComponent {

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }
    }

    private class RunAction extends AbstractAction {

        private RunAction() {
            putValue(Action.NAME, "Run");
            putValue(ACCELERATOR_KEY, PlatformUtils.getKeyStroke(KeyEvent.VK_R));
        }

        public void actionPerformed(ActionEvent actionEvent) {

        }
    }
}
