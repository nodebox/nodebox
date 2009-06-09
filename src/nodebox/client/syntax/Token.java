/*
 * Token.java - Generic token
 * Copyright (C) 1998, 1999 Slava Pestov
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

/**
 * A linked list of tokens. Each token has three fields - a token
 * identifier, which is a byte value that can be looked up in the
 * array returned by <code>SyntaxDocument.getColors()</code>
 * to get a color value, a length value which is the length of the
 * token in the text, and a pointer to the next token in the list.
 *
 * @author Slava Pestov
 * @version $Id: Token.java,v 1.4 2003/06/30 17:31:10 blaisorblade Exp $
 */
public class Token {
    /**
     * Normal text token id. This should be used to mark
     * normal text.
     */
    public static final byte NULL = 0;

    /**
     * Comment 1 token id. This can be used to mark a comment.
     */
    public static final byte COMMENT1 = 1;

    /**
     * Comment 2 token id. This can be used to mark a comment.
     */
    public static final byte COMMENT2 = 2;


    /**
     * Literal 1 token id. This can be used to mark a string
     * literal (eg, C mode uses this to mark "..." literals)
     */
    public static final byte LITERAL1 = 3;

    /**
     * Literal 2 token id. This can be used to mark an object
     * literal (eg, Java mode uses this to mark true, false, etc)
     */
    public static final byte LITERAL2 = 4;

    /**
     * Label token id. This can be used to mark labels
     * (eg, C mode uses this to mark ...: sequences)
     */
    public static final byte LABEL = 5;

    /**
     * Keyword 1 token id. This can be used to mark a
     * keyword. This should be used for general language
     * constructs.
     */
    public static final byte KEYWORD1 = 6;

    /**
     * Keyword 2 token id. This can be used to mark a
     * keyword. This should be used for preprocessor
     * commands, or variables.
     */
    public static final byte KEYWORD2 = 7;

    /**
     * Keyword 3 token id. This can be used to mark a
     * keyword. This should be used for data types.
     */
    public static final byte KEYWORD3 = 8;

    /**
     * Operator token id. This can be used to mark an
     * operator. (eg, SQL mode marks +, -, etc with this
     * token type)
     */
    public static final byte OPERATOR = 9;

    /**
     * Invalid token id. This can be used to mark invalid
     * or incomplete tokens, so the user can easily spot
     * syntax errors.
     */
    public static final byte INVALID = 10;

    /**
     * Methods calls.
     */
    public static final byte METHOD = 11;

    /**
     * The total number of defined token ids.
     */
    public static final byte ID_COUNT = 12;

    /**
     * The first id that can be used for internal state
     * in a token marker.
     */
    public static final byte INTERNAL_FIRST = 100;

    /**
     * The last id that can be used for internal state
     * in a token marker.
     */
    public static final byte INTERNAL_LAST = 126;

    /**
     * The token type, that along with a length of 0
     * marks the end of the token list.
     */
    public static final byte END = 127;

    /**
     * The length of this token.
     */
    public int length;

    /**
     * The id of this token.
     */
    public byte id;

    /**
     * The next token in the linked list.
     */
    public Token next;

    /**
     * If we need to highlight behind the token.
     */
    public boolean highlightBackground = false;

    /**
     * Creates a new token.
     *
     * @param length The length of the token
     * @param id     The id of the token
     */
    public Token(int length, byte id) {
        this(length, id, false);
    }

    public Token(int length, byte id, boolean highlightBackground) {
        this.length = length;
        this.id = id;
        this.highlightBackground = highlightBackground;
    }

    /**
     * Returns a string representation of this token.
     */
    public String toString() {
        return "[id=" + id + ",length=" + length + "]";
    }
}

/*
 * ChangeLog:
 * $Log: Token.java,v $
 * Revision 1.4  2003/06/30 17:31:10  blaisorblade
 * Fix for line-ends.
 *
 * Revision 1.3  2003/06/29 13:37:27  gfx
 * Support of JDK 1.4.2
 *
 * Revision 1.2  2003/02/26 21:20:42  gfx
 * New PHP highlighting feature
 *
 * Revision 1.1.1.1  2001/08/20 22:31:48  gfx
 * Jext 3.0pre5
 *
 * Revision 1.2  2001/08/04 22:11:45  gfx
 * Methods colorizing, new Python 2.2 keyword
 *
 */