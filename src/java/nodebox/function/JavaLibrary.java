package nodebox.function;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import nodebox.util.LoadException;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import static com.google.common.base.Preconditions.checkArgument;

public final class JavaLibrary extends FunctionLibrary {

    public static JavaLibrary loadStaticClass(String identifier) {
        try {
            Class c = Class.forName(identifier);
            Field instanceField = c.getDeclaredField("LIBRARY");
            return (JavaLibrary) instanceField.get(null);
        } catch (ClassNotFoundException e) {
            throw new LoadException(null, e);
        } catch (NoSuchFieldException e) {
            throw new LoadException(null, e);
        } catch (IllegalAccessException e) {
            throw new LoadException(null, e);
        } catch (ClassCastException e) {
            throw new LoadException(null, e);
        } catch (ExceptionInInitializerError e) {
            throw new LoadException(null, e);
        }
    }

    public static JavaLibrary ofClass(String namespace, Class c, String... methodNames) {
        ArrayList<Function> functions = new ArrayList<Function>();
        for (String methodName : methodNames) {
            Function function = StaticMethodFunction.find(c, methodName);
            functions.add(function);
        }
        return new JavaLibrary(namespace, c, functions);
    }

    private final String namespace;
    private final Class clazz;
    private final ImmutableMap<String, Function> functionMap;

    private JavaLibrary(String namespace, Class clazz, Iterable<Function> functions) {
        this.namespace = namespace;
        this.clazz = clazz;
        ImmutableMap.Builder<String, Function> b = ImmutableMap.builder();
        for (Function function : functions) {
            b.put(function.getName(), function);
        }
        functionMap = b.build();
    }

    @Override
    public String getLink(File baseFile) {
        return "java:" + clazz.getName();
    }

    public String getSimpleIdentifier() {
        return getNamespace();
    }

    public String getNamespace() {
        return namespace;
    }

    public String getLanguage() {
        return "java";
    }

    public Function getFunction(String name) {
        return functionMap.get(name);
    }

    public boolean hasFunction(String name) {
        return functionMap.containsKey(name);
    }

    private static class StaticMethodFunction implements Function {

        public static StaticMethodFunction find(Class c, String methodName) {
            Method m = Functions.findMethod(c, methodName, false);
            return new StaticMethodFunction(m);
        }

        private final Method method;
        private final ImmutableList<Argument> arguments;

        public StaticMethodFunction(Method method) {
            checkArgument(Modifier.isStatic(method.getModifiers()), "Method %s is not a static method.", method);
            this.method = method;
            this.arguments = Functions.introspect(method);
        }

        public String getName() {
            return method.getName();
        }

        public Object invoke(Object... args) throws Exception {
            return method.invoke(null, args);
        }

        public ImmutableList<Argument> getArguments() {
            return arguments;
        }
    }
}
