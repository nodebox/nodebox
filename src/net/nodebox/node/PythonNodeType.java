package net.nodebox.node;

import net.nodebox.graphics.*;
import org.python.core.*;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class PythonNodeType extends NodeType {

    private PyFunction function;
    String[] keywords;
    List<WeakReference<Node>> instanceRefs = new ArrayList<WeakReference<Node>>();

    public PythonNodeType(NodeTypeLibrary library, String identifier, ParameterType.Type outputType, PyFunction function) {
        super(library, identifier, outputType);
        this.function = function;
        initKeywords();
    }

    @Override
    public Node createNode() {
        Node n = super.createNode();
        instanceRefs.add(new WeakReference<Node>(n));
        return n;
    }

    @Override
    public boolean reload() {
        return getLibrary().reload();
    }

    public void reloadPython() {
        // Retrieve the method from the new python module.
        PyObject module = ((PythonNodeTypeLibrary) getLibrary()).getPythonModule();
        String functionName = function.__name__.intern();
        PyObject functionObject;
        try {
            functionObject = module.__getattr__(functionName);
        } catch (Exception e) {
            throw new RuntimeException("the method '" + functionName + "' does not exist in the module " + module, e);
        }
        try {
            this.function = (PyFunction) functionObject;
        } catch (ClassCastException e) {
            throw new RuntimeException("the module attribute '" + functionName + "' is not a Python function.", e);
        }
        // Mark all instances as dirty.
        for (WeakReference<Node> ref : instanceRefs) {
            Node n = ref.get();
            if (n != null)
                n.markDirty();
        }

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
            case GROB_PATH:
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
            case GROB:
                value = returnValue.__tojava__(Grob.class);
                break;
            case GROB_PATH:
                value = returnValue.__tojava__(BezierPath.class);
                break;
            case GROB_CANVAS:
                value = returnValue.__tojava__(Canvas.class);
                break;
            case GROB_GROUP:
                value = returnValue.__tojava__(Group.class);
                break;
            case GROB_IMAGE:
                value = returnValue.__tojava__(Image.class);
                break;
            case GROB_TEXT:
                value = returnValue.__tojava__(Text.class);
                break;
        }
        node.setOutputValue(value);
        return true;
    }

}
