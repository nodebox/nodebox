/*
 * TokenMarker.java - Generic token marker
 * Copyright (C) 1998, 1999 Slava Pestov
 * (C)2003 Romain GUY
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package nodebox.ui.syntax;

import javax.swing.text.Segment;

/**
 * A token marker that splits lines of text into tokens. Each token carries
 * a length field and an indentification tag that can be mapped to a color
 * for painting that token.<p>
 * <p/>
 * For performance reasons, the linked list of tokens is reused after each
 * line is tokenized. Therefore, the return value of <code>markTokens</code>
 * should only be used for immediate painting. Notably, it cannot be
 * cached.
 *
 * @author Slava Pestov
 * @version $Id: TokenMarker.java,v 1.5 2003/06/30 17:31:10 blaisorblade Exp $
 * @see nodebox.ui.syntax.Token
 */
public abstract class TokenMarker {
    /**
     * A wrapper for the lower-level <code>markTokensImpl</code> method
     * that is called to split a line up into tokens.
     *
     * @param line      The line
     * @param lineIndex The line number
     */
    public Token markTokens(Segment line, int lineIndex) {
        if (lineIndex >= length) {
            throw new IllegalArgumentException("Tokenizing invalid line: "
                    + lineIndex);
        }

        lastToken = null;

        LineInfo info = lineInfo[lineIndex];
        LineInfo prev;
        if (lineIndex == 0)
            prev = null;
        else
            prev = lineInfo[lineIndex - 1];

        byte oldToken = info.token;
        byte token = markTokensImpl(prev == null ?
                Token.NULL : prev.token, line, lineIndex);

        info.token = token;

        /*
         * This is a foul hack. It stops nextLineRequested
         * from being cleared if the same line is marked twice.
         *
         * Why is this necessary? It's all JEditTextArea's fault.
         * When something is inserted into the text, firing a
         * document event, the insertUpdate() method shifts the
         * caret (if necessary) by the amount inserted.
         *
         * All caret movement is handled by the select() method,
         * which eventually pipes the new position to scrollTo()
         * and calls repaint().
         *
         * Note that at this point in time, the new line hasn't
         * yet been painted; the caret is moved first.
         *
         * scrollTo() calls offsetToX(), which tokenizes the line
         * unless it is being called on the last line painted
         * (in which case it uses the text area's painter cached
         * token list). What scrollTo() does next is irrelevant.
         *
         * After scrollTo() has done it's job, repaint() is
         * called, and eventually we end up in paintLine(), whose
         * job is to paint the changed line. It, too, calls
         * markTokens().
         *
         * The problem was that if the line started a multiline
         * token, the first markTokens() (done in offsetToX())
         * would set nextLineRequested (because the line end
         * token had changed) but the second would clear it
         * (because the line was the same that time) and therefore
         * paintLine() would never know that it needed to repaint
         * subsequent lines.
         *
         * This bug took me ages to track down, that's why I wrote
         * all the relevant info down so that others wouldn't
         * duplicate it.
         */
        if (!(lastLine == lineIndex && nextLineRequested))
            nextLineRequested = (oldToken != token);

        lastLine = lineIndex;

        addToken(0, Token.END);

        return firstToken;
    }

    /**
     * An abstract method that splits a line up into tokens. It
     * should parse the line, and call <code>addToken()</code> to
     * add syntax tokens to the token list. Then, it should return
     * the initial token type for the next line.<p>
     * <p/>
     * For example if the current line contains the start of a
     * multiline comment that doesn't end on that line, this method
     * should return the comment token type so that it continues on
     * the next line.
     *
     * @param token     The initial token type for this line
     * @param line      The line to be tokenized
     * @param lineIndex The index of the line in the document,
     *                  starting at 0
     * @return The initial token type for the next line
     */
    protected abstract byte markTokensImpl(byte token, Segment line,
                                           int lineIndex);

    /**
     * Returns if the token marker supports tokens that span multiple
     * lines. If this is true, the object using this token marker is
     * required to pass all lines in the document to the
     * <code>markTokens()</code> method (in turn).<p>
     * <p/>
     * The default implementation returns true; it should be overridden
     * to return false on simpler token markers for increased speed.
     */
    public boolean supportsMultilineTokens() {
        return true;
    }

    /**
     * Informs the token marker that lines have been inserted into
     * the document. This inserts a gap in the <code>lineInfo</code>
     * array.
     *
     * @param index The first line number
     * @param lines The number of lines
     */
    public void insertLines(int index, int lines) {
        if (lines <= 0)
            return;
        length += lines;
        ensureCapacity(length);
        int len = index + lines;
        System.arraycopy(lineInfo, index, lineInfo, len,
                lineInfo.length - len);

        for (int i = index + lines - 1; i >= index; i--) {
            lineInfo[i] = new LineInfo();
        }
    }

    /**
     * Informs the token marker that line have been deleted from
     * the document. This removes the lines in question from the
     * <code>lineInfo</code> array.
     *
     * @param index The first line number
     * @param lines The number of lines
     */
    public void deleteLines(int index, int lines) {
        if (lines <= 0)
            return;
        int len = index + lines;
        length -= lines;
        System.arraycopy(lineInfo, len, lineInfo,
                index, lineInfo.length - len);
    }

    /**
     * Returns the number of lines in this token marker.
     */
    public int getLineCount() {
        return length;
    }

    /**
     * Returns true if the next line should be repainted. This
     * will return true after a line has been tokenized that starts
     * a multiline token that continues onto the next line.
     */
    public boolean isNextLineRequested() {
        return nextLineRequested;
    }

    // protected members

    /**
     * The first token in the list. This should be used as the return
     * value from <code>markTokens()</code>.
     */
    protected Token firstToken;

    /**
     * The last token in the list. New tokens are added here.
     * This should be set to null before a new line is to be tokenized.
     */
    protected Token lastToken;

    /**
     * An array for storing information about lines. It is enlarged and
     * shrunk automatically by the <code>insertLines()</code> and
     * <code>deleteLines()</code> methods.
     */
    protected LineInfo[] lineInfo;

    /**
     * The number of lines in the model being tokenized. This can be
     * less than the length of the <code>lineInfo</code> array.
     */
    protected int length;

    /**
     * The last tokenized line.
     */
    protected int lastLine;

    /**
     * True if the next line should be painted.
     */
    protected boolean nextLineRequested;

    /**
     * Creates a new <code>TokenMarker</code>. This DOES NOT create
     * a lineInfo array; an initial call to <code>insertLines()</code>
     * does that.
     */
    protected TokenMarker() {
        lastLine = -1;
    }

    /**
     * Ensures that the <code>lineInfo</code> array can contain the
     * specified index. This enlarges it if necessary. No action is
     * taken if the array is large enough already.<p>
     * <p/>
     * It should be unnecessary to call this under normal
     * circumstances; <code>insertLine()</code> should take care of
     * enlarging the line info array automatically.
     *
     * @param index The array index
     */
    protected void ensureCapacity(int index) {
        if (lineInfo == null)
            lineInfo = new LineInfo[index + 1];
        else if (lineInfo.length <= index) {
            LineInfo[] lineInfoN = new LineInfo[(index + 1) * 2];
            System.arraycopy(lineInfo, 0, lineInfoN, 0,
                    lineInfo.length);
            lineInfo = lineInfoN;
        }
    }

    /**
     * Returns the maximum line width in the specified line range.
     *
     * @param start The first line
     * @param len   The number of lines
     */
    public int getMaxLineWidth(int start, int len) {
        int retVal = 0;
        for (int i = start; i <= start + len; i++) {
            if (i >= length)
                break;
            retVal = Math.max(lineInfo[i].width, retVal);
        }
        return retVal;
    }

    /**
     * Adds a token to the token list.
     *
     * @param length The length of the token
     * @param id     The id of the token
     */
    protected void addToken(int length, byte id) {
        addToken(length, id, false);
    }

    protected void addToken(int length, byte id, boolean highlightBackground) {
        if (id >= Token.INTERNAL_FIRST && id <= Token.INTERNAL_LAST)
            throw new InternalError("Invalid id: " + id);

        if (length <= 0 && id != Token.END)
            return;

        if (firstToken == null) {
            firstToken = new Token(length, id);
            lastToken = firstToken;
        } else if (lastToken == null) {
            lastToken = firstToken;
            firstToken.length = length;
            firstToken.id = id;
        } else if (lastToken.next == null) {
            lastToken.next = new Token(length, id);
            lastToken = lastToken.next;
        } else {
            lastToken = lastToken.next;
            lastToken.length = length;
            lastToken.id = id;
        }
        lastToken.highlightBackground = highlightBackground;
    }

    /**
     * Store the width of a line, in pixels.
     *
     * @param lineIndex The line number
     * @param width     The width
     */
    public boolean setLineWidth(int lineIndex, int width) {
        LineInfo info = lineInfo[lineIndex];
        int oldWidth = info.width;
        info.width = width;
        return width != oldWidth;
    }

    /**
     * Inner class for storing information about tokenized lines.
     */
    public class LineInfo {
        /**
         * Creates a new LineInfo object with token = Token.NULL
         * and obj = null.
         */
        public LineInfo() {
        }

        /**
         * Creates a new LineInfo object with the specified
         * parameters.
         */
        public LineInfo(byte token, Object obj) {
            this.token = token;
            this.obj = obj;
        }

        public int width;

        /**
         * The id of the last token of the line.
         */
        public byte token;

        /**
         * This is for use by the token marker implementations
         * themselves. It can be used to store anything that
         * is an object and that needs to exist on a per-line
         * basis.
         */
        public Object obj;
    }
}

/*
 * ChangeLog:
 * $Log: TokenMarker.java,v $
 * Revision 1.5  2003/06/30 17:31:10  blaisorblade
 * Fix for line-ends.
 *
 * Revision 1.4  2003/06/29 13:37:27  gfx
 * Support of JDK 1.4.2
 *
 * Revision 1.3  2003/03/13 22:52:48  gfx
 * Improved focus gain
 *
 * Revision 1.2  2003/02/26 21:20:42  gfx
 * New PHP highlighting feature
 *
 * Revision 1.1.1.1  2001/08/20 22:31:48  gfx
 * Jext 3.0pre5
 *
 * Revision 1.1.1.1  2001/04/11 14:22:38  gfx
 *
 */