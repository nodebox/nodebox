package net.nodebox.node;

import net.nodebox.graphics.Color;
import net.nodebox.graphics.Group;
import net.nodebox.graphics.Image;
import org.python.core.*;

public class PythonNodeType extends NodeType {

    private PyFunction function;
    String[] keywords;

    public PythonNodeType(NodeTypeLibrary library, String identifier, ParameterType.Type outputType, PyFunction function) {
        super(library, identifier, outputType);
        this.function = function;
        initKeywords();
    }

    private void initKeywords() {
        keywords = new String[getParameterTypeCount()];
        int i = 0;
        for (ParameterType type : getParameterTypes()) {
            keywords[i] = type.getName();
            i++;
        }
    }

    private PyObject toPyObject(Parameter parameter) {
        switch (parameter.getCoreType()) {
            case INT:
                return new PyInteger(parameter.asInt());
            case FLOAT:
                return new PyFloat(parameter.asFloat());
            case STRING:
                return new PyUnicode(parameter.asString());
            case COLOR:
                return new PyJavaInstance(parameter.asColor());
            case GROB_CANVAS:
            case GROB_SHAPE:
            case GROB_IMAGE:
                return new PyJavaInstance(parameter.asGrob());
            default:
                throw new AssertionError("Unknown core type " + parameter.getCoreType());
        }
    }

    public boolean process(Node node, ProcessingContext ctx) {
        PyObject[] values = new PyObject[getParameterTypeCount()];
        int i = 0;
        for (ParameterType type : getParameterTypes()) {
            values[i] = toPyObject(node.getParameter(type.getName()));
            i++;
        }
        PyObject returnValue = function.__call__(values, keywords);
        Object value = null;
        // Try to consolidate into core type.
        switch (getOutputParameterType().getCoreType()) {
            case INT:
                value = returnValue.asInt();
                break;
            case FLOAT:
                value = returnValue.asDouble();
                break;
            case STRING:
                value = returnValue.asString();
                break;
            case COLOR:
                value = returnValue.__tojava__(Color.class);
                break;
            case GROB_CANVAS:
                value = returnValue.__tojava__(Color.class);
                break;
            case GROB_SHAPE:
                value = returnValue.__tojava__(Group.class);
                break;
            case GROB_IMAGE:
                value = returnValue.__tojava__(Image.class);
                break;
        }
        node.setOutputValue(value);
        return true;
    }


}
