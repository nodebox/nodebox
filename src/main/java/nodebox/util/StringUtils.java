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

package nodebox.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StringUtils {

    public static String humanizeName(String name) {
        StringBuilder sb = new StringBuilder();
        String[] tokens = name.split("_");
        for (String t : tokens) {
            if (t.length() == 0) continue;
            sb.append(t.substring(0, 1).toUpperCase(Locale.US));
            sb.append(t.substring(1));
            sb.append(" ");
        }
        return sb.toString().trim();
    }

    public static String humanizeConstant(String constant) {
        return humanizeName(constant.toLowerCase(Locale.US));
    }

    public static String toTitleCase(String value) {
        StringBuilder titleCase = new StringBuilder();
        boolean nextTitleCase = true;

        for (char c : value.toCharArray()) {
            if (Character.isSpaceChar(c)) {
                nextTitleCase = true;
            } else if (nextTitleCase) {
                c = Character.toTitleCase(c);
                nextTitleCase = false;
            }

            titleCase.append(c);
        }

        return titleCase.toString();
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

    public static String join(String charString, String separator) {
        List<Character> lst = new ArrayList<Character>();
        for (Character c : charString.toCharArray())
            lst.add(c);
        return join(lst, separator);
    }

}
