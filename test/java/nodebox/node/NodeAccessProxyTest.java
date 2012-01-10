package nodebox.node;

public class NodeAccessProxyTest extends NodeTestCase {

    public void testGet() {
        Node root = testLibrary.getRootNode();
        Node trunk = Node.ROOT_NODE.newInstance(testLibrary, "trunk");
        Node branch = Node.ROOT_NODE.newInstance(testLibrary, "branch");
        Node leaf1 = Node.ROOT_NODE.newInstance(testLibrary, "leaf1");
        Node leaf2 = Node.ROOT_NODE.newInstance(testLibrary, "leaf2");
        leaf1.setParent(branch);
        leaf2.setParent(branch);
        branch.setParent(trunk);
        trunk.addParameter("age", Parameter.Type.INT, 42);
        branch.addParameter("length", Parameter.Type.INT, 33);
        leaf1.addParameter("width", Parameter.Type.INT, 5);
        leaf2.addParameter("width", Parameter.Type.INT, 7);

        // Check reserved keywords.
        assertProxyEquals(root, trunk, "root");
        assertProxyEquals(root, trunk, "parent");
        assertProxyNull(root, "parent");

        // Check children
        assertProxyEquals(branch, trunk, "branch");
        assertProxyEquals(leaf1, branch, "leaf1");
        assertProxyEquals(leaf2, branch, "leaf2");
        assertProxyNull(branch, "leaf3");
    }

    /**
     * Assert the proxy equals expectedNode for the given key.
     *
     * @param expectedNode the node to expect
     * @param proxyNode    the node to wrap in a proxy
     * @param key          the key from that proxy
     */
    public void assertProxyEquals(Node expectedNode, Node proxyNode, String key) {
        NodeAccessProxy proxy = new NodeAccessProxy(proxyNode);
        Object obj = proxy.get(key);
        assertNotNull(obj);
        assertEquals(NodeAccessProxy.class, obj.getClass());
        assertEquals(expectedNode, ((NodeAccessProxy) obj).getNode());
    }

    /**
     * Assert the proxy returns null for the given key
     *
     * @param proxyNode the node to wrap in a proxy
     * @param key       the key from that proxy
     */
    private void assertProxyNull(Node proxyNode, String key) {
        NodeAccessProxy proxy = new NodeAccessProxy(proxyNode);
        Object obj = proxy.get(key);
        assertNull(obj);
    }


}
