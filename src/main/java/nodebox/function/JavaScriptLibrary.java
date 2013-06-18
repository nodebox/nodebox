package nodebox.function;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import nodebox.util.FileUtils;
import nodebox.util.LoadException;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Function library implementation of JavaScript code.
 */
final class JavaScriptLibrary extends FunctionLibrary {

    private final static Context globalContext;
    private final static Scriptable globalScope;

    static {
        globalContext = Context.enter();
        globalScope = globalContext.initStandardObjects();
    }

    private final String namespace;
    private final File file;
    private ImmutableMap<String, Function> functionMap;


    JavaScriptLibrary(String namespace, File file, ImmutableMap<String, Function> functionMap) {
        this.namespace = namespace;
        this.file = file;
        this.functionMap = functionMap;
    }

    /**
     * Load the functions in the provided script and return a new JavaScriptLibrary.
     *
     * @param fileName The file name.
     * @return The new JavaScript library.
     * @throws nodebox.util.LoadException If the script could not be loaded.
     */
    public static JavaScriptLibrary loadScript(String fileName) throws LoadException {
        return loadScript(null, fileName);
    }

    /**
     * Load the functions in the provided script and return a new JavaScriptLibrary.
     *
     * @param baseFile The file to which the path of this library is relative to.
     * @param fileName The file name.
     * @return The new JavaScript library.
     * @throws LoadException If the script could not be loaded.
     */
    public static JavaScriptLibrary loadScript(File baseFile, String fileName) throws LoadException {
        File file;
        if (baseFile != null) {
            file = new File(baseFile.getAbsoluteFile(), fileName);
        } else {
            file = new File(fileName);
        }
        return loadScript(file);
    }

    private static JavaScriptLibrary loadScript(File file) {
        String namespace;
        ImmutableMap.Builder<String, Function> builder = ImmutableMap.builder();

        try {
            Object[] beforeNamespaces = globalScope.getIds();
            globalContext.evaluateReader(globalScope, new FileReader(file), file.getName(), 1, null);
            Object[] afterNamespaces = globalScope.getIds();
            if (beforeNamespaces.length == afterNamespaces.length) {
                throw new LoadException(file, "JavaScript libraries did not define a namespace..");
            }
            // The last namespace is the one that was defined in the file.
            Object nsId = afterNamespaces[afterNamespaces.length - 1];
            if (!(nsId instanceof String)) {
                throw new LoadException(file, String.format("Namespace %s is not a string.", nsId));
            }
            namespace = (String) nsId;
            Scriptable ns = (Scriptable) globalScope.get(namespace, globalScope);
            Object[] ids = ns.getIds();
            for (Object id : ids) {
                String objectName = id.toString();
                Object o = ns.get(objectName, ns);
                if (o instanceof org.mozilla.javascript.Function) {
                    JavaScriptFunction fn = new JavaScriptFunction(objectName, (org.mozilla.javascript.Function) o);
                    builder.put(objectName, fn);
                }
            }
        } catch (IOException e) {
            throw new LoadException(file, String.format("Could not load file: " + e.getMessage()), e);
        }

        return new JavaScriptLibrary(namespace, file, builder.build());

    }

    @Override
    public String getSimpleIdentifier() {
        return file.getName();
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    public String getLanguage() {
        return "javascript";
    }

    @Override
    public String getLink(File baseFile) {
        File parentFile = baseFile != null ? baseFile.getParentFile() : null;
        return "javascript:" + FileUtils.getRelativeLink(file, parentFile);
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public Function getFunction(String name) {
        return functionMap.get(name);
    }

    @Override
    public boolean hasFunction(String name) {
        return functionMap.containsKey(name);
    }

    private static final class JavaScriptFunction implements Function {

        private final String name;
        private final org.mozilla.javascript.Function fn;

        private JavaScriptFunction(String name, org.mozilla.javascript.Function fn) {
            this.name = name;
            this.fn = fn;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Object invoke(Object... args) throws Exception {
            return fn.call(globalContext, globalScope, null, args);
        }

        @Override
        public ImmutableList<Argument> getArguments() {
            // TODO How do I get the argument list from a JavaScript function?
            return ImmutableList.of();
        }
    }
}
