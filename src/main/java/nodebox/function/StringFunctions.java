package nodebox.function;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import nodebox.util.StringUtils;

import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

/**
 * Library with functions for String manipulation.
 */
public class StringFunctions {

    public static final FunctionLibrary LIBRARY;

    static {
        LIBRARY = JavaLibrary.ofClass("string", StringFunctions.class,
        "string", "makeStrings", "length", "wordCount", "concatenate", "changeCase", "formatNumber", 
        "characters", "randomCharacter", "asBinaryString", "asBinaryList", "asNumberList", "countCharacters", 
        "characterAt", "contains", "endsWith", "equal", "replace", "startsWith", "subString", "trim" );
    }

    /**
     * Return the input string, as-is.
     * <p/>
     * This function is used for using strings as variables in NodeBox.
     */
    public static String string(String s) {
        return s;
    }

    /**
     * Make a list of strings from a big string with separators.
     * Whitespace is not stripped.
     *
     * @param s         The input string, e.g. "a;b;c"
     * @param separator The separator, e.g. ";". If the separator is empty, return each character separately.
     * @return A list of strings.
     */
    public static List<String> makeStrings(String s, String separator) {
        if (s == null) {
            return ImmutableList.of();
        }
        if (separator == null || separator.isEmpty()) {
            return ImmutableList.copyOf(Splitter.fixedLength(1).split(s));
        }
        return ImmutableList.copyOf(Splitter.on(separator).split(s));
    }
    
    public static int length(String s) {
        if (s == null) return 0;
        return s.length();
    }

    public static int wordCount(String s) {
        if (s == null) return 0;
        Iterable<String> split = Splitter.onPattern("\\w+").split(s);
        return Iterables.size(split) - 1;
    }

    public static String concatenate(String s1, String s2, String s3, String s4, String s5, String s6, String s7) {
        StringBuilder sb = new StringBuilder();
        if (s1 != null) {
            sb.append(s1);
        }
        if (s2 != null) {
            sb.append(s2);
        }
        if (s3 != null) {
            sb.append(s3);
        }
        if (s4 != null) {
            sb.append(s4);
        }
        if (s5 != null) {
            sb.append(s5);
        }
        if (s6 != null) {
            sb.append(s6);
        }
        if (s7 != null) {
            sb.append(s7);
        }
        return sb.toString();
    }

    public static String changeCase(String value, String caseMethod) {
        caseMethod = caseMethod.toLowerCase();
        if (caseMethod.equals("lowercase")) {
            return value.toLowerCase();
        } else if (caseMethod.equals("uppercase")) {
            return value.toUpperCase();
        } else if (caseMethod.equals("titlecase")) {
            return StringUtils.toTitleCase(value);
        } else {
            return value;
        }
    }

    public static String formatNumber(double value, String format) {
        return String.format(Locale.US, format, value);
    }

    /**
     * split the string into a list of characters
     * this duplicates some of the functionalility of makeStrings
     * added because it wasn't obvious that makeString would give
     * you characters when no seperator was present
     */
    public static List<String> characters(String s) {
        if (s == null) {
            return ImmutableList.of();
        }
        return ImmutableList.copyOf(Splitter.fixedLength(1).split(s));
    }
    
    /**
     * generates a list of random characters 
     * characters pulled from characterSet
     */
    public static List<String> randomCharacter(String characterSet, long amount, long seed) {
        List<String> result = new ArrayList<String>();
        Random r = new Random(seed * 1000000000);
        
        for (long i = 0; i < amount; i++) {
            int index = (int)(r.nextDouble() * characterSet.length());
            result.add( String.valueOf(characterSet.charAt(index)) );
        }
        return result;
    }
    
    /**
     * generates a formatted binary string
     * converts strings into a series of bytes
     * then converts those bytes into a binary representation
     * with proper zero padding
     */
    public static String asBinaryString(String s, String digitSep, String byteSep) {
        if (s ==null) {
            return s;
        }
        byte[] bytes = s.getBytes();
        StringBuilder result = new StringBuilder();

        for (byte b : bytes) {
            int val = b;
            for (int i = 0; i < 8; i++) {
                result.append( ((val & 128) == 0 ? "0" : "1") );
                if( i < 7) {
                    result.append( digitSep );
                }
                val <<= 1;
            }
            result.append( byteSep );
        }
        return result.toString();
    }
    
    /**
     * generates a list of binary values from a string
     * converts strings into a series of bytes
     * then converts those bytes into a binary representation
     * with proper zero padding
     */
    public static List<String> asBinaryList(String s) {
        List<String> result = new ArrayList<String>();
        if (s == null) {
            return result;
        }
        byte[] bytes = s.getBytes();
        
        for (byte b : bytes) {
            int val = b;
            for (int i = 0; i < 8; i++) {
                result.add( (val & 128) == 0 ? "0" : "1" );
                val <<= 1;
            }
        }
        return result;
    }

    /**
     * generates a list of strings (number representation) from a string
     * converts strings into a series of bytes
     * then converts those bytes into a numeric representation
     * numbers are converted into a given base/radix
     * checked to base/radix 20
     * optional zero padding
     */
    public static List<String> asNumberList(String s, long radix, boolean padding) {
        List<String> numberList = new ArrayList<String>();
        if ((radix < 2) || (s == null)) {
            return numberList;
        }
        
        byte[] bytes = s.getBytes();
        
        if (padding) {
            if (radix == 2) { // binary
                for (byte b : bytes) {
                    int cval = b;
                    StringBuilder result = new StringBuilder();
                    for (int i = 0; i < 8; i++) {
                        result.append( (cval & 128) == 0 ? "0" : "1" );
                        cval <<= 1;
                    }
                    numberList.add( result.toString() );
                }

            } else if (radix == 3) {
                for (byte b : bytes) {
                    int val = Integer.parseInt( Integer.toString( b, (int)radix ) );
                    numberList.add( String.format("%06d", val) );
                }

            } else if ((radix > 3) && (radix < 7) ) {
                for (byte b : bytes) {
                    int val = Integer.parseInt( Integer.toString( b, (int)radix ) );
                    numberList.add( String.format("%04d", val) );
                }

            } else if ((radix < 15) ) {
                for (byte b : bytes) {
                    StringBuilder result = new StringBuilder( Integer.toString( b, (int)radix ) );
                    for (int i = result.length(); i < 3; i++) {
                        result.insert( 0, "0" ); // zero pad the beginning of the string
                    }
                    numberList.add( result.toString() );
                }

            } else {
                for (byte b : bytes) {
                    StringBuilder result = new StringBuilder( Integer.toString( b, (int)radix ) );
                    for (int i = result.length(); i < 2; i++) {
                        result.insert( 0, "0" ); // zero pad the beginning of the string
                    }
                    numberList.add( result.toString() );
                }
            }
        } else {
            if (radix == 2) { // binary
                for (byte b : bytes) {
                    int cval = b;
                    StringBuilder result = new StringBuilder();
                    for (int i = 0; i < 8; i++) {
                        result.append( (cval & 128) == 0 ? "0" : "1" );
                        cval <<= 1;
                    }
                    numberList.add( result.toString() );
                }
            } else {
                for (byte b : bytes) {
                    numberList.add( Integer.toString( b, (int)radix ) );
                }
            }
        }
        return numberList;
    }

    /**
     * output the character at a given index
     */
    public static String characterAt(String s, long index) {
        if (s == null || s.isEmpty()) {
            return s;
        }

        if (index < 0) {
            index = s.length() + index;
        }

        if (index >= s.length() || index < 0) { return ""; }

        return String.valueOf(s.charAt((int) index));
    }

    /**
     * output a list of characters:count pairs 
     */
    public static String countCharacters(String s) {
        // TODO
        return s;
    }

    /**
     * Determine if the string contains a given string
     */
    public static boolean contains(String s, String value) {
        if ((s==null) || (value==null)) {
            return false;
        }
        return s.contains(value);
    }

    /**
     * Determine if the string end with a given string
     */
    public static boolean endsWith(String s, String value) {
        if ((s==null) || (value==null)) {
            return false;
        }
        return s.endsWith(value);
    }

    /**
     * Determine if the string equals a given string
     * optional case sensitivity
     * function renamed to equal because of name conflict
     */
    public static boolean equal(String s, String value, boolean caseSensitive) {
        if ((s==null) || (value==null)) {
            return false;
        }
        if (caseSensitive) {
            return s.equals(value);
        } else {
            return s.equalsIgnoreCase(value);
        }
    }

    /**
     * Replace part of a string
     */
    public static String replace(String s, String oldVal, String newVal) {
        if ((oldVal==null) || (newVal==null)) {
            return s;
        }
        return s.replace(oldVal, newVal);
    }
    
    /**
     * Determine if the string starts with a given string
     */
    public static boolean startsWith(String s, String value) {
        if ((s==null) || (value==null)) {
            return false;
        }
        return s.startsWith(value);
    }

    /**
     * Output a portion of a string
     * start and end values are indices of the characters
     * endOffset controls whether the character at the end position should be included or not
     */
    public static String subString(String s, long start, long end, boolean endOffset) {
        if (s == null) {
            return s;
        }

        if (end < start) { return ""; }

        if (start < 0 && end < 0) {
            start = s.length() + start;
            end = s.length() + end;
        }

        if (endOffset) {
            end++;
        }

        return s.substring((int) Math.max(0, start), (int) Math.min(s.length(), end));
    }
    
    /**
     * Remove white space from the start and end
     */
    public static String trim(String s) {
        if (s==null) {
            return s;
        }
        return s.trim();
    }

}
