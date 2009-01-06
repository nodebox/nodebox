/*
 * This file is part of NodeBox.
 *
 * Copyright (C) 2008 Frederik De Bleser (frederik@pandora.be)
 *
 * NodeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NodeBox is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NodeBox. If not, see <http://www.gnu.org/licenses/>.
 */

package net.nodebox.util;

import java.util.List;

public class StringUtils {

    public static String humanizeName(String name) {
        StringBuffer sb = new StringBuffer();
        String[] tokens = name.split("_");
        for (String t : tokens) {
            if (t.length() == 0) continue;
            sb.append(t.substring(0, 1).toUpperCase());
            sb.append(t.substring(1));
            sb.append(" ");
        }
        return sb.toString().trim();
    }

    public static String join(List items, String separator) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            boolean lastItem = i == items.size() - 1;
            sb.append(items.get(i));
            if (!lastItem) {
                sb.append(separator);
            }
        }
        return sb.toString();
    }

    /**
     * Given a position of a numeric character a text stream, finds and returns the left and right edges of the
     * entire number.
     *
     * @param text
     * @param index
     * @return int[]{left, right} or null
     */
    public static int[] numberRange(String text, int index) {
        if (index < 0 || index >= text.length()) return null;
        if (!isNumeric(text.charAt(index))) return null;
        int left = index;
        int right = index;
        // Left edge
        while (left > 0 && isNumeric(text.charAt(left - 1))) {
            left--;
        }
        // Right edge
        while (right < text.length() - 1 && isNumeric(text.charAt(right + 1))) {
            right++;
        }
        return new int[]{left, right};
    }

    /**
     * Given the numberRange obtained by numberRange, returns the number as a string.
     *
     * @param text        full text
     * @param numberRange range obtained by numberRange method
     * @return a String containing the number
     * @see #numberRange(String, int)
     */
    public static String numberForRange(String text, int[] numberRange) {
        // substring returns a string that doesn't contain the end character,
        // so add one to the range.
        return text.substring(numberRange[0], numberRange[1] + 1);
    }

    /**
     * Returns the true if the given char is a number (between 0 and 9)
     *
     * @param c the character to test
     * @return true if the given char is a number, false otherwise.
     */
    public static boolean isNumeric(char c) {
        return c >= '0' && c <= '9';
    }

}
