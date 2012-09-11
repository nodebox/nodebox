package nodebox.function;

import clojure.lang.Compiler;
import clojure.lang.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import nodebox.util.FileUtils;
import nodebox.util.LoadException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

final class ClojureLibrary extends FunctionLibrary {

    private static final Keyword NAME = Keyword.intern("name");
    private static final Keyword FN = Keyword.intern("fn");


    /**
     * Run the Clojure register-nodes function in the library.
     *
     * @param fileName The file name.
     * @return The new Clojure library.
     * @throws LoadException If the script could not be loaded.
     */
    public static ClojureLibrary loadScript(String fileName) throws LoadException {
        return loadScript(null, fileName);
    }

    /**
     * Run the Clojure register-nodes function in the library.
     *
     * @param baseFile The file to which the path of this library is relative to.
     * @param fileName The file name.
     * @return The new Clojure library.
     * @throws LoadException If the script could not be loaded.
     */
    public static ClojureLibrary loadScript(File baseFile, String fileName) throws LoadException {
        File file;
        if (baseFile != null) {
            file = new File(baseFile.getAbsoluteFile(), fileName);
        } else {
            file = new File(fileName);
        }
        return loadScript(file);
    }

    private static ClojureLibrary loadScript(File file) {
        Object returnValue;
        try {
            returnValue = Compiler.loadFile(file.getCanonicalPath());
        } catch (IOException e) {
            throw new LoadException(file, e);
        }
        // We need a Var as the last statement, because we need to retrieve the current namespace.
        if (!(returnValue instanceof Var)) {
            throw new LoadException(file,
                    String.format("The last statement does not define a var, but %s.\n" +
                            "Make sure the last line of your script looks like this:\n" +
                            "(def nodes [{:name \"foo\" :fn inc}])",
                            returnValue));
        }
        Var nodesVar = (Var) returnValue;
        Namespace ns = nodesVar.ns;
        String namespace = ns.name.getName();
        Object functionMap = nodesVar.deref();
        checkStructure(functionMap);
        ImmutableMap.Builder<String, Function> builder = ImmutableMap.builder();
        for (Object item : (Iterable) functionMap) {
            Map m = (Map) item;
            String name = (String) m.get(NAME);
            IFn fn = (IFn) m.get(FN);
            Function f = new ClojureFunction(name, fn);
            builder.put(name, f);
        }
        return new ClojureLibrary(namespace, file, builder.build());
    }

    private final String namespace;
    private final File file;
    private ImmutableMap<String, Function> functionMap;

    private ClojureLibrary(String namespace, File file, ImmutableMap<String, Function> functionMap) {
        this.namespace = namespace;
        this.file = file;
        this.functionMap = functionMap;
    }

    @Override
    public String getLink(File baseFile) {
        File parentFile = baseFile != null ? baseFile.getParentFile() : null;
        return "clojure:" + FileUtils.getRelativeLink(file, parentFile);
    }

    public String getSimpleIdentifier() {
        return file.getName();
    }

    public String getNamespace() {
        return namespace;
    }

    public String getLanguage() {
        return "clojure";
    }

    public File getFile() {
        return file;
    }

    public Function getFunction(String name) {
        return functionMap.get(name);
    }

    public boolean hasFunction(String name) {
        return functionMap.containsKey(name);
    }

    /**
     * Reloads the clojure module.
     */
    @Override
    public void reload() {
        ClojureLibrary reloadedLibrary = loadScript(file);
        if (!reloadedLibrary.namespace.equals(namespace))
            throw new RuntimeException("The namespace of a function library should not be changed.");
        this.functionMap = reloadedLibrary.functionMap;
    }

    /**
     * We expect a list of maps, each containing name and fn.
     *
     * @param v The Clojure data structure contained in all-nodes.
     */
    private static void checkStructure(Object v) {
        checkArgument(v instanceof Iterable, "The function map is not a list of maps but a %s", v);
        Iterable iterable = (Iterable) v;
        for (Object item : iterable) {
            checkArgument(item instanceof Map, "The function map is not a list of maps but a %s", v);
            Map m = (Map) item;
            checkArgument(m.containsKey(NAME), "The function map item %s does not contain a name.", m);
            checkArgument(m.containsKey(FN), "The function map item %s does not contain a fn.", m);
            checkArgument(m.get(FN) instanceof IFn, "The function map item %s does not point to a Clojure function.", m.get("name"));
        }
    }

    private static final class ClojureFunction implements Function {

        private final String name;
        private final IFn fn;
        private final ImmutableList<Argument> arguments;

        public ClojureFunction(String name, IFn fn) {
            this.name = name;
            this.fn = fn;
            this.arguments = introspect(fn);
        }

        public String getName() {
            return name;
        }

        public Object invoke(Object... args) throws Exception {
            return fn.applyTo(RT.arrayToList(args));
        }

        public ImmutableList<Argument> getArguments() {
            return arguments;
        }

        private static ImmutableList<Argument> introspect(IFn fn) {
            // Each function is a separate class.
            Class functionClass = fn.getClass();
            Method m = Functions.findMethod(functionClass, "invoke");
            return Functions.introspect(m);
        }

    }
}
