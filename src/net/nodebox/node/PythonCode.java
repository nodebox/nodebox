package net.nodebox.node;

import org.python.core.Py;
import org.python.core.PyDictionary;
import org.python.core.PyFunction;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

/**
 * Python source code is in this form:
 * <pre><code>
 * def cook(self):
 *     return Polygon.rect(self.x, self.y, self.width, self.height)
 * </code></pre>
 * <p/>
 * Self is a reference that points to a node access proxy that allows direct access to the node's parameter values.
 * You can access the node object using "self.node".
 * <p/>
 * When creating a PythonCode object, it is executed immediately to extract the "cook" function. Source code
 * that has no or invalid "cook" function will throw an IllegalArgumentException.
 * <p/>
 * The cook method on this class executes the Python "cook" function with the self reference. It also sets a number
 * of global parameters based on the ProcessingContext.
 */
public class PythonCode implements NodeCode {

    public static final String TYPE = "python";
    private String source;
    private PyDictionary namespace;
    private PyFunction cookFunction;

    public PythonCode(String source) {
        this.source = source;
    }

    private void preCook() {
        // The namespace will remain bound to the interpreter.
        // Changes to this dictionary will affect the namespace of the interpreter.
        namespace = new PyDictionary();
        PythonInterpreter interpreter = new PythonInterpreter(namespace);
        // Immediately run the code to extract the cook(self) method.
        interpreter.exec(source);
        try {
            cookFunction = (PyFunction) interpreter.get("cook");
            if (cookFunction == null)
                throw new RuntimeException("Source code does not contain a function \"cook(self)\".");
        } catch (ClassCastException e) {
            throw new RuntimeException("Attribute \"cook\" in source code is not a function.");
        }
        // We cannot check if the function takes only one (required) argument.
        // If the function has more arguments, this will throw an error when cooking.
    }

    public Object cook(Node node, ProcessingContext context) throws RuntimeException {
        if (cookFunction == null) preCook();
        // Add globals into the function namespace.
        namespace.put("FRAME", context.getFrame());
        PyObject self;
        if (node == null) {
            self = Py.None;
        } else {
            self = new SelfWrapper(node);
        }
        PyObject pyResult = cookFunction.__call__(self);
        // Unwrap the result.
        Object result = pyResult.__tojava__(Object.class);
        if (result == Py.NoConversion) {
            throw new RuntimeException("Cannot convert Python object " + pyResult + " to java.");
        }
        return result;
    }

    public String getSource() {
        return source;
    }

    public String getType() {
        return TYPE;
    }

    /**
     * The self wrapper allows easy access to parameter values from the node.
     * Instead of doing node.asString("someparameter"), you can use self.someparameter.
     * You can also access the node itself by querying self.node.
     * <p/>
     * The code here looks similar to the NodeAccessProxy, but it is not the same. Specifically, we don't give users
     * easy access to parameters/ports on other nodes. In principle, all the behaviour of the
     * node should be self-contained, which means that everything the node needs to operate is set in its parameters
     * and ports. Therefore it should not have access to other nodes. For nodes that implement special functionality
     * (such as the copy node), they can still access everything through the node reference.
     */
    public class SelfWrapper extends PyObject {

        private Node node;

        public SelfWrapper(Node node) {
            this.node = node;
        }

        @Override
        public PyObject __findattr_ex__(String name) {
            if ("node".equals(name)) return Py.java2py(node);
            Parameter p = node.getParameter(name);
            if (p == null) {
                Port port = node.getPort(name);
                if (port == null) {
                    // This will throw an error that we explicitly do not catch.
                    noParameterOrPortError(name);
                    throw new AssertionError("noParameterOrPortError method should have thrown an error.");
                } else {
                    if (port.getCardinality() == Port.Cardinality.SINGLE) {
                        return Py.java2py(port.getValue());
                    } else {
                        return Py.java2py(port.getValues());
                    }
                }
            } else {
                return Py.java2py(p.getValue());
            }
        }

        /**
         * This method throws an error that will be shown to users referring to non-existant parameters.
         * (e.g. self.inventedParameter)
         * <p/>
         * We could use the original noAttributeError, but that results in an ugly object name (a reference
         * to this proxy class). We'd much rather refer to the node identifier and parameter/port.
         *
         * @param name the name of the parameter
         */
        public void noParameterOrPortError(String name) {
            throw Py.AttributeError(String.format("Node '%.50s' has no parameter or port '%.400s'",
                    node.getIdentifier(), name));
        }

    }

}
