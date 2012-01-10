/*
 * SyntaxUtilities.java - Utility functions used by syntax colorizing
 * Copyright (C) 1999 Slava Pestov
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

package nodebox.client.syntax;

import javax.swing.text.Segment;
import javax.swing.text.TabExpander;
import javax.swing.text.Utilities;
import java.awt.*;

/**
 * Class with several utility functions used by jEdit's syntax colorizing
 * subsystem.
 *
 * @author Slava Pestov
 * @version $Id: SyntaxUtilities.java,v 1.4 2003/06/30 17:31:10 blaisorblade Exp $
 */
public class SyntaxUtilities {
    /**
     * Checks if a subregion of a <code>Segment</code> is equal to a
     * string.
     *
     * @param ignoreCase True if case should be ignored, false otherwise
     * @param text       The segment
     * @param offset     The offset into the segment
     * @param match      The string to match
     */
    public static boolean regionMatches(boolean ignoreCase, Segment text,
                                        int offset, String match) {
        int length = offset + match.length();
        char[] textArray = text.array;
        if (length > text.offset + text.count)
            return false;
        for (int i = offset, j = 0; i < length; i++, j++) {
            char c1 = textArray[i];
            char c2 = match.charAt(j);
            if (ignoreCase) {
                c1 = Character.toUpperCase(c1);
                c2 = Character.toUpperCase(c2);
            }
            if (c1 != c2)
                return false;
        }
        return true;
    }

    /**
     * Checks if a subregion of a <code>Segment</code> is equal to a
     * character array.
     *
     * @param ignoreCase True if case should be ignored, false otherwise
     * @param text       The segment
     * @param offset     The offset into the segment
     * @param match      The character array to match
     */
    public static boolean regionMatches(boolean ignoreCase, Segment text,
                                        int offset, char[] match) {
        int length = offset + match.length;
        char[] textArray = text.array;
        if (length > text.offset + text.count)
            return false;
        for (int i = offset, j = 0; i < length; i++, j++) {
            char c1 = textArray[i];
            char c2 = match[j];
            if (ignoreCase) {
                c1 = Character.toUpperCase(c1);
                c2 = Character.toUpperCase(c2);
            }
            if (c1 != c2)
                return false;
        }
        return true;
    }

    /**
     * Returns the default style table. This can be passed to the
     * <code>setStyles()</code> method of <code>SyntaxDocument</code>
     * to use the default syntax styles.
     */
    public static SyntaxStyle[] getDefaultSyntaxStyles() {
        SyntaxStyle[] styles = new SyntaxStyle[Token.ID_COUNT];

        styles[Token.COMMENT1] = new SyntaxStyle(new Color(128, 128, 128), false, false);
        styles[Token.COMMENT2] = new SyntaxStyle(new Color(128, 128, 128), false, false);
        styles[Token.KEYWORD1] = new SyntaxStyle(new Color(0, 0, 255), false, false);
        styles[Token.KEYWORD2] = new SyntaxStyle(new Color(0, 0, 255), false, false);
        styles[Token.KEYWORD3] = new SyntaxStyle(new Color(0, 0, 255), false, true);
        styles[Token.LITERAL1] = new SyntaxStyle(new Color(255, 0, 255), false, false);
        styles[Token.LITERAL2] = new SyntaxStyle(new Color(255, 0, 255), false, false);
        styles[Token.LABEL] = new SyntaxStyle(new Color(255, 0, 0), false, false);
        styles[Token.OPERATOR] = new SyntaxStyle(Color.black, false, false);
        styles[Token.INVALID] = new SyntaxStyle(Color.red, false, false);
        styles[Token.METHOD] = new SyntaxStyle(new Color(0, 0, 255), false, true);


        return styles;
    }

    /**
     * Paints the specified line onto the graphics context. Note that this
     * method munges the offset and count values of the segment.
     *
     * @param line     The line segment
     * @param tokens   The token list for the line
     * @param styles   The syntax style list
     * @param expander The tab expander used to determine tab stops. May
     *                 be null
     * @param gfx      The graphics context
     * @param x        The x co-ordinate
     * @param y        The y co-ordinate
     * @return The x co-ordinate, plus the width of the painted string
     */
    public static int paintSyntaxLine(Segment line, Token tokens,
                                      SyntaxStyle[] styles, TabExpander expander, Graphics gfx,
                                      int x, int y) {
        Font defaultFont = gfx.getFont();
        Color defaultColor = gfx.getColor();

        int offset = 0;
        for (; ;) {
            byte id = tokens.id;
            if (id == Token.END)
                break;

            int length = tokens.length;

            if (id == Token.NULL) {
                if (!defaultColor.equals(gfx.getColor()))
                    gfx.setColor(defaultColor);
                if (!defaultFont.equals(gfx.getFont()))
                    gfx.setFont(defaultFont);
            } else
                styles[id].setGraphicsFlags(gfx, defaultFont);

            line.count = length;

            x = Utilities.drawTabbedText(line, x, y, gfx, expander, 0);
            line.offset += length;
            offset += length;

            tokens = tokens.next;
        }

        return x;
    }

    // private members
    private SyntaxUtilities() {
    }
}

/*
 * ChangeLog:
 * $Log: SyntaxUtilities.java,v $
 * Revision 1.4  2003/06/30 17:31:10  blaisorblade
 * Fix for line-ends.
 *
 * Revision 1.3  2003/06/29 13:37:27  gfx
 * Support of JDK 1.4.2
 *
 * Revision 1.2  2003/02/26 21:20:42  gfx
 * New PHP highlighting feature
 *
 * Revision 1.1.1.1  2001/08/20 22:31:56  gfx
 * Jext 3.0pre5
 *
 * Revision 1.2  2001/08/04 22:11:45  gfx
 * Methods colorizing, new Python 2.2 keyword
 *
 * Revision 1.1.1.1  2001/04/11 14:22:36  gfx
 *
 */