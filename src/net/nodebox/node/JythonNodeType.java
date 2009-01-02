package net.nodebox.node;

import org.python.core.PyMethod;

public class JythonNodeType extends NodeType {

    private PyMethod method;

    public JythonNodeType(NodeManager manager, String identifier, ParameterType.Type outputType, PyMethod method) {
        super(manager, identifier, outputType);
        this.method = method;
    }

    public boolean process(Node node, ProcessingContext ctx) {
        method.__call__();
        return true;
    }


}
