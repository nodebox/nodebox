package net.nodebox.node;

public class NodeTypeTest extends NodeTestCase {


    /**
     * Type with no input parameters.
     */
    private class AlphaType extends NodeType {

        private AlphaType() {
            super(testLibrary, "alpha", ParameterType.Type.INT);
        }

        public boolean process(Node node, ProcessingContext ctx) {
            node.setOutputValue(12);
            return true;
        }
    }

    /**
     * Type with one input parameter, called value.
     */
    private class BetaType extends NodeType {
        private BetaType() {
            super(testLibrary, "beta", ParameterType.Type.INT);
            ParameterType ptValue = addParameterType("value", ParameterType.Type.INT);
            ptValue.setDefaultValue(13);
        }

        public boolean process(Node node, ProcessingContext ctx) {
            node.setOutputValue(-node.asInt("value"));
            return true;
        }
    }


    /**
     * Type with the same input parameter as beta, but with a different default value.
     */
    private class GammaType extends NodeType {
        private GammaType() {
            super(testLibrary, "gamma", ParameterType.Type.INT);
            ParameterType ptValue = addParameterType("value", ParameterType.Type.INT);
            ptValue.setDefaultValue(99);
        }

        public boolean process(Node node, ProcessingContext ctx) {
            node.setOutputValue(node.asInt("value") + 10);
            return true;
        }
    }


    /**
     * Type with the same input parameter as gamma, but with hard bounding.
     */
    private class DeltaType extends NodeType {
        private DeltaType() {
            super(testLibrary, "delta", ParameterType.Type.INT);
            ParameterType ptValue = addParameterType("value", ParameterType.Type.INT);
            ptValue.setBoundingMethod(ParameterType.BoundingMethod.HARD);
            ptValue.setMinimumValue(3.0);
        }

        public boolean process(Node node, ProcessingContext ctx) {
            node.setOutputValue(node.asInt("value") * 2);
            return true;
        }
    }

    /**
     * Type with the same input parameter as delta, but with float type instead of int. Also, the output is float.
     */
    private class EpsilonType extends NodeType {
        private EpsilonType() {
            super(testLibrary, "epsilon", ParameterType.Type.FLOAT);
            ParameterType ptValue = addParameterType("value", ParameterType.Type.FLOAT);
        }

        public boolean process(Node node, ProcessingContext ctx) {
            node.setOutputValue(node.asFloat("value") / 2.0);
            return true;
        }
    }

    /**
     * Type with the same input parameter as delta, but with string type instead of float. Also, the output is string.
     * Converts the input to uppercase.
     */
    private class ZetaType extends NodeType {
        private ZetaType() {
            super(testLibrary, "zeta", ParameterType.Type.STRING);
            ParameterType ptValue = addParameterType("value", ParameterType.Type.STRING);
        }

        public boolean process(Node node, ProcessingContext ctx) {
            node.setOutputValue(node.asString("value").toUpperCase());
            return true;
        }
    }

    /**
     * Type with two int parameters that are added together.
     */
    private class EtaType extends NodeType {
        private EtaType() {
            super(testLibrary, "eta", ParameterType.Type.INT);
            addParameterType("value", ParameterType.Type.INT);
            addParameterType("value2", ParameterType.Type.INT);
        }

        public boolean process(Node node, ProcessingContext ctx) {
            node.setOutputValue(node.asInt("value") + node.asInt("value2"));
            return true;
        }
    }

    public void testMigrateAddParameter() {
        NodeType alphaType = new AlphaType();
        NodeType betaType = new BetaType();
        // Create an alpha type node. This type has no parameters.
        Node n = alphaType.createNode();
        n.update();
        assertEquals(12, n.getOutputValue());
        // Change the node type to beta. This type has one parameter; the node should get the default parameter value.
        n.setNodeType(betaType);
        // Check if the parameter is there and has the correct value.
        assertEquals(ParameterType.Type.INT, n.getParameter("value").getType());
        assertEquals(13, n.asInt("value"));
        n.update();
        assertEquals(-13, n.getOutputValue());
    }

    public void testMigrateDifferentDefaultValue() {
        NodeType betaType = new BetaType();
        NodeType gammaType = new GammaType();
        // Create a beta type node. This type has one parameter, value.
        Node n = betaType.createNode();
        n.update();
        assertEquals(-13, n.getOutputValue());
        // Change the node type to gamma. This type has the same parameter but with a different default value.
        // Changing the type should not change the value.
        n.setNodeType(gammaType);
        // The default value refers to the new parameter type
        assertEquals(99, n.getParameter("value").getParameterType().getDefaultValue());
        // The value of the parameter is unchanged.
        assertEquals(13, n.asInt("value"));
        n.update();
        assertEquals(23, n.getOutputValue());
    }

    public void testMigrateBounding() {
        NodeType betaType = new BetaType();
        NodeType deltaType = new DeltaType();
        // Create a beta type node. This type has one parameter, value.
        Node n = betaType.createNode();
        // Set value to negative in preparation of bounding check.
        n.set("value", -100);
        n.update();
        assertEquals(100, n.getOutputValue());
        // Change the node type to delta, which has hard bounding set.
        n.setNodeType(deltaType);
        // Check that the value is clamped to bounds.
        assertEquals(3, n.asInt("value"));
        n.update();
        assertEquals(6, n.getOutputValue());
    }

    public void testMigrateCompatibleType() {
        NodeType betaType = new BetaType();
        NodeType epsilonType = new EpsilonType();
        // Create a beta type node. This type has one parameter, value.
        Node n = betaType.createNode();
        // Change the node type to epsilon, which has the same parameters of a different type.
        n.setNodeType(epsilonType);
        assertEquals(ParameterType.Type.FLOAT, n.getOutputParameter().getType());
        // Since the output type has changed and the node has not yet updated, the output gets the default value.
        assertEquals(0.0, n.getOutputValue());
        assertEquals(ParameterType.Type.FLOAT, n.getParameter("value").getType());
        assertEquals(13.0, n.asFloat("value"));
        n.update();
        assertEquals(6.5, n.getOutputValue());
    }

    public void testMigrateIncompatibleType() {
        NodeType betaType = new BetaType();
        NodeType zetaType = new ZetaType();
        // Create a beta type node. This type has one parameter, value.
        Node n = betaType.createNode();
        n.set("value", 101);
        n.update();
        assertEquals(-101, n.getOutputValue());
        // Change the node type to zeta, where the value parameter is of type string.
        n.setNodeType(zetaType);
        // Again, the output is set to default value of the string type.
        assertEquals("", n.getOutputValue());
        assertEquals("101", n.asString("value"));
        n.set("value", "no shouting");
        n.update();
        assertEquals("NO SHOUTING", n.getOutputValue());
        // Let's try to convert back to epsilon, which has an integer value. Of course, the value "no shouting" does
        // not convert to an integer value. The value will be reset to its default.
        n.setNodeType(betaType);
        assertEquals(13, n.asInt("value"));

    }

    /**
     * Migrate to a type that has less parameters than the original type, and check if the extraneous parameters
     * are removed.
     */
    public void testMigrateRemoveParameters() {
        // Type with two int parameters, value and value2
        NodeType etaType = new EtaType();
        // Type with one int parameter, value
        NodeType betaType = new BetaType();
        Node n = etaType.createNode();
        assertTrue(n.hasParameter("value"));
        assertTrue(n.hasParameter("value2"));
        n.setNodeType(betaType);
        assertTrue(n.hasParameter("value"));
        assertFalse(n.hasParameter("value2"));
    }

}
