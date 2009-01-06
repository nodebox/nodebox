package net.nodebox.client;

import net.nodebox.client.syntax.PythonTokenMarker;
import net.nodebox.client.syntax.SyntaxColoredViewFactory;
import net.nodebox.client.syntax.SyntaxDocument;
import net.nodebox.client.syntax.TokenMarker;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.event.ActionEvent;

public class PythonEditorKit extends StyledEditorKit {

    public static final String returnAction = "python-return";
    public static final String tabAction = "python-tab";
    public static final String indentAction = "python-indent";
    public static final String dedentAction = "python-dedent";

    public static final String FOUR_SPACE = "    ";
    public static final int SPACES = 4;

    private static final Action[] defaultActions = {
            new ReturnAction(returnAction),
            new TabAction(tabAction),
            new IndentAction(indentAction),
            new DedentAction(dedentAction),
    };

    private SyntaxColoredViewFactory viewFactory;
    private TokenMarker tokenMarker;

    public PythonEditorKit() {
        tokenMarker = new PythonTokenMarker();
        viewFactory = new SyntaxColoredViewFactory();
    }

    public Document createDefaultDocument() {
        SyntaxDocument doc = new SyntaxDocument();
        doc.setTokenMarker(tokenMarker);
        return doc;
    }

    public ViewFactory getViewFactory() {
        return viewFactory;
    }

    // ---- General purpose line functions ---- //

    public static String getLineForOffset(Document doc, int offs) {
        int lineNo = doc.getDefaultRootElement().getElementIndex(offs);
        Element line = doc.getDefaultRootElement().getElement(lineNo);
        try {
            String strLine = doc.getText(line.getStartOffset(), line.getEndOffset() - line.getStartOffset());
            return strLine;
        } catch (BadLocationException e) {
            throw new AssertionError("The offset falls outside of the line I am requesting: " + e);
        }
    }

    public static String getLineForLineNum(Document doc, int lineNum) {
        Element line = doc.getDefaultRootElement().getElement(lineNum);
        try {
            String strLine = doc.getText(line.getStartOffset(), line.getEndOffset() - line.getStartOffset());
            return strLine;
        } catch (BadLocationException e) {
            throw new AssertionError("The line number falls outside of the range: " + e);
        }
    }

    public static int getLineNum(Document doc, int offs) {
        return doc.getDefaultRootElement().getElementIndex(offs);
    }


    public static String getIndentForOffset(Document doc, int offs) {
        String line = getLineForOffset(doc, offs);
        return getIndent(line);
    }

    public static String getIndentForLine(Document doc, int lineNum) {
        String line = getLineForLineNum(doc, lineNum);
        return getIndent(line);
    }

    public static int getOffsetForLine(Document doc, int lineNum) {
        Element line = doc.getDefaultRootElement().getElement(lineNum);
        return line.getStartOffset();
    }

    /**
     * Get indent for one line of text.
     */
    public static String getIndent(String line) {
        String indent = "";
        int i = 0;
        while (i < line.length()) {
            char c = line.charAt(i);
            if (c == ' ' || c == '\t') {
                indent += c;
            } else {
                break;
            }
            i++;
        }
        return indent;
    }

    public static String indent(String line) {
        return getIndent(line) + FOUR_SPACE + stripIndent(line);
    }

    public static void indent(Document doc, int lineNum) {
        String line = getLineForLineNum(doc, lineNum);
        int offs = getOffsetForLine(doc, lineNum) + getIndentForLine(doc, lineNum).length();
        try {
            doc.insertString(offs, FOUR_SPACE, null);
        } catch (BadLocationException e) {
            throw new AssertionError("A bug in the dendenting code. Line: \"" + line + "\" Offset: " + offs);
        }
    }

    public static String dedent(String line) {
        int[] metrics = getDedentMetrics(line);
        String begin = line.substring(0, metrics[0]);
        String rest = line.substring(metrics[0] + metrics[1]);
        return begin + rest;
    }

    public static void dedent(Document doc, int lineNum) {
        String line = getLineForLineNum(doc, lineNum);
        int[] metrics = getDedentMetrics(line);
        try {
            doc.remove(getOffsetForLine(doc, lineNum) + metrics[0], metrics[1]);
        } catch (BadLocationException e) {
            throw new AssertionError("A bug in the dendenting code. Line: \"" + line + "\" Offset: " + metrics[0] + " length: " + metrics[1]);
        }
    }

    /**
     * Returns the offset and length of the dedent.
     *
     * @return an int array where the first value is the offset and the last the length.
     */
    public static int[] getDedentMetrics(String line) {
        String indent = getIndent(line);
        if (indent.length() == 0) return new int[]{0, 0};
        int offs = indent.length(); // Holds the position of where the cut will happen.
        int len = 0;
        // Indents can be either spaces or tabs.
        // When we encounter spaces, try to strip four off (the default).
        // If we encounter less than four before the end of the line or before a tab character,
        // just delete as much as we can.
        // If we encounter a tab character, just delete that.
        // Something else is impossible, because indents can only consist of spaces and tabs.
        while (offs > 0) {
            offs--;
            char c = indent.charAt(offs);
            if (c == ' ') {
                len++;
                if (len >= SPACES) break;
            } else if (c == '\t') {
                // no spaces yet
                if (len == 0) {
                    len = 1; // Just remove a single tab character...
                    break; // and stop the search.
                } else {
                    // If the len is bigger than 0, we were trimming off spaces until
                    // we encountered this tab. We only want to delete those spaces,
                    // and not this tab character, so:
                    offs++; // Indicate that this character should not be removed...
                    break; // and stop the search.
                }
            } else {
                throw new AssertionError("An indent should only consist of spaces and tabs. I got a " + c + ".");
            }
        }
        return new int[]{offs, len};
    }


    public static String stripIndent(Document doc, int offs) {
        String line = getLineForOffset(doc, offs);
        return stripIndent(line);
    }

    /**
     * Strips the indent from a line of text.
     */
    public static String stripIndent(String line) {
        int i = 0;
        while (i < line.length()) {
            char c = line.charAt(i);
            if (c == ' ' || c == '\t') {
                i++;
            } else {
                break;
            }
        }
        return line.substring(i);
    }


    public Action[] getActions() {
        return TextAction.augmentList(super.getActions(), defaultActions);
    }

    public static class BackspaceAction extends StyledTextAction {
        public BackspaceAction(String name) {
            super(name);
        }

        public void actionPerformed(ActionEvent e) {
            JTextComponent target = getTextComponent(e);
            boolean beep = true;
            if ((target != null) && (target.isEditable())) {
                try {
                    Document doc = target.getDocument();
                    Caret caret = target.getCaret();
                    int dot = caret.getDot();
                    int mark = caret.getMark();
                    if (dot != mark) {
                        doc.remove(Math.min(dot, mark), Math.abs(dot - mark));
                        beep = false;
                    } else if (dot > 0) {
                        int delChars = 1;

                        // todo: tab-deletion
                        // Find fake tabs and delete them
                        // A fake tab is always in the beginning of the line (the indent),
                        // so we should be near or inside the indent.
                        // Then, if there are four space characters, that's the tab, and you can delete them all.

                        if (dot > 1) {
                            String dotChars = doc.getText(dot - 2, 2);
                            char c0 = dotChars.charAt(0);
                            char c1 = dotChars.charAt(1);

                            if (c0 >= '\uD800' && c0 <= '\uDBFF' &&
                                    c1 >= '\uDC00' && c1 <= '\uDFFF') {
                                delChars = 2;
                            }
                        }

                        doc.remove(dot - delChars, delChars);
                        beep = false;
                    }
                } catch (BadLocationException bl) {
                }
            }
            if (beep) {
                UIManager.getLookAndFeel().provideErrorFeedback(target);
            }
        }
    }

    public static class ReturnAction extends StyledTextAction {
        public ReturnAction(String name) {
            super(name);
        }

        public void actionPerformed(ActionEvent e) {
            JTextComponent target = getTextComponent(e);
            if ((target != null) && (target.isEditable())) {
                int offs = target.getCaretPosition();

                String indent = getIndentForOffset(target.getDocument(), offs);

                try {
                    String prevChar = target.getDocument().getText(offs - 1, 1);
                    if (":".equals(prevChar)) {
                        target.getDocument().insertString(offs, "\n" + indent + FOUR_SPACE, null);
                        return;
                    }
                } catch (BadLocationException e1) {
                    // Out-of-bounds, there is no prev char.
                }
                try {
                    target.getDocument().insertString(offs, "\n" + indent, null);
                } catch (BadLocationException e1) {
                    // pass
                }
            }
        }
    }

    public static class TabAction extends StyledTextAction {
        public TabAction(String name) {
            super(name);
        }

        public void actionPerformed(ActionEvent e) {
            JTextComponent target = getTextComponent(e);
            if ((target != null) && (target.isEditable())) {
                Document doc = target.getDocument();
                Caret caret = target.getCaret();
                int dot = caret.getDot();
                int mark = caret.getMark();
                if (dot != mark) { // Text is selected. Indent all text.
                    int begin = Math.min(dot, mark);
                    int end = Math.max(dot, mark);
                    // removing one character from the last line compensates for lines
                    // where only the enter is selected, and which should not be
                    // indented.
                    end--;
                    int firstLine = getLineNum(doc, begin);
                    int lastLine = getLineNum(doc, end);
                    // go through each line.
                    for (int i = firstLine; i <= lastLine; i++) {
                        indent(doc, i);
                    }
                } else if (dot > 0) { // The cursor is just somewhere. Indent just this line.
                    try {
                        doc.insertString(dot, FOUR_SPACE, null);
                    } catch (BadLocationException e1) {
                        throw new AssertionError("Position of dot falls outside of the document. Dot: " + dot);
                    }
                }
            }
        }
    }

    public static class IndentAction extends StyledTextAction {
        public IndentAction(String name) {
            super(name);
        }

        public void actionPerformed(ActionEvent e) {
            JTextComponent target = getTextComponent(e);
            if ((target != null) && (target.isEditable())) {
                Document doc = target.getDocument();
                Caret caret = target.getCaret();
                int dot = caret.getDot();
                int mark = caret.getMark();
                if (dot != mark) { // Text is selected. Indent all text.
                    int begin = Math.min(dot, mark);
                    int end = Math.max(dot, mark);
                    // removing one character from the last line compensates for lines
                    // where only the enter is selected, and which should not be
                    // indented.
                    end--;
                    int firstLine = getLineNum(doc, begin);
                    int lastLine = getLineNum(doc, end);
                    // go through each line.
                    for (int i = firstLine; i <= lastLine; i++) {
                        indent(doc, i);
                    }
                } else if (dot > 0) { // The cursor is just somewhere. Indent just this line.
                    int line = getLineNum(doc, dot);
                    indent(doc, line);
                }
            }
        }
    }

    public static class DedentAction extends StyledTextAction {
        public DedentAction(String nm) {
            super(nm);    //To change body of overridden methods use File | Settings | File Templates.
        }

        public void actionPerformed(ActionEvent e) {
            JTextComponent target = getTextComponent(e);
            if ((target != null) && (target.isEditable())) {
                Document doc = target.getDocument();
                Caret caret = target.getCaret();
                int dot = caret.getDot();
                int mark = caret.getMark();
                // A selection was made
                if (dot != mark) {
                    int begin = Math.min(dot, mark);
                    int end = Math.max(dot, mark);
                    // removing one character from the last line compensates for lines
                    // where only the enter is selected, and which should not be
                    // dedented.
                    end--;
                    int firstLine = getLineNum(doc, begin);
                    int lastLine = getLineNum(doc, end);
                    // go through each line.
                    for (int i = firstLine; i <= lastLine; i++) {
                        dedent(doc, i);
                    }
                } else if (dot > 0) { // No selection
                    dedent(doc, getLineNum(doc, dot));
                }
            }
        }
    }

}
