package nodebox.node;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PublishedPortTest {

    public static final Node number42Node = Node.ROOT
            .withName("number42")
            .withFunction("math/number")
            .withInputAdded(Port.floatPort("number", 42));

    public static final Node net = Node.ROOT
            .withChildAdded(number42Node)
            .withRenderedChildName("number42");

    @Test
    public void testPublishInput() {
        Node n = net;
        assertEquals(0, n.getPublishedInputs().size());
        n = n.publish("number42", "number", "pNumber");
        assertEquals(1, n.getPublishedInputs().size());
        assertTrue(n.isPublished("number42", "number"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPublishFromUnexistingChild() {
        net.publish("number3", "number", "pNumber");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPublishFromUnexistingChildInput() {
        net.publish("number42", "myNumber", "pNumber");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPublishSameInputTwice() {
        Node n = net;
        n = n.publish("number42", "number", "pNumber");
        n.publish("number42", "number", "pNumber");
    }

    @Test
    public void testUnpublishInput() {
        Node n = net;
        n = n.publish("number42", "number", "pNumber");
        assertEquals(1, n.getPublishedInputs().size());
        n = n.unpublish("number42", "number");
        assertEquals(0, n.getPublishedInputs().size());
        assertFalse(n.isPublished("number42", "number"));
    }
}
