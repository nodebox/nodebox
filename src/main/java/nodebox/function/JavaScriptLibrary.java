package nodebox.function;


import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import nodebox.util.FileUtils;
import nodebox.util.LoadException;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;

/**
 * Function library implementation of JavaScript code.
 */
final class JavaScriptLibrary extends FunctionLibrary {

    private final Context context;
    private final Scriptable scope;
    private final File file;
    private String namespace;
    private ImmutableMap<String, Function> functionMap;

    JavaScriptLibrary(File file) {
        this.file = file;
        context = Context.enter();
        scope = context.initStandardObjects();

        loadSourceFromResource("underscore.js");
        loadScript(file);
    }

    private static String getSource(String resourceName) {
        try {
            URL url = Resources.getResource(resourceName);
            return Resources.toString(url, Charsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        return new JavaScriptLibrary(file);
    }

    private Object loadSourceFromResource(String resourceName) {
        return context.evaluateString(scope, getSource(resourceName), resourceName, 1, null);
    }

    private void loadScript(File file) {
        ImmutableMap.Builder<String, Function> builder = ImmutableMap.builder();

        try {
            Object[] beforeNamespaces = scope.getIds();
            context.evaluateReader(scope, new FileReader(file), file.getName(), 1, null);
            Object[] afterNamespaces = scope.getIds();
            if (beforeNamespaces.length == afterNamespaces.length) {
                throw new LoadException(file, "JavaScript libraries did not define a namespace..");
            }
            // The last namespace is the one that was defined in the file.
            Object nsId = afterNamespaces[afterNamespaces.length - 1];
            if (!(nsId instanceof String)) {
                throw new LoadException(file, String.format("Namespace %s is not a string.", nsId));
            }
            namespace = (String) nsId;
            Scriptable ns = (Scriptable) scope.get(namespace, scope);
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
        functionMap = builder.build();
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

    private final class JavaScriptFunction implements Function {

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
            return fn.call(context, scope, null, args);
        }

        @Override
        public ImmutableList<Argument> getArguments() {
            // TODO How do I get the argument list from a JavaScript function?
            return ImmutableList.of();
        }
    }
}
