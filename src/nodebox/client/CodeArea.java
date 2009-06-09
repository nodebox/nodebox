package nodebox.client;

import nodebox.util.StringUtils;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Element;
import java.awt.*;
import java.awt.event.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CodeArea extends JEditorPane {

    /**
     * The last search string is the last search the user actually typed.
     */
    private String lastSearchString = "";

    /**
     * The current search string is the last search performed, whether selected text or typed text, and is
     * re-performed when find next is invoked.
     */
    private String currentSearchString = "";

    public final static Logger LOG = Logger.getLogger(CodeArea.class.getName());
    private Element rootElement;
    private boolean wrap;

    public static InputMap defaultInputMap;

    static {
        defaultInputMap = new InputMap();
        defaultInputMap.put(PlatformUtils.getKeyStroke(KeyEvent.VK_UP), DefaultEditorKit.beginAction);
        defaultInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), PythonEditorKit.returnAction);
        defaultInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), PythonEditorKit.tabAction);
        defaultInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, Event.SHIFT_MASK), PythonEditorKit.dedentAction);
        defaultInputMap.put(PlatformUtils.getKeyStroke('['), PythonEditorKit.dedentAction);
        defaultInputMap.put(PlatformUtils.getKeyStroke(']'), PythonEditorKit.indentAction);

        defaultInputMap.put(PlatformUtils.getKeyStroke(KeyEvent.VK_DOWN), DefaultEditorKit.endAction);
        defaultInputMap.put(PlatformUtils.getKeyStroke(KeyEvent.VK_UP, Event.SHIFT_MASK), DefaultEditorKit.selectionBeginAction);
        defaultInputMap.put(PlatformUtils.getKeyStroke(KeyEvent.VK_DOWN, Event.SHIFT_MASK), DefaultEditorKit.selectionEndAction);

    }

    private void init() {
        this.setMargin(new Insets(0, 5, 0, 5));
        setFont(PlatformUtils.getEditorFont());
        setEditorKit(new PythonEditorKit());
        rootElement = getDocument().getDefaultRootElement();

        // todo:this code should be in the kit
        for (KeyStroke ks : defaultInputMap.allKeys()) {
            getInputMap().put(ks, defaultInputMap.get(ks));
        }
        addMouseListener(new DragDetector());
    }

    public CodeArea() {
        init();
    }

    public void setWrap(boolean wrap) {
        this.wrap = wrap;
        setSize(getSize());
    }

    public boolean isWrap() {
        return wrap;
    }

    public void setSize(Dimension d) {
        if (!wrap) {
            if (d.width < getParent().getSize().width)
                d.width = getParent().getSize().width;
        }
        super.setSize(d);
    }

    public boolean getScrollableTracksViewportWidth() {
        return wrap;
    }

    /**
     * Position the cursor at the first occurence of the given text and scrolls it into view.
     * If the text is not found, nothing happens.
     *
     * @param text the text to find. Case-sensitive
     * @return true if the text was found.
     */
    public boolean find(String text) {
        // There is a difference between the last searched string and the current find string.
        // The last searched is the last typed find, whereas the current find is either
        // the last typed *or* the selected text.
        // Selected text is not saved as the last find.
        String selectedText = getSelectedText();
        if (text != null && !text.equals(selectedText)) {
            lastSearchString = text;
        }
        currentSearchString = text;
        return find(text, 0);
    }

    /**
     * Position the cursor at the next occurence of the text given to the previous find call.
     * If the text is not found, nothing happens.
     *
     * @return true if the text was found again.
     */
    public boolean findAgain() {
        return find(currentSearchString, getCaretPosition() + 1);
    }

    public boolean find(String text, int fromPos) {
        int index = getText().indexOf(text, fromPos);
        if (index >= 0) {
            setCaretPosition(index - 1);
            moveCaretPosition(index + text.length() - 1);
            scrollToPos(index);
            return true;
        }
        return false;
    }

    /**
     * Return the string that will be used for searching. If text is selected, the
     * selected text will be the search string. Otherwise, return the last search
     * string used.
     *
     * @return the string used for searching.
     */
    public String getSearchString() {
        String selectedText = getSelectedText();
        if (selectedText != null) {
            return selectedText;
        } else {
            return lastSearchString;
        }
    }

    /**
     * Set the caret at the beginning of the given line and scrolls the line into view.
     *
     * @param line the line number, one-based
     * @return true if the line could be found.
     */
    public boolean goToLine(int line) {
        int offset = getOffsetForLine(line);
        if (offset >= 0) {
            setCaretPosition(offset);
            scrollToPos(offset);
            return true;
        }
        return false;
    }

    /**
     * Returns the position in the document's text of the beginning of the given line.
     *
     * @param line the line number, one-based
     * @return the position at the beginning of the line, or -1 if the line number is invalid.
     */
    public int getOffsetForLine(int line) {
        if (line <= 0) {
            line = 1;
        }
        if (line > rootElement.getElementCount()) {
            line = rootElement.getElementCount();
        }
        Element lineEl = rootElement.getElement(line - 1);
        if (lineEl != null) {
            return lineEl.getStartOffset();
        }
        return -1;
    }

    /**
     * Returns the line number of the documents' text offset.
     *
     * @param offset the offset
     * @return the line-number, one-based
     */
    public int getLineForOffset(int offset) {
        return rootElement.getElementIndex(offset) + 1;
    }

    /**
     * Return the column for the given offset. This is the position in the current line.
     *
     * @param offset the offset
     * @return the column of the offset.
     * @see #getLineForOffset(int) the line for the given offset.
     */
    public int getColumnForOffset(int offset) {
        int line = getLineForOffset(offset);
        int begin = getOffsetForLine(line);
        return offset - begin + 1;
    }

    public boolean scrollToPos(int index) {
        try {
            // todo: FIX scrolling to half of view, so line is in the middle of the view.
            Rectangle r = modelToView(index);
            Dimension pDim = getParent().getSize();
            int halfHeight = pDim.height / 2;
            scrollRectToVisible(new Rectangle(r.x, r.y - halfHeight, r.width, r.height));
            return true;
        } catch (BadLocationException e) {
            return false;
        }
    }

    public class Dragger extends MouseMotionAdapter {
        public void mouseDragged(MouseEvent e) {
            super.mouseDragged(e);
        }
    }

    public class DragDetector implements MouseListener, MouseMotionListener {

        public DragDetector() {
            dragWindow = new JWindow();
            dragLabel = new JLabel();
            dragLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
            dragLabel.setText("12300.00");
            dragLabel.setAlignmentX(0F);
            dragWindow.getContentPane().add(dragLabel, BorderLayout.CENTER);
            dragWindow.pack();
        }

        public void mousePressed(MouseEvent e) {
            if ((e.getModifiersEx() & BTN1_CTRL_MASK) != BTN1_CTRL_MASK) return;
            LOG.info("mouse pressed at " + e.getPoint());
            int index = viewToModel(e.getPoint()); // Returns the correct coordinate.
            String text = getText();
            int[] range = StringUtils.numberRange(text, index);
            if (range == null) return;
            int left = range[0];
            int right = range[1];
            String number = StringUtils.numberForRange(text, range);
            LOG.info("The number is " + number);
            try {
                value = Float.parseFloat(number);
            } catch (NumberFormatException ex) {
                LOG.log(Level.WARNING, "parsing gave error: " + number, ex);
            }
            try {
                // Get position of rightmost character of the number.
                Rectangle r = modelToView(left);
                // Get the location of the code area.
                Point p = CodeArea.this.getLocationOnScreen();
                // The position of the window equals the bottom-left point of the rightmost character.
                p.x += r.x;
                p.y += r.y + r.height;
                dragWindow.setLocation(p);
                dragLabel.setText(number);
                dragWindow.setVisible(true);
                // Initialize the drag listener
                prevX = startX = e.getX();
                CodeArea.this.addMouseMotionListener(this);
            } catch (BadLocationException e1) {
                e1.printStackTrace();
            }
        }

        public void mouseReleased(MouseEvent e) {
            if (dragWindow.isVisible()) {
                dragWindow.setVisible(false);
                CodeArea.this.removeMouseMotionListener(this);
            }
        }

        public void mouseDragged(MouseEvent e) {
            int newX = e.getX();
            float delta = newX - prevX;
            LOG.info("prev " + prevX + " newX " + newX + " delta " + delta);
            //if ((e.getModifiersEx() & BTN1_CTRL_ALT_MASK) == BTN1_CTRL_ALT_MASK) delta /= 100;
            //if ((e.getModifiersEx() & BTN1_CTRL_SHIFT_MASK) == BTN1_CTRL_SHIFT_MASK) delta *= 10;
            value += delta;
            dragLabel.setText("" + value);
            prevX = newX;
            e.consume();
        }


        public void mouseClicked(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        public void mouseMoved(MouseEvent e) {
        }

        private JWindow dragWindow;
        private JLabel dragLabel;
        private int startX, prevX;
        private float value;

        private static final int BTN1_CTRL_MASK = MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.CTRL_DOWN_MASK;
        private static final int BTN1_CTRL_SHIFT_MASK = MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.CTRL_DOWN_MASK | MouseEvent.SHIFT_DOWN_MASK;
        private static final int BTN1_CTRL_ALT_MASK = MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.CTRL_DOWN_MASK | MouseEvent.ALT_DOWN_MASK;
    }

    public String getLastSearchString() {
        return lastSearchString;
    }

    public void setLastSearchString(String lastSearchString) {
        this.lastSearchString = lastSearchString;
    }

    public String getCurrentSearchString() {
        return currentSearchString;
    }

    public void setCurrentSearchString(String currentSearchString) {
        this.currentSearchString = currentSearchString;
    }

}
