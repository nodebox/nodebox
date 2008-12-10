package net.nodebox.node;

import junit.framework.TestCase;

public class DispatcherTest extends TestCase {

    private int accessCounter;

    public void setUp() {
        accessCounter = 0;
    }

    public void testNoObject() {
        Dispatcher.connect(this, "countAccess", "hello", null);
        assertEquals(0, accessCounter);
        Dispatcher.send("hello");
        assertEquals(1, accessCounter);
        // Send signal from some object, should be received.
        Dispatcher.send("hello", "someObject");
        assertEquals(2, accessCounter);
        // Disconnect the signal and see if this has effect.
        Dispatcher.disconnect(this, "countAccess", "hello", null);
        Dispatcher.send("hello");
        assertEquals(2, accessCounter);

    }

    public void testObject() {
        String someObject = "someObject";
        String otherObject = "otherObject";
        Dispatcher.connect(this, "countAccess", "hello", someObject);
        assertEquals(0, accessCounter);
        // Send from "everybody"; should not dispatch
        Dispatcher.send("hello", null);
        assertEquals(0, accessCounter);
        // Try to send from another object; should not dispatch countAccess.
        Dispatcher.send("hello", otherObject);
        assertEquals(0, accessCounter);
        // Send from the correct object.
        Dispatcher.send("hello", someObject);
        assertEquals(1, accessCounter);
    }

    public void testDisconnect() {

    }

    public void tearDown() {
        Dispatcher.removeAllReceivers();
    }

    public void countAccess() {
        accessCounter++;
    }
}
