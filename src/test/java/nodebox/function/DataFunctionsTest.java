package nodebox.function;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import java.awt.*;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.*;
import static nodebox.function.DataFunctions.importCSV;
import static nodebox.function.DataFunctions.lookup;
import static nodebox.function.DataFunctions.makeTable;
import static nodebox.util.Assertions.assertResultsEqual;

public class DataFunctionsTest {

    @Test
    public void testLookupNull() {
        assertNull(lookup(null, "xxx"));
        assertNull(lookup(new Point(11, 22), null));
    }

    @Test
    public void testLookupInMap() {
        Map<String, Integer> greek = ImmutableMap.of("alpha", 1, "beta", 2, "gamma", 3);
        assertEquals(1, lookup(greek, "alpha"));
        assertEquals(2, lookup(greek, "beta"));
        assertNull(lookup(greek, "xxx"));
    }

    @Test
    public void testLookupInObject() {
        Point awtPoint = new Point(11, 22);
        assertEquals(11.0, lookup(awtPoint, "x"));
        assertEquals(22.0, lookup(awtPoint, "y"));
        assertNull(lookup(awtPoint, "xxx"));
    }

    @Test
    public void testNestedLookup() {
        Map<String, Integer> m = ImmutableMap.of("alpha", 1, "beta", 2, "gamma", 3);
        Map<String, Map<String, Integer>> mm = ImmutableMap.of("greek", m);
        assertEquals(1, lookup(mm, "greek.alpha"));
        assertEquals(2, lookup(mm, "greek.beta"));
        assertNull(lookup(mm, "greek.xxx"));
        assertNull(lookup(mm, "greek.alpha.test"));
    }

    @Test
    public void testImportCSV() {
        List<Map<String, Object>> l = importSimpleCSV("src/test/files/colors.csv");
        assertEquals(5, l.size());
        Map<String, Object> black = l.get(0);
        assertResultsEqual(black.keySet(), "Name", "Red", "Green", "Blue");
        assertEquals("Black", black.get("Name"));
        // Numerical data is automatically converted to doubles.
        assertEquals(0.0, black.get("Red"));
    }

    @Test
    public void testImportCSVUnicode() {
        List<Map<String, Object>> l = importSimpleCSV("src/test/files/unicode.csv");
        assertEquals(2, l.size());
        Map<String, Object> frederik = l.get(0);
        assertResultsEqual(frederik.keySet(), "Name", "Age");
        assertEquals("Fr\u00e9d\u00ebr\u00eck", frederik.get("Name"));
        Map<String, Object> bob = l.get(1);
        assertEquals("B\u00f8b", bob.get("Name"));
    }

    @Test
    public void testImportEmptyCSV() {
        List l = importSimpleCSV(null);
        assertTrue(l.isEmpty());
    }

    @Test(expected = RuntimeException.class)
    public void testImportNonexistentCSV() {
        importSimpleCSV("blah/blah.csv");
    }

    @Test
    public void testImportCSVWithWhitespace() {
        List<Map<String, Object>> l = importSimpleCSV("src/test/files/whitespace.csv");
        assertEquals(2, l.size());
        Map<String, Object> alice = l.get(0);
        assertResultsEqual(alice.keySet(), "Name", "Age");
        assertEquals("Alice", alice.get("Name"));
        // Numerical data is automatically converted to doubles.
        assertEquals(41.0, alice.get("Age"));
    }

    @Test
    public void testImportCSVWithBadHeaders() {
        List<Map<String, Object>> l = importSimpleCSV("src/test/files/bad-headers.csv");
        assertEquals(2, l.size());
        Map<String, Object> row1 = l.get(0);
        assertResultsEqual(row1.keySet(), "Alpha", "Column 2", "Column 3");
        assertResultsEqual(row1.values(), 1.0, 2.0, 3.0);
    }

    @Test
    public void testImportCSVWithDuplicateHeaders() {
        List<Map<String, Object>> l = importSimpleCSV("src/test/files/duplicate-headers.csv");
        assertEquals(2, l.size());
        Map<String, Object> row1 = l.get(0);
        assertResultsEqual(row1.keySet(), "Strings", "Numbers 1", "Integers", "Numbers 2", "Floats");
        assertResultsEqual(row1.values(), 1.0, 2.0, 3.0, 4.0, 5.0);
    }

    @Test
    public void testImportCSVWithDifferentFloatingPoint() {
        List<Map<String, Object>> l = importCSV("src/test/files/floats.csv", "semicolon", "double", "period");
        assertEquals(2, l.size());
        assertResultsEqual(l.get(0).values(), 2.5, 10.99, "40,000.60", 10000.10, "10.000,10");
        assertResultsEqual(l.get(1).values(), 25.0, 1099.0, "40.000,60", 20200.20, "20.200,20");
        l = importCSV("src/test/files/floats.csv", "semicolon", "double", "comma");
        assertResultsEqual(l.get(0).values(), 25.0, 1099.0, "40,000.60", "10,000.10", 10000.10);
        assertResultsEqual(l.get(1).values(), 2.5, 10.99, "40.000,60", "20,200.20", 20200.20);
    }

    @Test
    public void testImportCSVWithMixedInput() {
        List<Map<String, Object>> l = importSimpleCSV("src/test/files/mixed-input.csv");
        assertEquals(4, l.size());
        assertResultsEqual(l.get(0).values(), 100.0, "0", "zero", 0.0);
        assertResultsEqual(l.get(1).values(), 255.0, "100", "0", 255.0);
        assertResultsEqual(l.get(2).values(), 0.0, "100 k", "0", 255.0);
        assertResultsEqual(l.get(3).values(), 100.0, "200", "255.0", 0.0);
    }

    @Test
    public void testMakeTable() {
        List<String> alphaList = ImmutableList.of("a0", "a1");
        List<String> betaList = ImmutableList.of("b0", "b1");
        List<Map<String, Object>> l = makeTable("alpha,beta", alphaList, betaList, null, null, null, null);
        assertEquals(2, l.size());
        assertEquals(l.get(0).keySet(), ImmutableSet.of("alpha", "beta"));
        assertResultsEqual(l.get(0).values(), "a0", "b0");
        assertResultsEqual(l.get(1).values(), "a1", "b1");
    }

    @Test
    public void testMakeTableWithNoData() {
        List<Map<String, Object>> l = makeTable("alpha,beta", null, null, null, null, null, null);
        assertEquals(0, l.size());
    }

    @Test
    public void testMakeTableAutoHeaders() {
        List<String> alphaList = ImmutableList.of("a0", "a1");
        List<String> betaList = ImmutableList.of("b0", "b1");
        List<Map<String, Object>> l = makeTable("alpha", alphaList, betaList, null, null, null, null);
        assertEquals(2, l.size());
        assertEquals(l.get(0).keySet(), ImmutableSet.of("alpha", "list2"));
    }

    @Test
    public void testMakeTableHeadersWithSpaces() {
        List<String> alphaList = ImmutableList.of("a0", "a1");
        List<String> betaList = ImmutableList.of("b0", "b1");
        List<Map<String, Object>> l = makeTable("alpha; beta ", alphaList, betaList, null, null, null, null);
        assertEquals(l.get(0).keySet(), ImmutableSet.of("alpha", "beta"));
    }

    @Test
    public void testMakeTableDifferentSizedLists() {
        List<String> alphaList = ImmutableList.of("a0", "a1");
        List<String> betaList = ImmutableList.of("b0");
        List<Map<String, Object>> l = makeTable("alpha", alphaList, betaList, null, null, null, null);
        assertEquals(2, l.size());
        assertEquals(l.get(0).keySet(), ImmutableSet.of("alpha", "list2"));
        assertResultsEqual(l.get(0).values(), "a0", "b0");
        assertResultsEqual(l.get(1).values(), "a1", "");
    }

    @Test
    public void testMakeTableWithNoDataForColumn() {
        List<String> alphaList = ImmutableList.of("a0", "a1");
        List<String> gammaList = ImmutableList.of("c0", "c1");
        List<Map<String, Object>> l = makeTable("alpha,beta,gamma", alphaList, ImmutableList.of(), gammaList, null, null, null);
        assertEquals(2, l.size());
        assertEquals(l.get(0).keySet(), ImmutableSet.of("alpha", "gamma"));
        assertResultsEqual(l.get(0).values(), "a0", "c0");
        assertResultsEqual(l.get(1).values(), "a1", "c1");
    }

    private List<Map<String, Object>> importSimpleCSV(String fileName) {
        return importCSV(fileName, "comma", "double", "period");
    }

}
