package nodebox.client;

import nodebox.graphics.CanvasContext;
import nodebox.graphics.Text;
import nodebox.graphics.Geometry;
import nodebox.graphics.Path;
import org.python.util.PythonInterpreter;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.ByteArrayOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EditorDocument extends JFrame {
    private final static String WINDOW_MODIFIED = "windowModified";
    private static Logger logger = Logger.getLogger("nodebox.client.EditorDocument");

    private CanvasContext context;
    private PythonInterpreter interpreter;
    private EditorViewer viewer;
    private CodeArea codeArea;
    private FeedbackArea feedbackArea;

    private UndoManager undo = new UndoManager();
    private UndoAction undoAction = new UndoAction();
    private RedoAction redoAction = new RedoAction();

    public EditorDocument() {
        context = new CanvasContext();
        interpreter = new PythonInterpreter();
        JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        viewer = new EditorViewer();
        codeArea = new CodeArea();
        codeArea.setBorder(BorderFactory.createEtchedBorder());
        JScrollPane codeScroll = new JScrollPane(codeArea);
        codeScroll.setBorder(BorderFactory.createEmptyBorder());
        feedbackArea = new FeedbackArea();
        feedbackArea.setFont(Theme.EDITOR_FONT);
        feedbackArea.setBorder(BorderFactory.createEtchedBorder());
        JScrollPane feedbackScroll = new JScrollPane(feedbackArea);
        feedbackScroll.setBorder(BorderFactory.createEmptyBorder());
        JSplitPane split1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, codeScroll, feedbackScroll);
        split1.setBorder(BorderFactory.createEmptyBorder());
        split1.setDividerLocation(0.5);
        split1.setResizeWeight(0.5);
        JSplitPane split2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, viewer, split1);
        split2.setBorder(BorderFactory.createEmptyBorder());
        split2.setDividerLocation(0.5);
        split2.setResizeWeight(0.5);
        rootPanel.add(split2, BorderLayout.CENTER);
        setContentPane(rootPanel);
        initMenu();
        setSize(1100, 800);
    }

    private void initMenu() {
        JMenuBar menuBar = new JMenuBar();

        // Edit
        JMenu editMenu = new JMenu("Edit");
        editMenu.add(undoAction);
        editMenu.add(redoAction);
        editMenu.addSeparator();
        menuBar.add(editMenu);

        // Python
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
            //g2.translate(getWidth() / 2, getHeight() / 2);
            context.getCanvas().draw(g2);
        }
    }

    private void addString(String s) {
        try {
            feedbackArea.getDocument().insertString(feedbackArea.getDocument().getLength(), s, null);
        } catch (BadLocationException e) {
            logger.log(Level.WARNING, "addString: bad location (" + s + ")", e);
        }
    }

    private class RunAction extends AbstractAction {

        private RunAction() {
            putValue(Action.NAME, "Run");
            putValue(ACCELERATOR_KEY, PlatformUtils.getKeyStroke(KeyEvent.VK_R));
        }

        public void actionPerformed(ActionEvent actionEvent) {
            // Clear out feedback area
            context.resetContext();
            context.getCanvas().clear();
            feedbackArea.setText("");
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
            interpreter.set("g", context);
            interpreter.set("Path", nodebox.graphics.Path.class);
            interpreter.set("Canvas", nodebox.graphics.Canvas.class);
            interpreter.set("Color", nodebox.graphics.Color.class);
            interpreter.set("GraphicsContext", CanvasContext.class);
            interpreter.set("Grob", nodebox.graphics.Grob.class);
            interpreter.set("Path", Path.class);
            interpreter.set("Geometry", Geometry.class);
            interpreter.set("Image", nodebox.graphics.Image.class);
            interpreter.set("NodeBoxError", nodebox.graphics.NodeBoxError.class);
            interpreter.set("PathElement", nodebox.graphics.PathElement.class);
            interpreter.set("Point", nodebox.graphics.Point.class);
            interpreter.set("Rect", nodebox.graphics.Rect.class);
            interpreter.set("Text", nodebox.graphics.Text.class);
            interpreter.set("Transform", nodebox.graphics.Transform.class);
            interpreter.set("LEFT", Text.Align.LEFT);
            interpreter.set("RIGHT", Text.Align.RIGHT);
            interpreter.set("CENTER", Text.Align.CENTER);
            interpreter.set("JUSTIFY", Text.Align.JUSTIFY);
            interpreter.setOut(outputStream);
            interpreter.setErr(errorStream);
            Exception pythonException = null;
            String pythonCode = codeArea.getText();
            pythonCode = "_g = globals()\nfor n in dir(g): _g[n] = getattr(g, n)\n\n" + pythonCode;
            try {
                interpreter.exec(pythonCode);
            } catch (Exception e) {
                pythonException = e;
                logger.log(Level.INFO, "Error on exec", e);
            }
            String os = outputStream.toString();
            if (os.length() > 0)
                addString(os);
            if (!os.endsWith("\n"))
                addString("\n");
            if (pythonException != null)
                addString(pythonException.toString() + "\n");
            viewer.repaint();
        }
    }

    public class UndoAction extends AbstractAction {
        public UndoAction() {
            putValue(NAME, "Undo");
            putValue(ACCELERATOR_KEY, PlatformUtils.getKeyStroke(KeyEvent.VK_Z));
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) {
            try {
                undo.undo();
            } catch (CannotUndoException ex) {
                logger.log(Level.WARNING, "Unable to undo.", ex);
            }
            update();
            redoAction.update();
        }

        public void update() {
            if (undo.canUndo()) {
                setEnabled(true);
                putValue(Action.NAME, undo.getUndoPresentationName());
            } else {
                setEnabled(false);
                putValue(Action.NAME, "Undo");
            }
        }
    }

    public class RedoAction extends AbstractAction {
        public RedoAction() {
            putValue(NAME, "Redo");
            putValue(ACCELERATOR_KEY, PlatformUtils.getKeyStroke(KeyEvent.VK_Z, Event.SHIFT_MASK));
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) {
            try {
                undo.redo();
            } catch (CannotRedoException ex) {
                logger.log(Level.WARNING, "Unable to redo.", ex);
            }
            update();
            undoAction.update();
        }

        public void update() {
            if (undo.canRedo()) {
                setEnabled(true);
                putValue(Action.NAME, undo.getRedoPresentationName());
            } else {
                setEnabled(false);
                putValue(Action.NAME, "Redo");
            }
        }
    }


    private class FeedbackArea extends JTextArea {

    }
}
