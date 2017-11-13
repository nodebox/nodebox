package nodebox.function;

import au.com.bytecode.opencsv.CSVReader;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import nodebox.util.ReflectionUtils;

import java.io.*;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.*;

public class DataFunctions {


    public static final FunctionLibrary LIBRARY;
    private static final Map<String, Character> separators;
    private static final Splitter DOT_SPLITTER = Splitter.on('.');

    static {
        LIBRARY = JavaLibrary.ofClass("data", DataFunctions.class,
                "lookup", "importText", "importCSV", "filterData", "makeTable");

        separators = new HashMap<String, Character>();
        separators.put("period", '.');
        separators.put("comma", ',');
        separators.put("semicolon", ';');
        separators.put("tab", '\t');
        separators.put("space", ' ');
        separators.put("double", '"');
        separators.put("single", '\'');
    }

    /**
     * Try a number of ways to lookup a key in an object.
     * <p/>
     * If the key has dots in it, use a nested lookup.
     *
     * @param o   The object to search
     * @param key The key to find.
     * @return The value of the key if found, otherwise null.
     */
    public static Object lookup(Object o, String key) {
        if (key == null) return null;
        return nestedLookup(o, DOT_SPLITTER.split(key));
    }

    /**
     * Lookup a value in a nested structure.
     *
     * @param o    The object to search.
     * @param keys The keys to find.
     * @return The value of the key if found, otherwise null.
     */
    public static Object nestedLookup(Object o, Iterable<String> keys) {
        for (String key : keys) {
            o = fastLookup(o, key);
            if (o == null) {
                break;
            }
        }
        return o;
    }

    /**
     * Try a number of ways to lookup a key in an object.
     *
     * @param o   The object to search
     * @param key The key to find.
     * @return The value of the key if found, otherwise null.
     */
    private static Object fastLookup(Object o, String key) {
        if (o == null || key == null) return null;
        if (o instanceof Map) {
            Map m = (Map) o;
            return m.get(key);
        } else {
            return ReflectionUtils.get(o, key, null);
        }
    }

    /**
     * Import a text file as a list of strings, separated by lines.
     *
     * @param fileName The file to read in.
     * @return A list of lines.
     */
    public static List<String> importText(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) return ImmutableList.of();
        try {
            ImmutableList.Builder<String> lines = ImmutableList.builder();
            InputStreamReader in = new InputStreamReader(new FileInputStream(fileName), "UTF-8");
            BufferedReader br = new BufferedReader(in);
            for (; ; ) {
                String line = br.readLine();
                if (line == null) {
                    break;
                }
                lines.add(line);
            }
            return lines.build();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Could not decode file " + fileName + ": " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("Could not read file " + fileName + ": " + e.getMessage(), e);
        }
    }


    /**
     * Import the CSV from a file.
     * <p/>
     * This method assumes the first row is the header row. It will not be returned: instead, it will serves as the
     * keys for the maps we return.
     *
     * @param fileName           The file to read in.
     * @param delimiter          The name of the character delimiting column values.
     * @param quotationCharacter The name of the character acting as the quotation separator.
     * @param numberSeparator    The character used to separate the fractional part.
     * @return A list of maps.
     */
    public static List<Map<String, Object>> importCSV(String fileName, String delimiter, String quotationCharacter, String numberSeparator) {
        if (fileName == null || fileName.trim().isEmpty()) return ImmutableList.of();
        try {
            InputStreamReader in = new InputStreamReader(new FileInputStream(fileName), "UTF-8");
            Character sep = separators.get(delimiter);
            if (sep == null) sep = ',';
            Character quot = separators.get(quotationCharacter);
            if (quot == null) quot = '"';
            CSVReader reader = new CSVReader(in, sep, quot);
            ImmutableList.Builder<Map<String, Object>> b = ImmutableList.builder();
            String[] headers = reader.readNext();

            Map<String, Integer> headerDuplicates = new HashMap<String, Integer>();
            Map<String, Boolean> columnNumerics = new HashMap<String, Boolean>();
            List<String> tmp = new ArrayList<String>();
            for (int i = 0; i < headers.length; i++) {
                headers[i] = headers[i].trim();
                if (headers[i].isEmpty())
                    headers[i] = String.format("Column %s", i + 1);
                if (tmp.contains(headers[i]))
                    headerDuplicates.put(headers[i], 0);
                tmp.add(headers[i]);
            }

            for (int i = 0; i < headers.length; i++) {
                String header = headers[i];
                if (headerDuplicates.get(header) != null) {
                    int number = headerDuplicates.get(header) + 1;
                    headers[i] = header + " " + number;
                    headerDuplicates.put(header, number);
                }
                columnNumerics.put(headers[i], null);
            }

            String[] row;

            NumberFormat nf = NumberFormat.getNumberInstance(numberSeparator.equals("comma") ? Locale.GERMANY : Locale.US);
            while ((row = reader.readNext()) != null) {
                ImmutableMap.Builder<String, Object> mb = ImmutableMap.builder();
                for (int i = 0; i < row.length; i++) {
                    String header = i < headers.length ? headers[i] : String.format("Column %s", i + 1);
                    if (!columnNumerics.containsKey(header))
                        columnNumerics.put(header, null);

                    String v = row[i].trim();
                    try {
                        ParsePosition position = new ParsePosition(0);
                        nf.parse(v, position).doubleValue();
                        if (columnNumerics.get(header) == null)
                            columnNumerics.put(header, true);
                        if (position.getIndex() != v.length()) {
                            throw new ParseException("Failed to parse entire string: " + v, position.getIndex());
                        }
                    } catch (ParseException e) {
                        columnNumerics.put(header, false);
                    } catch (NullPointerException e) {
                        columnNumerics.put(header, false);
                    }
                    mb.put(header, v);
                }
                b.add(mb.build());
            }
            List<Map<String, Object>> tempRows = b.build();

            List<String> headersNumericCols = new ArrayList<String>();
            for (String header : columnNumerics.keySet()) {
                if (columnNumerics.get(header) == true)
                    headersNumericCols.add(header);
            }

            if (headersNumericCols.isEmpty()) return tempRows;

            b = ImmutableList.builder();
            for (Map<String, Object> r : tempRows) {
                ImmutableMap.Builder<String, Object> mb = ImmutableMap.builder();
                for (String header : r.keySet()) {
                    if (headersNumericCols.contains(header)) {
                        try {
                            double d = nf.parse(((String) r.get(header))).doubleValue();
                            mb.put(header, d);
                        } catch (ParseException e) {
                        }
                    } else {
                        mb.put(header, r.get(header));
                    }
                }
                b.add(mb.build());
            }
            return b.build();
        } catch (IOException e) {
            throw new RuntimeException("Could not read file " + fileName + ": " + e.getMessage(), e);
        }
    }

    public static List<Object> filterData(List<Object> rows, String key, String op, Object value) {
        if (value == null) return rows;
        ImmutableList.Builder<Object> b = ImmutableList.builder();
        try {
            double floatValue = Double.parseDouble(value.toString());
            for (Object o : rows) {
                if (doubleMatches(o, key, op, floatValue)) {
                    b.add(o);
                }
            }
        } catch (NumberFormatException e) {
            for (Object o : rows) {
                if (objectMatches(o, key, op, value)) {
                    b.add(o);
                }
            }
        }
        return b.build();
    }

    private static boolean objectMatches(Object o, String key, String op, Object value) {
        if (value == null) return false;
        Object v = fastLookup(o, key);
        if (op.equals("=") && value.equals(v)) {
            return true;
        } else if (op.equals("!=") && !value.equals(v)) {
            return true;
        } else {
            return false;
        }
    }

    private static boolean doubleMatches(Object o, String key, String op, double value) {
        Object v = fastLookup(o, key);
        if (v == null) return false; // TODO is this the best thing?
        double dv;
        if (v instanceof Double) {
            dv = (Double) v;
        } else if (v instanceof String) {
            try {
                dv = Double.parseDouble(v.toString());
            } catch (NumberFormatException e) {
                // We use a sentinel here so that it can still be non-equal.
                dv = Double.MAX_VALUE;
            }
        } else if (v instanceof Long) {
            dv = (double) (Long) v;
        } else if (v instanceof Integer) {
            dv = (double) (Integer) v;
        } else if (v instanceof Float) {
            dv = (double) (Float) v;
        } else {
            return false;
        }

        if (op.equals("=") && dv == value) {
            return true;
        } else if (op.equals("!=") && dv != value) {
            return true;
        } else if (op.equals(">") && dv > value) {
            return true;
        } else if (op.equals(">=") && dv >= value) {
            return true;
        } else if (op.equals("<") && dv < value) {
            return true;
        } else if (op.equals("<=") && dv <= value) {
            return true;
        } else {
            return false;
        }
    }

    public static List<Map<String, Object>> makeTable(String headers, List<?> l1, List<?> l2, List<?> l3, List<?> l4, List<?> l5, List<?> l6) {
        List<String> dirtyHeaderList = Arrays.asList(headers.split("[,;]"));
        ArrayList<String> headerList = new ArrayList<>(6);
        int dirtyHeaderListSize = dirtyHeaderList.size();
        for (int i = 0; i < 6; i++) {
            String key = i < dirtyHeaderListSize ? dirtyHeaderList.get(i) : null;
            if (key == null) {
                key = "list" + (i + 1);
            } else {
                key = key.trim();
                if (key.length() == 0) {
                    key = "list" + (i + 1);
                }
            }
            headerList.add(key);
        }

        ArrayList<List<?>> lists = new ArrayList<>(6);
        lists.add(l1);
        lists.add(l2);
        lists.add(l3);
        lists.add(l4);
        lists.add(l5);
        lists.add(l6);
        int colCount = nonEmptyListSize(lists);
        int rowCount = maxListSize(lists);
        ImmutableList.Builder<Map<String, Object>> result = new ImmutableList.Builder<>();
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            ImmutableMap.Builder<String, Object> row = ImmutableMap.builder();
            for (int colIndex = 0; colIndex < colCount; colIndex++) {
                String key = headerList.get(colIndex);
                List<?> l = lists.get(colIndex);
                if (l != null && !l.isEmpty()) {
                    row.put(key, rowIndex < l.size() ? l.get(rowIndex) : "");
                }
            }
            result.add(row.build());
        }
        return result.build();
    }

    private static int nonEmptyListSize(List<List<?>> lists) {
        int index = -1;
        for (int i = 0; i < lists.size(); i++) {
            if (lists.get(i) != null && !lists.get(i).isEmpty()) {
                index = i;
            }
        }
        return index + 1;
    }

    private static int maxListSize(List<List<?>> lists) {
        int maxSize = 0;
        for (List<?> l : lists) {
            if (l != null) {
                maxSize = Math.max(maxSize, l.size());
            }
        }
        return maxSize;
    }

}
