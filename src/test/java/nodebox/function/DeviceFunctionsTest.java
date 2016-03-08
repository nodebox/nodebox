package nodebox.function;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import nodebox.node.Node;
import nodebox.node.NodeContext;
import nodebox.node.NodeLibrary;
import nodebox.node.Port;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;


public class DeviceFunctionsTest {

    private final FunctionLibrary listLibrary = ListFunctions.LIBRARY;
    private final FunctionLibrary deviceLibrary = DeviceFunctions.LIBRARY;
    private final FunctionRepository functions = FunctionRepository.of(deviceLibrary, listLibrary);
    private final NodeLibrary testLibrary = NodeLibrary.create("test", Node.ROOT, functions);

    private final Node oscReceiveNode = Node.ROOT
            .withOutputRange(Port.Range.LIST)
            .withFunction("device/receiveOSC")
            .withInputAdded(Port.stringPort("device_name", "osc"))
            .withInputAdded(Port.stringPort("prefix", "/"))
            .withInputAdded(Port.stringPort("args", ""))
            .withInputAdded(Port.customPort("context", "context"));

    private final Map<String, List<Object>> oscMessages = ImmutableMap.of(
                "/2/multifader/11", (List<Object>) ImmutableList.<Object>of(0.5),
                "/2/multifader/8", (List<Object>) ImmutableList.<Object>of(0.9),
                "/2/multitoggle/2/15", (List<Object>) ImmutableList.<Object>of(0.1, 0.2),
                "/2/multitoggle/4/10", (List<Object>) ImmutableList.<Object>of(0.3, 0.6),
                "/r/g/b/20", (List<Object>) ImmutableList.<Object>of(0.15, 0.35, 0.77));

    private List<?> renderNode(Node node) {
        Map<String, Object> data = ImmutableMap.<String, Object>of("osc.messages", oscMessages);
        return new NodeContext(testLibrary.withRoot(node), null, data).renderNode("/");
    }

    @Test
    public void testCallReceiveAll() {
        assertEquals(ImmutableList.of(
                ImmutableMap.of("address", "/2/multifader/11", "Column1", 0.5, "Column2", 0, "Column3", 0),
                ImmutableMap.of("address", "/2/multifader/8", "Column1", 0.9, "Column2", 0, "Column3", 0),
                ImmutableMap.of("address", "/2/multitoggle/2/15", "Column1", 0.1, "Column2", 0.2, "Column3", 0),
                ImmutableMap.of("address", "/2/multitoggle/4/10", "Column1", 0.3, "Column2", 0.6, "Column3", 0),
                ImmutableMap.of("address", "/r/g/b/20", "Column1", 0.15, "Column2", 0.35, "Column3", 0.77))
                , renderNode(oscReceiveNode));
    }

    @Test
    public void testCallReceiveSpecific() {
        Node oscReceiveNode1 = oscReceiveNode.withInputValue("prefix", "/2/multifader/8");
        List<Map<String, Object>> expectedResult = ImmutableList.<Map<String, Object>>of(
                ImmutableMap.<String, Object>of("address", "/2/multifader/8", "Column", 0.9));
        assertEquals(expectedResult, renderNode(oscReceiveNode1));
        Node oscReceiveNode2 = oscReceiveNode.withInputValue("prefix", "/2/multifader/8*");
        assertEquals(expectedResult, renderNode(oscReceiveNode2));
    }

    @Test
    public void testCallReceivePrefix() {
        Node oscReceiveNode1 = oscReceiveNode.withInputValue("prefix", "/2/multifader");
        List<Map<String, Object>> expectedResult = ImmutableList.<Map<String, Object>>of(
                ImmutableMap.<String, Object>of("address", "/2/multifader/11", "Column", 0.5),
                ImmutableMap.<String, Object>of("address", "/2/multifader/8", "Column", 0.9));
        assertEquals(expectedResult, renderNode(oscReceiveNode1));
        Node oscReceiveNode2 = oscReceiveNode.withInputValue("prefix", "/2/multifader*");
        assertEquals(expectedResult, renderNode(oscReceiveNode2));
    }

    @Test
    public void testCallReceiveWildcard() {
        Node oscReceiveNode1 = oscReceiveNode.withInputValue("prefix", "/2/multi*");
        List<Map<String, Object>> expectedResult = ImmutableList.<Map<String, Object>>of(
                ImmutableMap.<String, Object>of("address", "/2/multifader/11", "Column1", 0.5, "Column2", 0),
                ImmutableMap.<String, Object>of("address", "/2/multifader/8", "Column1", 0.9, "Column2", 0),
                ImmutableMap.<String, Object>of("address", "/2/multitoggle/2/15", "Column1", 0.1, "Column2", 0.2),
                ImmutableMap.<String, Object>of("address", "/2/multitoggle/4/10", "Column1", 0.3, "Column2", 0.6));
        assertEquals(expectedResult, renderNode(oscReceiveNode1));
        Node oscReceiveNode2 = oscReceiveNode.withInputValue("prefix", "/2/*lti*");
        assertEquals(expectedResult, renderNode(oscReceiveNode2));
        Node oscReceiveNode3 = oscReceiveNode.withInputValue("prefix", "*multi");
        assertEquals(expectedResult, renderNode(oscReceiveNode3));
        Node oscReceiveNode4 = oscReceiveNode.withInputValue("prefix", "multi*");
        assertEquals(ImmutableList.of(), renderNode(oscReceiveNode4));
    }

    @Test
    public void testCallReceiveMessagePattern() {
        Node oscReceiveNode1 = oscReceiveNode.withInputValue("prefix", "/2/multitoggle/<x>/<y>");
        List<Map<String, Object>> expectedResult = ImmutableList.<Map<String, Object>>of(
                ImmutableMap.<String, Object>of("address", "/2/multitoggle/2/15", "x", "2", "y", "15", "Column1", 0.1, "Column2", 0.2),
                ImmutableMap.<String, Object>of("address", "/2/multitoggle/4/10", "x", "4", "y", "10", "Column1", 0.3, "Column2", 0.6));
        assertEquals(expectedResult, renderNode(oscReceiveNode1));
    }

    @Test
    public void testCallReceiveMessageTypedPattern() {
        Node oscReceiveNode1 = oscReceiveNode.withInputValue("prefix", "/<pageid>/multifader/<faderid>");
        List<Map<String, Object>> expectedResult1 = ImmutableList.<Map<String, Object>>of(
                ImmutableMap.<String, Object>of("address", "/2/multifader/11", "pageid", "2", "faderid", "11", "Column", 0.5),
                ImmutableMap.<String, Object>of("address", "/2/multifader/8", "pageid", "2", "faderid", "8", "Column", 0.9));
        assertEquals(expectedResult1, renderNode(oscReceiveNode1));
        Node oscReceiveNode2 = oscReceiveNode.withInputValue("prefix", "/<pageid:string>/multifader/<faderid:s>");
        assertEquals(expectedResult1, renderNode(oscReceiveNode2));
        Node oscReceiveNode3 = oscReceiveNode.withInputValue("prefix", "/<pageid:int>/multifader/<faderid:i>");
        List<Map<String, Object>> expectedResult3 = ImmutableList.<Map<String, Object>>of(
                ImmutableMap.<String, Object>of("address", "/2/multifader/11", "pageid", 2, "faderid", 11, "Column", 0.5),
                ImmutableMap.<String, Object>of("address", "/2/multifader/8", "pageid", 2, "faderid", 8, "Column", 0.9));
        assertEquals(expectedResult3, renderNode(oscReceiveNode3));
        Node oscReceiveNode4 = oscReceiveNode.withInputValue("prefix", "/<pageid:f>/multifader/<faderid:float>");
        List<Map<String, Object>> expectedResult4 = ImmutableList.<Map<String, Object>>of(
                ImmutableMap.<String, Object>of("address", "/2/multifader/11", "pageid", 2.0, "faderid", 11.0, "Column", 0.5),
                ImmutableMap.<String, Object>of("address", "/2/multifader/8", "pageid", 2.0, "faderid", 8.0, "Column", 0.9));
        assertEquals(expectedResult4, renderNode(oscReceiveNode4));
        Node oscReceiveNode5 = oscReceiveNode.withInputValue("prefix", "/<pageid:l>/multifader/<faderid>");
        assertEquals(ImmutableList.of(), renderNode(oscReceiveNode5));
    }

    @Test
    public void testCallReceiveAndSortData() {
        Node oscReceiveNode1 = oscReceiveNode
                .withName("osc_receive1")
                .withInputValue("prefix", "/*/*/<itemid>");
        Node sortNode = Node.ROOT
                .withOutputRange(Port.Range.LIST)
                .withFunction("list/sort")
                .withInputAdded(Port.customPort("list", "list").withRange(Port.Range.LIST))
                .withInputAdded(Port.stringPort("key", ""));
        Node sortNode1 = sortNode
                .withName("sort1")
                .withInputValue("key", "itemid");
        Node net1 = Node.NETWORK
                .withChildAdded(oscReceiveNode1)
                .withChildAdded(sortNode1)
                .withRenderedChildName("sort1")
                .connect("osc_receive1", "sort1", "list");
        List<Map<String, Object>> expectedResult1 = ImmutableList.<Map<String, Object>>of(
                ImmutableMap.<String, Object>of("address", "/2/multifader/11", "itemid", "11", "Column1", 0.5, "Column2", 0, "Column3", 0),
                ImmutableMap.<String, Object>of("address", "/2/multitoggle/2/15", "itemid", "2", "Column1", 0.1, "Column2", 0.2, "Column3", 0),
                ImmutableMap.<String, Object>of("address", "/2/multitoggle/4/10", "itemid", "4", "Column1", 0.3, "Column2", 0.6, "Column3", 0),
                ImmutableMap.<String, Object>of("address", "/2/multifader/8", "itemid", "8", "Column1", 0.9, "Column2", 0, "Column3", 0),
                ImmutableMap.<String, Object>of("address", "/r/g/b/20", "itemid", "b", "Column1", 0.15, "Column2", 0.35, "Column3", 0.77));
        assertEquals(expectedResult1, renderNode(net1));
        Node oscReceiveNode2 = oscReceiveNode
                .withName("osc_receive2")
                .withInputValue("prefix", "/*/*/<itemid:i>");
        Node net2 = Node.NETWORK
                .withChildAdded(oscReceiveNode2)
                .withChildAdded(sortNode1)
                .withRenderedChildName("sort1")
                .connect("osc_receive2", "sort1", "list");
        List<Map<String, Object>> expectedResult2 = ImmutableList.<Map<String, Object>>of(
                ImmutableMap.<String, Object>of("address", "/r/g/b/20", "itemid", 0, "Column1", 0.15, "Column2", 0.35, "Column3", 0.77),
                ImmutableMap.<String, Object>of("address", "/2/multitoggle/2/15", "itemid", 2, "Column1", 0.1, "Column2", 0.2, "Column3", 0),
                ImmutableMap.<String, Object>of("address", "/2/multitoggle/4/10", "itemid", 4, "Column1", 0.3, "Column2", 0.6, "Column3", 0),
                ImmutableMap.<String, Object>of("address", "/2/multifader/8", "itemid", 8, "Column1", 0.9, "Column2", 0, "Column3", 0),
                ImmutableMap.<String, Object>of("address", "/2/multifader/11", "itemid", 11, "Column1", 0.5, "Column2", 0, "Column3", 0));
        assertEquals(expectedResult2, renderNode(net2));
    }

    @Test
    public void testCallReceiveArguments() {
        Node oscReceiveNode1 = oscReceiveNode
                .withInputValue("prefix", "/2/multifader*")
                .withInputValue("args", "v");
        List<Map<String, Object>> expectedResult1 = ImmutableList.<Map<String, Object>>of(
                ImmutableMap.<String, Object>of("address", "/2/multifader/11", "v", 0.5),
                ImmutableMap.<String, Object>of("address", "/2/multifader/8", "v", 0.9));
        assertEquals(expectedResult1, renderNode(oscReceiveNode1));
        Node oscReceiveNode2 = oscReceiveNode
                .withInputValue("prefix", "/2/multitoggle*")
                .withInputValue("args", "x,y");
        List<Map<String, Object>> expectedResult2 = ImmutableList.<Map<String, Object>>of(
                ImmutableMap.<String, Object>of("address", "/2/multitoggle/2/15", "x", 0.1, "y", 0.2),
                ImmutableMap.<String, Object>of("address", "/2/multitoggle/4/10", "x", 0.3, "y", 0.6));
        assertEquals(expectedResult2, renderNode(oscReceiveNode2));
        assertEquals(expectedResult2, renderNode(oscReceiveNode2.withInputValue("args", "x ,y")));
        assertEquals(expectedResult2, renderNode(oscReceiveNode2.withInputValue("args", "x,  y")));
    }
}
