package nodebox.node;

import org.junit.Test;

import static junit.framework.TestCase.*;

public class PortTest {

    Port menuItemsPort = Port.stringPort("p", "keyA")
            .withMenuItemAdded("keyA", "labelA")
            .withMenuItemAdded("keyB", "labelB")
            .withMenuItemAdded("keyC", "labelC");

    @Test
    public void testParsedPort() {
        assertEquals(42, Port.parsedPort("myInt", "int", "42").intValue());
        assertEquals(33.3, Port.parsedPort("myInt", "float", "33.3").floatValue());
        assertEquals("hello", Port.parsedPort("myInt", "string", "hello").stringValue());
        assertEquals(true, Port.parsedPort("myBoolean", "boolean", "true").booleanValue());
    }

    @Test
    public void testParseBooleanPort() {
        assertEquals(true, Port.parsedPort("myBoolean", "boolean", "true").booleanValue());
        assertEquals(false, Port.parsedPort("myBoolean", "boolean", "false").booleanValue());
        assertEquals(false, Port.parsedPort("myBoolean", "boolean", "xxx").booleanValue());
    }

    /**
     * If the value is null, the default value is used.
     */
    @Test
    public void testParsedPortNullValue() {
        assertEquals(0, Port.parsedPort("myInt", "int", null).intValue());
        assertEquals(0.0, Port.parsedPort("myInt", "float", null).floatValue());
        assertEquals("", Port.parsedPort("myInt", "string", null).stringValue());
    }

    /**
     * Test if values are clamped when a bounding range is specified.
     */
    @Test
    public void testFloatBounding() {
        Port p1 = Port.floatPort("p", 5.0, 0.0, 10.0);
        Port p2 = p1.withValue(-10.0);
        assertEquals(0.0, p2.getValue());
    }

    @Test
    public void testFloatMin() {
        Port p1 = Port.floatPort("p", 5.0, 0.0, null);
        Port p2 = p1.withValue(9999.0);
        assertEquals(9999.0, p2.getValue());
        Port p3 = p1.withValue(-10.0);
        assertEquals(0.0, p3.getValue());
    }

    @Test
    public void testAddMenuItem() {
        Port p = menuItemsPort.withMenuItemAdded("keyD", "labelD");
        assertEquals(4, p.getMenuItems().size());
        assertEquals(new MenuItem("keyD", "labelD"), p.getMenuItems().get(3));
    }

    @Test
    public void testRemoveMenuItem() {
        Port p = menuItemsPort.withMenuItemRemoved(new MenuItem("keyC", "labelC"));
        assertEquals(2, p.getMenuItems().size());
    }

    @Test
    public void testChangeMenuItem() {
        Port p = menuItemsPort.withMenuItemChanged(0, "keyF", "labelF");
        assertEquals(new MenuItem("keyF", "labelF"), p.getMenuItems().get(0));
    }

    @Test
    public void testMoveMenuItem() {
        Port p = menuItemsPort.withMenuItemMovedUp(2);
        assertEquals(new MenuItem("keyC", "labelC"), p.getMenuItems().get(1));
        p = p.withMenuItemMovedDown(0);
        assertEquals(new MenuItem("keyA", "labelA"), p.getMenuItems().get(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMoveMenuItemMinIndex() {
        menuItemsPort.withMenuItemMovedUp(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMoveMenuItemMaxIndex() {
        menuItemsPort.withMenuItemMovedDown(menuItemsPort.getMenuItems().size() - 1);
    }

    /**
     * Test if values are clamped when a bounding range is specified.
     */
    @Test
    public void testIntBounding() {
        Port p1 = Port.intPort("p", 5, 0, 10);
        Port p2 = p1.withValue(-10);
        assertEquals(0L, p2.getValue());
    }

}
