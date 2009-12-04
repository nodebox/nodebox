package nodebox.node;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

public class JavaMethodWrapper implements NodeCode {

    public static final String TYPE_JAVA = "java".intern();

    private Class methodClass;
    private String methodName;
    private Method method;

    public JavaMethodWrapper(Class methodClass, String methodName) {
        this.methodClass = methodClass;
        this.methodName = methodName;
        try {
            this.method = methodClass.getMethod(methodName, Node.class, ProcessingContext.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("The given method does not exist.");
        }
        if (!Modifier.isStatic(this.method.getModifiers())) {
            throw new RuntimeException("The given method is not static.");
        }
    }

    public Class getMethodClass() {
        return methodClass;
    }

    public String getMethodName() {
        return methodName;
    }

    public Method getMethod() {
        return method;
    }

    public Object cook(Node node, ProcessingContext context) {
        try {
            return method.invoke(null, node, context);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Illegal access exception", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Invocation target exception", e);
        }
    }

    public String getSource() {
        return "// Source not available";
    }

    public String getType() {
        return TYPE_JAVA;
    }
}
