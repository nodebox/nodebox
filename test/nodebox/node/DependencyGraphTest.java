package nodebox.node;

import junit.framework.TestCase;

import java.util.List;
import java.util.Iterator;

public class DependencyGraphTest extends TestCase {

    public void testTopNodes() {
        DependencyGraph<Character, Object> dg;
        List<Character> topNodes;

        // Construct a simple graph: A <- B <- C
        dg = new DependencyGraph<Character, Object>();
        dg.addDependency('A', 'B');
        dg.addDependency('B', 'C');
        topNodes = dg.getTopNodes();
        assertEquals(1, topNodes.size());
        assertTrue(topNodes.contains('A'));

        // Construct a graph with no dependencies: A, B, C
        dg = new DependencyGraph<Character, Object>();
        dg.addNode('A');
        dg.addNode('B');
        dg.addNode('C');
        topNodes = dg.getTopNodes();
        assertEquals(3, topNodes.size());
        assertTrue(topNodes.contains('A'));
        assertTrue(topNodes.contains('B'));
        assertTrue(topNodes.contains('C'));

        // Construct a graph with more complex dependencies: A <- B, A <- C, B <- D
        dg = new DependencyGraph<Character, Object>();
        dg.addDependency('A', 'B');
        dg.addDependency('A', 'C');
        dg.addDependency('B', 'D');
        topNodes = dg.getTopNodes();
        assertEquals(1, topNodes.size());
        assertTrue(topNodes.contains('A'));
    }

    public void testCycles() {
        DependencyGraph<Character, Object> dg;

        // Direct cycle: A <- A
        dg = new DependencyGraph<Character, Object>();
        assertInvalidDependency(dg, 'A', 'A');

        // One degree of separation: A <- B <- A
        dg = new DependencyGraph<Character, Object>();
        dg.addDependency('A', 'B');
        assertInvalidDependency(dg, 'B', 'A');
        assertEquals(0, dg.getDependencies('A').size());
        assertEquals('A', (char)dg.getDependencies('B').iterator().next());

        // Two degrees of separation: A <- B <- C <- A
        dg = new DependencyGraph<Character, Object>();
        dg.addDependency('A', 'B');
        dg.addDependency('B', 'C');
        assertInvalidDependency(dg, 'C', 'A');

        // Diamond shape: A <- B <- D, A <- C <- D
        dg = new DependencyGraph<Character, Object>();
        dg.addDependency('A', 'B');
        dg.addDependency('A', 'C');
        dg.addDependency('B', 'D');
        dg.addDependency('C', 'D');
        assertInvalidDependency(dg, 'B', 'A');
        assertInvalidDependency(dg, 'C', 'A');
        assertInvalidDependency(dg, 'D', 'A');
    }

    public void testBreadthFirst() {
        // Because of the hashing function, we cannot predict the order of undependent nodes.
        // Therefore, we use our custom assertOneOf method to indicate "one of the following".
        DependencyGraph<Character, Object> dg;
        Iterator<Character> it;

        // Simple graph: A <- B
        dg = new DependencyGraph<Character, Object>();
        dg.addDependency('A', 'B');
        it = dg.getBreadthFirstIterator();
        assertEquals('A', (char)it.next());
        assertEquals('B', (char)it.next());
        assertFalse(it.hasNext());

        // No dependencies: A, B, C
        dg = new DependencyGraph<Character, Object>();
        dg.addNode('A');
        dg.addNode('B');
        dg.addNode('C');
        it = dg.getBreadthFirstIterator();
        assertOneOf("ABC", it.next());
        assertOneOf("ABC", it.next());
        assertOneOf("ABC", it.next());
        assertFalse(it.hasNext());

        // Complex: A <- B <- C, B <- D
        dg = new DependencyGraph<Character, Object>();
        dg.addDependency('A', 'B');
        dg.addDependency('B', 'C');
        dg.addDependency('B', 'D');
        it = dg.getBreadthFirstIterator();
        assertEquals('A', (char)it.next());
        assertEquals('B', (char)it.next());
        assertOneOf("CD", it.next());
        assertOneOf("CD", it.next());
        assertFalse(it.hasNext());
    }

    public void testRemoveDependencies() {
        DependencyGraph<Character, Object> dg;
        dg = new DependencyGraph<Character, Object>();
        // Z depends on A and B.
        dg.addDependency('A', 'Z');
        dg.addDependency('B', 'Z');
        // X and Y depend on Z
        dg.addDependency('Z', 'X');
        dg.addDependency('Z', 'Y');
        assertTrue(dg.hasDependency('A', 'Z'));
        assertTrue(dg.hasDependency('B', 'Z'));
        assertFalse(dg.hasDependency('Z', 'A'));
        assertTrue(dg.hasDependency('Z', 'X'));
        assertTrue(dg.hasDependency('Z', 'Y'));
        // Remove all dependencies for Z.
        dg.removeDependencies('Z');
        assertFalse(dg.hasDependency('A', 'Z'));
        assertFalse(dg.hasDependency('B', 'Z'));
        assertFalse(dg.hasDependency('Z', 'A'));
        assertTrue(dg.hasDependency('Z', 'X'));
        assertTrue(dg.hasDependency('Z', 'Y'));
    }

    public void testRemoveDependents() {
        DependencyGraph<Character, Object> dg;
        dg = new DependencyGraph<Character, Object>();
        // Z depends on A and B.
        dg.addDependency('A', 'Z');
        dg.addDependency('B', 'Z');
        // X and Y depend on Z
        dg.addDependency('Z', 'X');
        dg.addDependency('Z', 'Y');
        // We already checked if the dependencies are correct in testRemoveDependencies().
        // Remove all dependents for Z. This happens when the Z parameter is about to be removed.
        dg.removeDependents('Z');
        assertTrue(dg.hasDependency('A', 'Z'));
        assertTrue(dg.hasDependency('B', 'Z'));
        assertFalse(dg.hasDependency('Z', 'X'));
        assertFalse(dg.hasDependency('Z', 'Y'));
    }

    public void assertInvalidDependency(DependencyGraph<Character, Object> dg, Character dependency, Character dependent) {
        try {
            dg.addDependency(dependency, dependent);
            fail("Should have thrown an IllegalArgumentException: " + dependency + " <- " + dependent);
        } catch (IllegalArgumentException ignored) {
        }
    }

    /**
     * Possibles contains all letters that are possible. This string is split up.
     * @param expected a list of possible letters, e.g. "ABC"
     * @param actual the actual value.
     */
    public void assertOneOf(String expected, char actual) {
        for (char c:expected.toCharArray()) {
            if (expected.indexOf(c) >= 0)
                return;
        }
        fail("The character '" + actual + "' is not one of the expected \"" + expected + "\".");
    }
}
