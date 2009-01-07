package net.nodebox.node;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class JavaNodeType extends NodeType {

    private Method method;

    public JavaNodeType(NodeTypeLibrary library, String identifier, ParameterType.Type outputType) {
        super(library, identifier, outputType);
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Class clazz, String methodName) throws NoSuchMethodException {
        Class[] parameterClasses = new Class[getParameterTypeCount()];
        for (int i = 0; i < getParameterTypeCount(); i++) {
            parameterClasses[i] = getParameterTypes().get(i).getTypeClass();
        }
        method = clazz.getMethod(methodName, parameterClasses);
        if (!Modifier.isStatic(method.getModifiers())) {
            throw new NoSuchMethodException("Method " + methodName + " on class " + clazz.getName() + " is not static.");
        }
    }

    public void setMethod(Method m) throws NoSuchMethodException {
        setMethod(m.getDeclaringClass(), m.getName());
    }

    public boolean process(Node node, ProcessingContext ctx) {
        if (method == null)
            throw new RuntimeException("Java node type " + getName() + "does not have method set.");
        Object[] parameterValues = new Object[getParameterTypeCount()];
        for (int i = 0; i < getParameterTypeCount(); i++) {
            ParameterType pt = getParameterTypes().get(i);
            parameterValues[i] = node.getValue(pt.getName());
        }
        try {
            method.invoke(null, parameterValues);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Error while invoking method " + getMethod(), e);
        }
    }

}
