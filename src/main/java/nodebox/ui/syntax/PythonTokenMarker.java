/*
 * PythonTokenMarker.java - Python token marker
 * Copyright (C) 1999 Jonathan Revusky
 * Copyright (C) 2001 Romain Guy
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
 * Python token marker.
 *
 * @author Jonathan Revusky, Romain Guy
 * @version $Id: PythonTokenMarker.java,v 1.5 2001/12/01 18:54:31 gfx Exp $
 */
public class PythonTokenMarker extends TokenMarker {
    public static final String METHOD_DELIMITERS = " \t~!%^*()-+=|\\#/{}[]:;\"'<>,.?@";

    private static final byte TRIPLEQUOTE1 = Token.INTERNAL_FIRST;
    private static final byte TRIPLEQUOTE2 = Token.INTERNAL_LAST;

    public PythonTokenMarker() {
        this.keywords = getKeywords();
    }

    public byte markTokensImpl(byte token, Segment line, int lineIndex) {
        char[] array = line.array;
        int offset = line.offset;
        lastOffset = offset;
        lastKeyword = offset;
        lastWhitespace = offset - 1;
        int length = line.count + offset;
        boolean backslash = false;

        loop:
        for (int i = offset; i < length; i++) {
            int i1 = (i + 1);

            char c = array[i];
            if (c == '\\') {
                backslash = !backslash;
                continue;
            }

            switch (token) {
                case Token.NULL:
                    switch (c) {
                        case '(':
                            if (backslash) {
                                doKeyword(line, i, c);
                                backslash = false;
                            } else {
                                if (doKeyword(line, i, c))
                                    break;
                                addToken(lastWhitespace - lastOffset + 1, token);
                                addToken(i - lastWhitespace - 1, Token.METHOD);
                                addToken(1, Token.NULL);
                                token = Token.NULL;
                                lastOffset = lastKeyword = i1;
                                lastWhitespace = i;
                            }
                            break;
                        case '#':
                            if (backslash)
                                backslash = false;
                            else {
                                doKeyword(line, i, c);
                                addToken(i - lastOffset, token);
                                addToken(length - i, Token.COMMENT1);
                                lastOffset = lastKeyword = length;
                                break loop;
                            }
                            break;
                        case '"':
                            doKeyword(line, i, c);
                            if (backslash)
                                backslash = false;
                            else {
                                addToken(i - lastOffset, token);
                                if (SyntaxUtilities.regionMatches(false, line, i1, "\"\"")) {
                                    lastOffset = lastKeyword = i;
                                    i += 3;
                                    token = TRIPLEQUOTE1;
                                } else {
                                    token = Token.LITERAL1;
                                    lastOffset = lastKeyword = i;
                                }
                            }
                            break;
                        case '\'':
                            doKeyword(line, i, c);
                            if (backslash)
                                backslash = false;
                            else {
                                addToken(i - lastOffset, token);
                                if (SyntaxUtilities.regionMatches(false, line, i1, "''")) {
                                    lastOffset = lastKeyword = i;
                                    i += 3;
                                    token = TRIPLEQUOTE2;
                                } else {
                                    token = Token.LITERAL2;
                                    lastOffset = lastKeyword = i;
                                }
                            }
                            break;
                        default:
                            backslash = false;
                            if (!Character.isLetterOrDigit(c) && c != '_')
                                doKeyword(line, i, c);
                            if (METHOD_DELIMITERS.indexOf(c) != -1) {
                                lastWhitespace = i;
                            }
                            break;
                    }
                    break;
                case Token.LITERAL1:
                    if (backslash)
                        backslash = false;
                    else if (c == '"') {
                        addToken(i1 - lastOffset, token);
                        token = Token.NULL;
                        lastOffset = lastKeyword = i1;
                    }
                    break;
                case Token.LITERAL2:
                    if (backslash)
                        backslash = false;
                    else if (c == '\'') {
                        addToken(i1 - lastOffset, Token.LITERAL1);
                        token = Token.NULL;
                        lastOffset = lastKeyword = i1;
                    }
                    break;
                case TRIPLEQUOTE1:
                    if (SyntaxUtilities.regionMatches(false, line, i, "\"\"\"")) {
                        addToken((i += 3) - lastOffset, Token.LITERAL2);
                        token = Token.NULL;
                        lastOffset = lastKeyword = i;
                    }
                    break;
                case TRIPLEQUOTE2:
                    if (SyntaxUtilities.regionMatches(false, line, i, "'''")) {
                        addToken((i += 3) - lastOffset, Token.LITERAL2);
                        token = Token.NULL;
                        lastOffset = lastKeyword = i;
                    }
                    break;
                default:
                    throw new InternalError("Invalid state: " + token);
            }
        }

        switch (token) {
            case Token.LITERAL1:
            case Token.LITERAL2:
                addToken(length - lastOffset, Token.INVALID);
                token = Token.NULL;
                break;
            case TRIPLEQUOTE1:
            case TRIPLEQUOTE2:
                addToken(length - lastOffset, Token.LITERAL2);
                break;
            case Token.NULL:
                doKeyword(line, length, '\0');
            default:
                addToken(length - lastOffset, token);
                break;
        }

        return token;
    }

    public static KeywordMap getKeywords() {
        if (pyKeywords == null) {
            pyKeywords = new KeywordMap(false);
            pyKeywords.add("and", Token.KEYWORD1);
            pyKeywords.add("not", Token.KEYWORD1);
            pyKeywords.add("or", Token.KEYWORD1);
            pyKeywords.add("if", Token.KEYWORD1);
            pyKeywords.add("yield", Token.KEYWORD1);
            pyKeywords.add("for", Token.KEYWORD1);
            pyKeywords.add("assert", Token.KEYWORD1);
            pyKeywords.add("break", Token.KEYWORD1);
            pyKeywords.add("continue", Token.KEYWORD1);
            pyKeywords.add("elif", Token.KEYWORD1);
            pyKeywords.add("else", Token.KEYWORD1);
            pyKeywords.add("except", Token.KEYWORD1);
            pyKeywords.add("exec", Token.KEYWORD1);
            pyKeywords.add("finally", Token.KEYWORD1);
            pyKeywords.add("raise", Token.KEYWORD1);
            pyKeywords.add("return", Token.KEYWORD1);
            pyKeywords.add("try", Token.KEYWORD1);
            pyKeywords.add("while", Token.KEYWORD1);

            pyKeywords.add("def", Token.KEYWORD2);
            pyKeywords.add("class", Token.KEYWORD2);
            pyKeywords.add("lambda", Token.KEYWORD2);
            pyKeywords.add("del", Token.KEYWORD2);
            pyKeywords.add("from", Token.KEYWORD2);
            pyKeywords.add("global", Token.KEYWORD2);
            pyKeywords.add("import", Token.KEYWORD2);
            pyKeywords.add("in", Token.KEYWORD2);
            pyKeywords.add("is", Token.KEYWORD2);
            pyKeywords.add("pass", Token.KEYWORD2);
            pyKeywords.add("print", Token.KEYWORD2);

            pyKeywords.add("self", Token.LITERAL2);

            pyKeywords.add("__dict__", Token.LABEL);
            pyKeywords.add("__methods__", Token.LABEL);
            pyKeywords.add("__members__", Token.LABEL);
            pyKeywords.add("__class__", Token.LABEL);
            pyKeywords.add("__bases__", Token.LABEL);
            pyKeywords.add("__name__", Token.LABEL);

            // Add all commands and classes available in the grobs API.
            /*
            for (int i = 0; i < Grob.COMMANDS.length; i++) {
                pyKeywords.add(Grob.COMMANDS[i], Token.KEYWORD3);
            }
            for (int i = 0; i < Grob.CLASSES.length; i++) {
                pyKeywords.add(Grob.CLASSES[i], Token.KEYWORD3);
            }
            */
        }
        return pyKeywords;
    }

    // private members
    private static KeywordMap pyKeywords;

    private KeywordMap keywords;
    private int lastOffset;
    private int lastKeyword;
    private int lastWhitespace;

    private boolean doKeyword(Segment line, int i, char c) {
        int i1 = i + 1;

        int len = i - lastKeyword;
        byte id = keywords.lookup(line, lastKeyword, len);
        if (id != Token.NULL) {
            if (lastKeyword != lastOffset)
                addToken(lastKeyword - lastOffset, Token.NULL);
            addToken(len, id);
            lastOffset = i;
            lastWhitespace = i1;
            lastKeyword = i1;
            return true;
        }
        lastKeyword = i1;
        return false;
    }
}