package nodebox.function;

import au.com.bytecode.opencsv.CSVReader;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import nodebox.util.ReflectionUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;

public class DataFunctions {


    public static final FunctionLibrary LIBRARY;
    private static final Map<String, Character> separators;

    static {
        LIBRARY = JavaLibrary.ofClass("data", DataFunctions.class,
                "lookup", "importCSV");

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
     *
     * @param o   The object to search
     * @param key The key to find.
     * @return The value of the key if found, otherwise null.
     */
    public static Object lookup(Object o, String key) {
        if (o == null || key == null) return null;
        if (o instanceof Map) {
            Map m = (Map) o;
            return m.get(key);
        } else {
            return ReflectionUtils.get(o, key, null);
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
     * @return A list of maps.
     */
    public static List<Map<String, Object>> importCSV(String fileName, String delimiter, String quotationCharacter, String floatSeparator) {
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
            }

            String[] row;

            while ((row = reader.readNext()) != null) {
                ImmutableMap.Builder<String, Object> mb = ImmutableMap.builder();
                for (int i = 0; i < row.length; i++) {
                    String header = i < headers.length ? headers[i] : String.format("Column %s", i + 1);
                    String v = row[i].trim();
                    Object value;
                    try {
                        NumberFormat nf = NumberFormat.getInstance(floatSeparator.equals("comma") ? Locale.GERMANY : Locale.US);
                        value = nf.parse(v).doubleValue();
                    } catch (ParseException e) {
                        value = v;
                    }
                    mb.put(header, value);
                }
                b.add(mb.build());
            }

            return b.build();
        } catch (IOException e) {
            throw new RuntimeException("Could not read file " + fileName + ": " + e.getMessage(), e);
        }
    }

}
