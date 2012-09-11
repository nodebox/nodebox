package nodebox.function;

import au.com.bytecode.opencsv.CSVReader;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import nodebox.util.ReflectionUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

public class DataFunctions {


    public static final FunctionLibrary LIBRARY;

    static {
        LIBRARY = JavaLibrary.ofClass("data", DataFunctions.class,
                "lookup", "importCSV");
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
     * @param fileName The file to read in.
     * @return A list of maps.
     */
    public static List<Map<String, Object>> importCSV(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) return ImmutableList.of();
        try {
            InputStreamReader in = new InputStreamReader(new FileInputStream(fileName), "UTF-8");
            CSVReader reader = new CSVReader(in);
            ImmutableList.Builder<Map<String, Object>> b = ImmutableList.builder();
            String[] headers = reader.readNext();
            for (int i = 0; i < headers.length; i++) {
                headers[i] = headers[i].trim();
                if (headers[i].isEmpty())
                    headers[i] = String.format("Column %s", i + 1);
            }
            String[] row;

            while ((row = reader.readNext()) != null) {
                ImmutableMap.Builder<String, Object> mb = ImmutableMap.builder();
                for (int i = 0; i < row.length; i++) {
                    String header = i < headers.length ? headers[i] : String.format("Column %s", i + 1);
                    String v = row[i].trim();
                    Object value;
                    try {
                        value = Double.valueOf(v);
                    } catch (NumberFormatException e) {
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
